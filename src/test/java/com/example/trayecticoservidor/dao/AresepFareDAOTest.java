package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.AbstractIntegrationTest;
import com.example.trayecticoservidor.model.AresepFare;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class AresepFareDAOTest extends AbstractIntegrationTest {

    @Autowired
    private AresepFareDAO dao;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void importingSameFaresTwiceDoesNotAddRows() {
        long initialCount = rowCount();
        List<AresepFare> fares = List.of(
                fare("TEST-T0001", "350.00", LocalDate.of(2026, 9, 4)),
                fare("TEST-T0002", "500.00", LocalDate.of(2026, 9, 4)));

        dao.upsertAll(fares);
        assertEquals(initialCount + 2, rowCount());

        dao.upsertAll(fares);
        assertEquals(initialCount + 2, rowCount());
    }

    @Test
    void importingOlderFarePreservesCurrentRow() {
        dao.upsertAll(List.of(fare("TEST-T0001", "350.00", LocalDate.of(2026, 9, 4))));
        var currentRow = jdbc.queryForMap(
                "SELECT * FROM aresep_fare WHERE fraction_code = ?", "TEST-T0001");

        dao.upsertAll(List.of(fare("TEST-T0001", "250.00", LocalDate.of(2026, 8, 1))));

        assertEquals(currentRow, jdbc.queryForMap(
                "SELECT * FROM aresep_fare WHERE fraction_code = ?", "TEST-T0001"));
    }

    @Test
    void reimportingPreservesManuallyEnteredPaymentMethod() {
        dao.upsertAll(List.of(fare("TEST-T0001", "350.00", LocalDate.of(2026, 9, 4))));
        jdbc.update("UPDATE aresep_fare SET payment_method = ? WHERE fraction_code = ?",
                "Efectivo, Tarjeta", "TEST-T0001");

        dao.upsertAll(List.of(fare("TEST-T0001", "400.00", LocalDate.of(2026, 10, 1))));

        assertEquals("Efectivo, Tarjeta", jdbc.queryForObject(
                "SELECT payment_method FROM aresep_fare WHERE fraction_code = ?",
                String.class, "TEST-T0001"));
        assertEquals(new BigDecimal("400.00"), jdbc.queryForObject(
                "SELECT regular_fare FROM aresep_fare WHERE fraction_code = ?",
                BigDecimal.class, "TEST-T0001"));
    }

    private long rowCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM aresep_fare", Long.class);
    }

    private AresepFare fare(String fractionCode, String regularFare, LocalDate effectiveDate) {
        return new AresepFare("I-1", "DESAMPARADOS-MORAVIA", "R0002", "DESAMPARADOS-MORAVIA",
                fractionCode, "DESAMPARADOS-MORAVIA", new BigDecimal("13.15"),
                new BigDecimal(regularFare), new BigDecimal("0.00"), "RE-001", effectiveDate,
                "100", null, effectiveDate, effectiveDate, "ET-001", "Operador de prueba",
                "Regular", "San Jose");
    }
}
