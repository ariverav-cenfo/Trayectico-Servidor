package com.example.trayecticoservidor.config;

import com.example.trayecticoservidor.client.AresepClient;
import com.example.trayecticoservidor.model.AresepFare;
import com.example.trayecticoservidor.model.EquivalenciaRamal;
import com.example.trayecticoservidor.model.TipoCoincidencia;
import com.example.trayecticoservidor.service.CruceTarifasService;
import ctpcliente.CtpCliente;
import ctpcliente.model.Ramal;
import ctpcliente.model.Ruta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Genera {@code data/equivalencias-ramales.csv}: el cruce entre los ramales del CTP y las tarifas de
 * ARESEP (SCRUM-36). Es lo que despues se carga a la base en SCRUM-91.
 *
 * <p>Se corre a mano desde IntelliJ (boton de ejecutar sobre {@link #main(String[])}); no levanta Spring
 * ni necesita la base de datos. Lee las tarifas del JSON guardado en el repo, asi el resultado no cambia
 * cada vez que ARESEP publica un pliego nuevo, y le pide al CTP la lista actual de rutas y ramales.</p>
 *
 * <p>Argumentos opcionales: ruta del JSON de ARESEP y ruta del CSV de salida. Por defecto usa
 * {@code data/aresep-tarifas.json} y {@code data/equivalencias-ramales.csv}, relativas a la raiz del
 * proyecto.</p>
 */
public final class CruceTarifasRunner {

    private static final Logger log = LoggerFactory.getLogger(CruceTarifasRunner.class);
    private static final Path ARESEP_POR_DEFECTO = Path.of("data", "aresep-tarifas.json");
    private static final Path CSV_POR_DEFECTO = Path.of("data", "equivalencias-ramales.csv");

    private CruceTarifasRunner() {
    }

    /**
     * Ejecuta el cruce completo y escribe el CSV.
     *
     * @param args {@code [jsonAresep] [csvSalida]}, ambos opcionales
     * @throws IOException si no se puede leer el JSON o escribir el CSV
     */
    public static void main(String[] args) throws IOException {
        Path json = args.length > 0 ? Path.of(args[0]) : ARESEP_POR_DEFECTO;
        Path csv = args.length > 1 ? Path.of(args[1]) : CSV_POR_DEFECTO;
        if (!Files.isRegularFile(json)) {
            throw new IllegalStateException("No existe " + json.toAbsolutePath()
                    + ". Descarguelo primero (ver README, seccion del cruce de tarifas).");
        }

        List<AresepFare> tarifas = new AresepClient(URI.create("http://localhost"), 1)
                .parseFares(Files.readString(json, StandardCharsets.UTF_8));
        log.info("ARESEP: {} fraccionamientos leidos de {}", tarifas.size(), json);

        Map<Ruta, List<Ramal>> ramalesPorRuta;
        List<String> rutasFallidas = new ArrayList<>();
        try (CtpCliente ctp = new ConfigurationCtp().ctpCliente()) {
            ramalesPorRuta = descargarRamales(ctp, rutasFallidas);
        }

        CruceTarifasService service = new CruceTarifasService();
        Map<String, TipoCoincidencia> rutas = service.cruzarRutas(
                ramalesPorRuta.keySet().stream().map(Ruta::codigo).toList(), tarifas);
        List<EquivalenciaRamal> ramales = service.cruzarRamales(ramalesPorRuta, tarifas);

        Path carpeta = csv.toAbsolutePath().getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }
        Files.writeString(csv, service.aCsv(ramales), StandardCharsets.UTF_8);

        reportar(rutas, ramales, rutasFallidas);
        log.info("CSV escrito en {}", csv.toAbsolutePath());
    }

    /** Pide al CTP todas las rutas y sus ramales, en orden de codigo; anota las rutas que fallan y sigue. */
    private static Map<Ruta, List<Ramal>> descargarRamales(CtpCliente ctp, List<String> rutasFallidas) {
        List<Ruta> rutas = ctp.visor().rutas().stream()
                .sorted(Comparator.comparing((Ruta r) -> r.codigo().strip()).thenComparingInt(Ruta::id))
                .toList();
        log.info("CTP: {} rutas; descargando sus ramales", rutas.size());
        Map<Ruta, List<Ramal>> resultado = new LinkedHashMap<>();
        int i = 0;
        for (Ruta ruta : rutas) {
            i++;
            try {
                resultado.put(ruta, ctp.visor().ramales(ruta.id()).stream()
                        .sorted(Comparator.comparing(Ramal::codigo).thenComparingInt(Ramal::id))
                        .toList());
            } catch (RuntimeException e) {
                log.warn("Ruta {} (id {}): no se pudieron leer sus ramales: {}", ruta.codigo(), ruta.id(),
                        e.getMessage());
                rutasFallidas.add(ruta.codigo().strip());
                resultado.put(ruta, List.of());
            }
            if (i % 50 == 0) {
                log.info("CTP: {}/{} rutas", i, rutas.size());
            }
        }
        return resultado;
    }

    /** Escribe en el log los conteos y las listas que van en la nota de SCRUM-36. */
    private static void reportar(Map<String, TipoCoincidencia> rutas, List<EquivalenciaRamal> ramales,
                                 List<String> rutasFallidas) {
        Map<TipoCoincidencia, TreeSet<String>> rutasPorTipo = new EnumMap<>(TipoCoincidencia.class);
        rutas.forEach((codigo, tipo) -> rutasPorTipo.computeIfAbsent(tipo, t -> new TreeSet<>()).add(codigo));
        Map<TipoCoincidencia, Integer> ramalesPorTipo = new EnumMap<>(TipoCoincidencia.class);
        ramales.forEach(e -> ramalesPorTipo.merge(e.tipo(), 1, Integer::sum));

        log.info("==== Rutas del CTP: {} ====", rutas.size());
        rutasPorTipo.forEach((tipo, codigos) -> log.info("{}: {}", tipo, codigos.size()));
        log.info("Solo por numero: {}", rutasPorTipo.getOrDefault(TipoCoincidencia.SOLO_NUMERO, new TreeSet<>()));
        log.info("Sin coincidencia: {}",
                rutasPorTipo.getOrDefault(TipoCoincidencia.SIN_COINCIDENCIA, new TreeSet<>()));
        log.info("==== Ramales del CTP: {} ====", ramales.size());
        ramalesPorTipo.forEach((tipo, cantidad) -> log.info("{}: {}", tipo, cantidad));
        if (!rutasFallidas.isEmpty()) {
            log.warn("Rutas cuyos ramales no se pudieron leer ({}): {}", rutasFallidas.size(), rutasFallidas);
        }
    }
}
