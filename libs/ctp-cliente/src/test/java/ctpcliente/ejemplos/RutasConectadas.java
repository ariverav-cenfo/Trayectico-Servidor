package ctpcliente.ejemplos;

import ctpcliente.CtpCliente;
import ctpcliente.error.CtpException;
import ctpcliente.error.CtpNoEncontradoException;
import ctpcliente.model.ParadaEnRecorrido;
import ctpcliente.model.Ruta;
import ctpcliente.util.Identificadores;

import java.io.PrintStream;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Lista las rutas que comparten paradas con una o varias rutas base (0002 si no se indica ninguna),
 * de la que comparte más paradas a la que comparte menos. Sirve para elegir el grupo de rutas de SCRUM-87.
 *
 * <p>La tabla sale por la salida estándar; el avance y los problemas salen como logs con {@link System.Logger},
 * igual que la biblioteca. Los mensajes van sin tildes, como los de la biblioteca, para que no se rompan en la
 * consola de Windows.
 *
 * <p>Código de salida:
 * <ul>
 *   <li>0: todas las rutas se analizaron completas.</li>
 *   <li>1: al menos una ruta no se pudo analizar (no existe, el CTP falló o no devolvió paradas).</li>
 *   <li>2: los argumentos no sirven; no se hace ninguna consulta al CTP.</li>
 *   <li>3: la tabla salió, pero faltaron paradas por consultar.</li>
 * </ul>
 *
 * <p>No se corre con {@code exec:java}: el pom fija {@link EjemploCtp} como clase principal y
 * {@code -Dexec.mainClass} no la reemplaza. Desde {@code libs/ctp-cliente}, en PowerShell:
 *
 * <pre>{@code
 * .\mvnw.cmd -q test-compile dependency:build-classpath "-Dmdep.outputFile=target/cp.txt"
 * java '-Djava.util.logging.SimpleFormatter.format=%4$s %5$s%6$s%n' `
 *     -cp "target/classes;target/test-classes;$(Get-Content target/cp.txt)" ctpcliente.ejemplos.RutasConectadas 0002
 * }</pre>
 */
public final class RutasConectadas {

    /** Avance y problemas de la corrida. */
    private static final System.Logger LOG = System.getLogger(RutasConectadas.class.getName());
    /** Salida de las tablas, en UTF-8 como en {@link EjemploCtp}. */
    private static final PrintStream OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);

    /** Ruta que se analiza si no se pasa ningún argumento. */
    private static final String RUTA_POR_DEFECTO = "0002";
    /** Código de salida: todas las rutas se analizaron completas. */
    private static final int COMPLETO = 0;
    /** Código de salida: al menos una ruta no se pudo analizar. */
    private static final int FALLO = 1;
    /** Código de salida: los argumentos no sirven. */
    private static final int ARGUMENTOS_INVALIDOS = 2;
    /** Código de salida: la tabla salió, pero faltaron paradas por consultar. */
    private static final int INCOMPLETO = 3;
    /** Cuántas rutas vecinas se muestran por tabla. */
    private static final int FILAS_TABLA = 15;
    /** Cada cuántas paradas se registra el avance. */
    private static final int AVANCE_CADA = 20;

    /** Solo tiene {@code main}; no se instancia. */
    private RutasConectadas() {
    }

    /**
     * Analiza las rutas indicadas y termina con el código de salida descrito en la clase.
     *
     * @param args códigos de ruta separados por espacio o por coma, por ejemplo {@code 0002 0006}; vacío usa 0002
     */
    public static void main(String[] args) {
        List<String> codigos = codigosBase(args);
        if (codigos.isEmpty()) {
            System.exit(ARGUMENTOS_INVALIDOS);
        }
        int resultado;
        try (CtpCliente ctp = CtpCliente.crear()) {
            resultado = ejecutar(ctp, codigos, OUT);
        } catch (RuntimeException e) {
            // Algo que no es un fallo del CTP: es un error de programación, se registra con la traza completa
            LOG.log(Level.ERROR, "Error inesperado; se detiene la corrida", e);
            resultado = FALLO;
        }
        if (resultado != COMPLETO) {
            System.exit(resultado);
        }
    }

    /**
     * Convierte los argumentos en la lista de rutas a analizar. Acepta {@code 0002 0006} o {@code 0002,0006},
     * quita espacios y repetidos, y registra cada problema en el log.
     *
     * @param args argumentos de la línea de comandos
     * @return los códigos en el orden en que vinieron, {@code [0002]} si no hay argumentos, o una lista vacía si
     *         algún código no tiene 4 dígitos o no quedó ninguno; en ese caso no se debe consultar al CTP
     */
    static List<String> codigosBase(String... args) {
        if (args.length == 0) {
            LOG.log(Level.INFO, "Sin argumentos: se usa la ruta {0}", RUTA_POR_DEFECTO);
            return List.of(RUTA_POR_DEFECTO);
        }
        Set<String> codigos = new LinkedHashSet<>();
        List<String> invalidos = new ArrayList<>();
        for (String arg : args) {
            for (String parte : arg.split(",")) {
                String codigo = parte.strip();
                if (codigo.isEmpty()) {
                    continue;
                }
                if (!Identificadores.esRuta(codigo)) {
                    invalidos.add(codigo);
                } else if (!codigos.add(codigo)) {
                    LOG.log(Level.WARNING, "La ruta {0} viene repetida; se analiza una sola vez", codigo);
                }
            }
        }
        if (!invalidos.isEmpty()) {
            LOG.log(Level.ERROR, "Codigos de ruta invalidos {0}: deben ser 4 digitos, por ejemplo 0002", invalidos);
            return List.of();
        }
        if (codigos.isEmpty()) {
            LOG.log(Level.ERROR, "No se indico ninguna ruta. Uso: RutasConectadas 0002 [0006 ...]");
            return List.of();
        }
        return List.copyOf(codigos);
    }

    /**
     * Analiza cada ruta por separado: si una falla, se registra y se sigue con las demás.
     *
     * @param ctp     cliente del CTP; las pruebas pueden pasar uno con transporte simulado
     * @param codigos rutas base ya validadas por {@link #codigosBase(String...)}
     * @param out     dónde se imprimen las tablas
     * @return el peor resultado de todas las rutas: 1 le gana a 3 y 3 le gana a 0
     */
    static int ejecutar(CtpCliente ctp, List<String> codigos, PrintStream out) {
        int peor = COMPLETO;
        for (String codigo : codigos) {
            int resultado = analizar(ctp, codigo, out);
            // FALLO le gana a INCOMPLETO: una ruta que no se pudo analizar es peor que una tabla con huecos
            if (resultado == FALLO || (resultado == INCOMPLETO && peor == COMPLETO)) {
                peor = resultado;
            }
        }
        return peor;
    }

    /**
     * Cuenta, para una ruta base, cuántas de sus paradas comparte cada una de las otras rutas e imprime la tabla.
     *
     * @param ctp        cliente del CTP
     * @param codigoBase código de la ruta base, por ejemplo {@code 0002}
     * @param out        dónde se imprime la tabla
     * @return 0 si se consultaron todas las paradas, 3 si faltaron algunas, 1 si no se pudo analizar la ruta
     */
    private static int analizar(CtpCliente ctp, String codigoBase, PrintStream out) {
        Ruta base;
        Map<String, String> paradas;
        try {
            base = ctp.ids().ruta(codigoBase);
            paradas = paradasDistintas(ctp, base);
        } catch (CtpNoEncontradoException e) {
            LOG.log(Level.ERROR, "La ruta {0} no existe en el catalogo del CTP", codigoBase);
            return FALLO;
        } catch (CtpException e) {
            LOG.log(Level.ERROR, "No se pudieron leer las paradas de la ruta {0}: {1}", codigoBase, e.getMessage());
            return FALLO;
        }
        if (paradas.isEmpty()) {
            LOG.log(Level.ERROR, "El CTP no devolvio paradas para la ruta {0}; no hay nada que comparar", codigoBase);
            return FALLO;
        }
        LOG.log(Level.INFO, "Ruta {0}: {1} paradas distintas. Consultando que rutas pasan por cada una...",
                codigoBase, paradas.size());

        // ruta vecina -> paradas que comparte con la ruta base
        Map<String, Set<String>> compartidas = new TreeMap<>();
        // Paradas que no se pudieron consultar: se registran y se sigue, para no perder minutos de consultas
        List<String> fallidas = new ArrayList<>();
        int consultadas = 0;
        for (String codigoParada : paradas.keySet()) {
            if (!Identificadores.esParada(codigoParada)) {
                // Filtro.parada lanzaría IllegalArgumentException y cortaría toda la corrida
                LOG.log(Level.WARNING, "Ruta {0}: parada con codigo invalido \"{1}\"; se omite", codigoBase, codigoParada);
                fallidas.add(codigoParada.isEmpty() ? "(sin codigo)" : codigoParada);
            } else {
                try {
                    for (String otra : ctp.consultas().rutasPorParada(codigoParada)) {
                        if (!otra.equals(base.codigo())) {
                            compartidas.computeIfAbsent(otra, k -> new TreeSet<>()).add(codigoParada);
                        }
                    }
                } catch (CtpException e) {
                    LOG.log(Level.WARNING, "Ruta {0}: no se pudo consultar la parada {1}: {2}",
                            codigoBase, codigoParada, e.getMessage());
                    fallidas.add(codigoParada);
                }
            }
            consultadas++;
            if (consultadas % AVANCE_CADA == 0) {
                LOG.log(Level.INFO, "Ruta {0}: {1} de {2} paradas consultadas", codigoBase, consultadas, paradas.size());
            }
        }

        if (fallidas.size() == paradas.size()) {
            LOG.log(Level.ERROR, "Ruta {0}: no se pudo consultar ninguna de sus {1} paradas", codigoBase, paradas.size());
            return FALLO;
        }
        imprimir(out, base.codigo(), paradas, compartidas);
        if (compartidas.isEmpty()) {
            LOG.log(Level.WARNING, "Ninguna otra ruta comparte paradas con la {0}; no sirve como centro del grupo",
                    codigoBase);
        }
        if (!fallidas.isEmpty()) {
            LOG.log(Level.WARNING, "Ruta {0}: faltaron {1} de {2} paradas {3}; los conteos pueden quedar cortos",
                    codigoBase, fallidas.size(), paradas.size(), fallidas);
            return INCOMPLETO;
        }
        return COMPLETO;
    }

    /**
     * Una parada aparece una vez por cada ramal y sentido; aquí queda cada código una sola vez.
     *
     * @param ctp  cliente del CTP
     * @param base ruta base, ya resuelta a su id interno
     * @return código de parada a descripción corta, en el orden en que el CTP las devuelve
     * @throws CtpException si el CTP no responde o responde con error
     */
    private static Map<String, String> paradasDistintas(CtpCliente ctp, Ruta base) {
        Map<String, String> paradas = new LinkedHashMap<>();
        for (ParadaEnRecorrido p : ctp.consultas().paradasDeRuta(base.id())) {
            paradas.putIfAbsent(p.codigo(), p.descripcionCorta());
        }
        return paradas;
    }

    /**
     * Imprime las {@value #FILAS_TABLA} rutas que más paradas comparten con la base, con una parada de ejemplo.
     *
     * @param out         dónde se imprime
     * @param codigoBase  código de la ruta base
     * @param paradas     paradas de la ruta base, para mostrar la descripción del ejemplo
     * @param compartidas ruta vecina a las paradas que comparte con la base
     */
    private static void imprimir(PrintStream out, String codigoBase, Map<String, String> paradas,
                                 Map<String, Set<String>> compartidas) {
        List<Map.Entry<String, Set<String>>> orden = new ArrayList<>(compartidas.entrySet());
        orden.sort((a, b) -> b.getValue().size() - a.getValue().size());

        out.printf("%nRutas que comparten paradas con la %s (%d en total):%n", codigoBase, orden.size());
        out.printf("%-8s %-10s %s%n", "Ruta", "Paradas", "Ejemplo de parada compartida");
        for (Map.Entry<String, Set<String>> e : orden.stream().limit(FILAS_TABLA).toList()) {
            String ejemplo = e.getValue().iterator().next();
            out.printf("%-8s %-10d %s (%s)%n", e.getKey(), e.getValue().size(), ejemplo, paradas.get(ejemplo));
        }
    }
}
