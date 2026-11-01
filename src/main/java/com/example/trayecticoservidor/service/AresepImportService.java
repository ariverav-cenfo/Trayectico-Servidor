package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.client.AresepClient;
import com.example.trayecticoservidor.dao.TarifaAresepDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AresepImportService {
    private static final Logger LOG = LoggerFactory.getLogger(AresepImportService.class);
    private final AresepClient cliente;
    private final TarifaAresepDAO dao;

    public AresepImportService(AresepClient cliente, TarifaAresepDAO dao) {
        this.cliente = cliente;
        this.dao = dao;
    }

    public int importarTarifas() {
        LOG.info("Iniciando importacion de tarifas ARESEP");
        try {
            var tarifas = cliente.obtenerTarifas();
            dao.insertarOActualizarTodas(tarifas);
            LOG.info("Importacion ARESEP completada: {} registros procesados", tarifas.size());
            return tarifas.size();
        } catch (RuntimeException e) {
            LOG.error("Fallo la importacion ARESEP; se conserva la carga anterior", e);
            throw e;
        }
    }
}