package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.RutaDAO;
import com.example.trayecticoservidor.dto.RutaImportada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Importa UNA ruta del CTP al arrancar la app: la lee y la guarda
// Solo se activa si la app arranca con --trayectico.importar-ruta=<código>,
// por ejemplo --trayectico.importar-ruta=0002. Sirve con cualquier código de ruta
// Sin ese argumento esta clase no existe, así que ni el arranque normal ni el CI llaman al CTP

@Component
@ConditionalOnProperty("trayectico.importar-ruta")
public class ImportadorRuta implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportadorRuta.class);

    private final LectorRutaCtpService lector;
    private final RutaDAO rutaDAO;
    private final String codigoRuta;

    public ImportadorRuta(LectorRutaCtpService lector, RutaDAO rutaDAO,
                          @Value("${trayectico.importar-ruta}") String codigoRuta) {
        this.lector = lector;
        this.rutaDAO = rutaDAO;
        this.codigoRuta = codigoRuta;
    }

    @Override
    public void run(ApplicationArguments args) {
        RutaImportada ruta = lector.leer(codigoRuta);
        rutaDAO.guardar(ruta);

        long paradas = ruta.ramales().stream()
                .flatMap(r -> r.desplazamientos().stream())
                .mapToLong(d -> d.paradas().size())
                .sum();
        log.info("Ruta {} importada: {} ramales, {} paradas en recorridos",
                ruta.codigo(), ruta.ramales().size(), paradas);
    }
}
