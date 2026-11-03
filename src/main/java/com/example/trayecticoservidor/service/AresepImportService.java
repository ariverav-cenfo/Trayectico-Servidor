package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.client.AresepClient;
import com.example.trayecticoservidor.dao.TarifaAresepDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado de coordinar la importación de tarifas publicadas por ARESEP.
 */
@Service
public class AresepImportService {
    private static final Logger LOG = LoggerFactory.getLogger(AresepImportService.class);
    private final AresepClient cliente;
    private final TarifaAresepDAO dao;

    /**
     * Construye el servicio con el cliente ARESEP y el DAO de tarifas.
     *
     * @param cliente Cliente encargado de descargar y procesar las tarifas.
     * @param dao DAO encargado de almacenar las tarifas en la base de datos.
     */
    public AresepImportService(AresepClient cliente, TarifaAresepDAO dao) {
        this.cliente = cliente;
        this.dao = dao;
    }

    /**
     * Descarga, valida e importa las tarifas ARESEP a la base de datos.
     *
     * @return Cantidad de tarifas procesadas.
     * @throws RuntimeException Si ocurre un error durante la descarga o importación.
     */
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