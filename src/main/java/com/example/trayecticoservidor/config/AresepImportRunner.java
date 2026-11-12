package com.example.trayecticoservidor.config;

import com.example.trayecticoservidor.service.AresepImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aresep.import-on-startup", havingValue = "true")
public class AresepImportRunner implements CommandLineRunner {
    private final AresepImportService servicio;

    public AresepImportRunner(AresepImportService servicio) {
        this.servicio = servicio;
    }

    @Override
    public void run(String... args) {
        servicio.importarTarifas();
    }
}