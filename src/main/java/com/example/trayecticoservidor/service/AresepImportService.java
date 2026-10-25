package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.client.AresepClient;
import com.example.trayecticoservidor.dao.AresepFareDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AresepImportService {
    private static final Logger LOG = LoggerFactory.getLogger(AresepImportService.class);
    private final AresepClient client;
    private final AresepFareDAO dao;

    public AresepImportService(AresepClient client, AresepFareDAO dao) {
        this.client = client;
        this.dao = dao;
    }

    public int importFares() {
        LOG.info("Iniciando importacion de tarifas ARESEP");
        try {
            var fares = client.fetchFares();
            dao.upsertAll(fares);
            LOG.info("Importacion ARESEP completada: {} registros procesados", fares.size());
            return fares.size();
        } catch (RuntimeException e) {
            LOG.error("Fallo la importacion ARESEP; se conserva la carga anterior", e);
            throw e;
        }
    }
}

