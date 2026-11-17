package com.example.trayecticoservidor.config;

import com.example.trayecticoservidor.service.CargaEquivalenciasService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Carga las equivalencias de ramales al arrancar, solo si se pasa
 * {@code --trayectico.importar-equivalencias=<ruta del CSV>} (SCRUM-91).
 *
 * <p>Se corre después de importar las rutas del CTP y las tarifas de ARESEP: la carga solo
 * guarda ramales que ya están en {@code ramales} y que tienen tarifa (ver README).</p>
 */
@Component
@ConditionalOnProperty(name = "trayectico.importar-equivalencias")
public class ImportadorEquivalenciasRunner implements CommandLineRunner {

    private final CargaEquivalenciasService servicio;
    private final String archivo;

    /**
     * Construye el runner con el servicio de carga y la ruta del CSV.
     *
     * @param servicio servicio que lee, filtra y guarda las equivalencias.
     * @param archivo  ruta del CSV, por ejemplo {@code data/equivalencias-ramales.csv}.
     */
    public ImportadorEquivalenciasRunner(CargaEquivalenciasService servicio,
                                         @Value("${trayectico.importar-equivalencias}") String archivo) {
        this.servicio = servicio;
        this.archivo = archivo;
    }

    /**
     * Ejecuta la carga. Los conteos quedan en el log del servicio.
     *
     * @param args argumentos de la línea de comandos (no se usan).
     */
    @Override
    public void run(String... args) {
        servicio.cargar(Path.of(archivo));
    }
}