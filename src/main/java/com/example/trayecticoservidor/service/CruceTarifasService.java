package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.model.AresepFare;
import com.example.trayecticoservidor.model.EquivalenciaRamal;
import com.example.trayecticoservidor.model.TipoCoincidencia;
import ctpcliente.model.Ramal;
import ctpcliente.model.Ruta;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cruza el catalogo del CTP con el pliego tarifario de ARESEP (SCRUM-36).
 *
 * <p>Las dos fuentes escriben los codigos distinto: ARESEP usa {@code 02} y el CTP {@code 0002}, y los
 * codigos de ramal no se parecen ({@code R0012} y {@code 0002-A}). Por eso las rutas se cruzan por
 * codigo y los ramales por nombre, dentro de la misma ruta.</p>
 *
 * <p>No usa red ni base de datos: recibe las listas ya descargadas y devuelve el resultado.</p>
 */
@Service
public class CruceTarifasService {

    /** Similitud minima para marcar dos nombres como parecidos. */
    public static final double UMBRAL_PARECIDO = 0.85;

    private static final Pattern NUMERO_INICIAL = Pattern.compile("^\\s*(\\d+)");
    private static final Pattern BARRIO = Pattern.compile("\\bB\\s*[º°]\\s*");
    private static final Pattern NO_ALFANUMERICO = Pattern.compile("[^A-Z0-9 ]");
    private static final Pattern MARCAS = Pattern.compile("\\p{M}");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    /**
     * Codigo de ruta de ARESEP llevado al formato del CTP: si es solo numerico se completa con ceros
     * a la izquierda hasta 4 digitos; si no, se deja igual, sin espacios sobrantes.
     *
     * @param codigoAresep codigo de ruta segun ARESEP, p. ej. {@code 02} o {@code 66 BS}
     * @return codigo comparable con el del CTP, p. ej. {@code 0002} o {@code 66 BS}
     */
    public static String codigoExacto(String codigoAresep) {
        String codigo = codigoAresep.strip();
        return codigo.chars().allMatch(Character::isDigit) && !codigo.isEmpty() ? completar(codigo) : codigo;
    }

    /**
     * Numero inicial de un codigo de ruta, completado con ceros, sin letras ni sufijos.
     *
     * @param codigo codigo de ruta de cualquiera de las dos fuentes, p. ej. {@code 617-618}
     * @return el numero en formato del CTP, p. ej. {@code 0617}; vacio si el codigo no empieza con un numero
     */
    public static Optional<String> codigoNumerico(String codigo) {
        Matcher m = NUMERO_INICIAL.matcher(codigo);
        return m.find() ? Optional.of(completar(m.group(1))) : Optional.empty();
    }

    /**
     * Normaliza un nombre de ramal para compararlo entre fuentes: mayusculas, sin tildes, {@code Bº} como
     * {@code BARRIO}, sin "Y VICEVERSA", sin signos y con un solo espacio entre palabras.
     *
     * @param nombre nombre tal como viene de la fuente; {@code null} se trata como vacio
     * @return nombre normalizado
     */
    public static String normalizarNombre(String nombre) {
        if (nombre == null) {
            return "";
        }
        String texto = nombre.toUpperCase(Locale.ROOT);
        texto = BARRIO.matcher(texto).replaceAll("BARRIO ");
        texto = MARCAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        texto = texto.replace("Y VICEVERSA", " ");
        texto = NO_ALFANUMERICO.matcher(texto).replaceAll(" ");
        return ESPACIOS.matcher(texto).replaceAll(" ").strip();
    }

    /**
     * Similitud entre dos textos con la distancia de Levenshtein: 1 si son iguales, 0 si no comparten nada.
     *
     * @param a primer texto
     * @param b segundo texto
     * @return valor entre 0 y 1
     */
    public static double similitud(String a, String b) {
        int largo = Math.max(a.length(), b.length());
        return largo == 0 ? 1.0 : 1.0 - (double) levenshtein(a, b) / largo;
    }

    /**
     * Clasifica cada ruta del CTP segun si ARESEP publica tarifas para ella.
     *
     * @param codigosCtp codigos publicos de las rutas del CTP, p. ej. {@code 0002}
     * @param tarifas    fraccionamientos de ARESEP
     * @return tipo de coincidencia por codigo de ruta, ordenado por codigo
     */
    public Map<String, TipoCoincidencia> cruzarRutas(Collection<String> codigosCtp, List<AresepFare> tarifas) {
        Set<String> exactos = new LinkedHashSet<>();
        Set<String> numeros = new LinkedHashSet<>();
        for (AresepFare tarifa : tarifas) {
            exactos.add(codigoExacto(tarifa.routeCode()));
            codigoNumerico(tarifa.routeCode()).ifPresent(numeros::add);
        }
        Map<String, TipoCoincidencia> resultado = new TreeMap<>();
        for (String codigo : codigosCtp) {
            String ctp = codigo.strip();
            if (exactos.contains(ctp)) {
                resultado.put(ctp, TipoCoincidencia.EXACTA);
            } else if (codigoNumerico(ctp).filter(numeros::contains).isPresent()) {
                resultado.put(ctp, TipoCoincidencia.SOLO_NUMERO);
            } else {
                resultado.put(ctp, TipoCoincidencia.SIN_COINCIDENCIA);
            }
        }
        return resultado;
    }

    /**
     * Agrupa los ramales de ARESEP por numero de ruta; cada ramal guarda sus nombres normalizados.
     * La llave de cada ramal es su ruta mas su codigo, para que dos rutas con el mismo numero
     * ({@code 80} y {@code 80 A}) no mezclen sus ramales aunque repitan el codigo.
     */
    public List<EquivalenciaRamal> cruzarRamales(Map<Ruta, List<Ramal>> ramalesPorRuta, List<AresepFare> tarifas) {
        Map<String, Map<String, Candidato>> indice = indexar(tarifas);
        List<EquivalenciaRamal> resultado = new ArrayList<>();
        ramalesPorRuta.forEach((ruta, ramales) -> {
            Collection<Candidato> candidatos = codigoNumerico(ruta.codigo())
                    .map(n -> indice.getOrDefault(n, Map.of()).values())
                    .orElse(List.of());
            for (Ramal ramal : ramales) {
                resultado.add(cruzarRamal(ruta.codigo().strip(), ramal, candidatos));
            }
        });
        return resultado;
    }

    /**
     * Convierte el resultado en CSV con encabezado, separado por comas y en UTF-8.
     *
     * @param equivalencias filas del cruce
     * @return texto CSV, una fila por ramal del CTP
     */
    public String aCsv(List<EquivalenciaRamal> equivalencias) {
        StringBuilder csv = new StringBuilder("codigo_ruta_ctp,id_ramal_ctp,codigo_ramal_ctp,descripcion_ctp,"
                + "codigo_ruta_aresep,codigo_ramal_aresep,nombre_ramal_aresep,tipo_coincidencia,puntaje\n");
        for (EquivalenciaRamal e : equivalencias) {
            csv.append(String.join(",", campo(e.codigoRutaCtp()), String.valueOf(e.idRamalCtp()),
                    campo(e.codigoRamalCtp()), campo(e.descripcionCtp()), campo(e.codigoRutaAresep()),
                    campo(e.codigoRamalAresep()), campo(e.nombreRamalAresep()), e.tipo().name(),
                    String.format(Locale.ROOT, "%.2f", e.puntaje()))).append('\n');
        }
        return csv.toString();
    }

    private EquivalenciaRamal cruzarRamal(String codigoRuta, Ramal ramal, Collection<Candidato> candidatos) {
        if (candidatos.isEmpty()) {
            return equivalencia(codigoRuta, ramal, null, TipoCoincidencia.SIN_RUTA_ARESEP, 0.0);
        }
        String nombre = normalizarNombre(ramal.descripcion());
        if (nombre.isEmpty()) {
            // Un ramal sin nombre no se puede comparar: queda para revisar, sin candidato
            return equivalencia(codigoRuta, ramal, null, TipoCoincidencia.REVISAR, 0.0);
        }
        List<Candidato> exactos = candidatos.stream().filter(c -> c.nombres().contains(nombre)).toList();
        if (!exactos.isEmpty()) {
            TipoCoincidencia tipo = exactos.size() == 1 ? TipoCoincidencia.NOMBRE_EXACTO : TipoCoincidencia.NOMBRE_AMBIGUO;
            return equivalencia(codigoRuta, ramal, exactos.getFirst(), tipo, 1.0);
        }
        Candidato mejor = null;
        double puntaje = 0.0;
        for (Candidato c : candidatos) {
            for (String otro : c.nombres()) {
                double s = similitud(nombre, otro);
                if (s > puntaje) {
                    mejor = c;
                    puntaje = s;
                }
            }
        }
        TipoCoincidencia tipo = puntaje >= UMBRAL_PARECIDO ? TipoCoincidencia.NOMBRE_PARECIDO : TipoCoincidencia.REVISAR;
        return equivalencia(codigoRuta, ramal, mejor, tipo, puntaje);
    }

    private static EquivalenciaRamal equivalencia(String codigoRuta, Ramal ramal, Candidato c,
                                                  TipoCoincidencia tipo, double puntaje) {
        return new EquivalenciaRamal(codigoRuta, ramal.id(), ramal.codigo(), ramal.descripcion(),
                c == null ? null : c.codigoRuta(), c == null ? null : c.codigoRamal(),
                c == null ? null : c.nombreRamal(), tipo, puntaje);
    }

    /** Agrupa los ramales de ARESEP por numero de ruta; cada ramal guarda sus nombres normalizados. */
    private static Map<String, Map<String, Candidato>> indexar(List<AresepFare> tarifas) {
        Map<String, Map<String, Candidato>> indice = new HashMap<>();
        tarifas.stream().sorted(Comparator.comparing(AresepFare::branchCode)).forEach(t ->
                codigoNumerico(t.routeCode()).ifPresent(numero -> {
                    Candidato c = indice.computeIfAbsent(numero, k -> new LinkedHashMap<>())
                            .computeIfAbsent(t.routeCode() + "|" + t.branchCode(), k -> new Candidato(
                                    t.routeCode(), t.branchCode(), t.branchName(), new LinkedHashSet<>()));
                    agregarNombre(c.nombres(), t.branchName());
                    agregarNombre(c.nombres(), t.fractionName());
                }));
        return indice;
    }

    /** Agrega el nombre normalizado solo si no queda vacio: un nombre vacio no identifica ningun ramal. */
    private static void agregarNombre(Set<String> nombres, String nombre) {
        String normalizado = normalizarNombre(nombre);
        if (!normalizado.isEmpty()) {
            nombres.add(normalizado);
        }
    }

    private static String completar(String digitos) {
        return digitos.length() >= 4 ? digitos : "0".repeat(4 - digitos.length()) + digitos;
    }

    private static String campo(String valor) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.strip();
        return limpio.contains(",") || limpio.contains("\"") ? "\"" + limpio.replace("\"", "\"\"") + "\"" : limpio;
    }

    private static int levenshtein(String a, String b) {
        int[] previa = new int[b.length() + 1];
        int[] actual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previa[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            actual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                actual[j] = Math.min(Math.min(actual[j - 1] + 1, previa[j] + 1), previa[j - 1] + costo);
            }
            int[] tmp = previa;
            previa = actual;
            actual = tmp;
        }
        return previa[b.length()];
    }

    /** Un ramal de ARESEP con todos los nombres por los que se puede reconocer. */
    private record Candidato(String codigoRuta, String codigoRamal, String nombreRamal, Set<String> nombres) {
    }
}
