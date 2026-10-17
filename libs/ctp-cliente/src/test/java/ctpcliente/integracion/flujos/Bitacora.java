package ctpcliente.integracion.flujos;

import org.junit.jupiter.api.TestReporter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registro de los pasos de un escenario: servicio, duracion y resumen del resultado.
 */
final class Bitacora {

    record Paso(String servicio, String nombre, long milisegundos, String resumen) {
    }

    private final String escenario;
    private final List<Paso> pasos = new ArrayList<>();

    Bitacora(String escenario) {
        this.escenario = escenario;
    }

    <T> T paso(String servicio, String nombre, Supplier<T> accion, Function<T, String> resumen) {
        long inicio = System.nanoTime();
        T resultado = accion.get();
        pasos.add(new Paso(servicio, nombre, (System.nanoTime() - inicio) / 1_000_000, resumen.apply(resultado)));
        return resultado;
    }

    void verificacion(String nombre, String resumen) {
        pasos.add(new Paso("-", nombre, 0, resumen));
    }

    List<Paso> pasos() {
        return List.copyOf(pasos);
    }

    String informe() {
        StringBuilder sb = new StringBuilder(escenario).append('\n');
        for (Paso p : pasos) {
            sb.append(String.format("  %-4s %-48s %6d ms  %s%n", p.servicio(), p.nombre(), p.milisegundos(), p.resumen()));
        }
        return sb.toString();
    }

    void publicar(TestReporter reporte, String clave) {
        reporte.publishEntry(clave, informe());
        try {
            Path carpeta = Files.createDirectories(Path.of("target", "informes-flujos"));
            Files.writeString(carpeta.resolve(clave + ".txt"), informe(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
