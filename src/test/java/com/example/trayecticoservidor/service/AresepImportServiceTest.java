package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.client.AresepClient;
import com.example.trayecticoservidor.dao.AresepFareDAO;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.net.URI;
import com.example.trayecticoservidor.model.AresepFare;
import static org.junit.jupiter.api.Assertions.*;

class AresepImportServiceTest {
    @Test
    void downloadFailureDoesNotWriteToDatabase() {
        var client = new AresepClient(URI.create("http://localhost"), 1) {
            @Override
            public List<AresepFare> fetchFares() {
                throw new IllegalStateException("ARESEP no disponible");
            }
        };
        var dao = new AresepFareDAO(null) {
            @Override
            public void upsertAll(List<AresepFare> fares) {
                fail("No debe escribir si la descarga falla");
            }
        };
        assertThrows(IllegalStateException.class, () -> new AresepImportService(client, dao).importFares());
    }

    @Test
    void databaseFailureIsNotReportedAsSuccess() {
        var client = new AresepClient(URI.create("http://localhost"), 1) {
            @Override
            public List<AresepFare> fetchFares() {
                return List.of();
            }
        };
        var dao = new AresepFareDAO(null) {
            @Override
            public void upsertAll(List<AresepFare> fares) {
                throw new IllegalStateException("BD no disponible");
            }
        };
        assertThrows(IllegalStateException.class, () -> new AresepImportService(client, dao).importFares());
    }
}

