package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.client.AresepClient;
import com.example.trayecticoservidor.dao.TarifaAresepDAO;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.net.URI;
import com.example.trayecticoservidor.model.TarifaAresep;
import static org.junit.jupiter.api.Assertions.*;

class AresepImportServiceTest {
    @Test
    void falloDeDescargaNoEscribeEnLaBaseDeDatos() {
        var cliente = new AresepClient(URI.create("http://localhost"), 1) {
            @Override
            public List<TarifaAresep> obtenerTarifas() {
                throw new IllegalStateException("ARESEP no disponible");
            }
        };
        var dao = new TarifaAresepDAO(null) {
            @Override
            public void insertarOActualizarTodas(List<TarifaAresep> tarifas) {
                fail("No debe escribir si la descarga falla");
            }
        };
        assertThrows(IllegalStateException.class, () -> new AresepImportService(cliente, dao).importarTarifas());
    }

    @Test
    void falloDeBaseDeDatosNoSeReportaComoExito() {
        var cliente = new AresepClient(URI.create("http://localhost"), 1) {
            @Override
            public List<TarifaAresep> obtenerTarifas() {
                return List.of();
            }
        };
        var dao = new TarifaAresepDAO(null) {
            @Override
            public void insertarOActualizarTodas(List<TarifaAresep> tarifas) {
                throw new IllegalStateException("BD no disponible");
            }
        };
        assertThrows(IllegalStateException.class, () -> new AresepImportService(cliente, dao).importarTarifas());
    }
}