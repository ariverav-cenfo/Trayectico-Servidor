package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.AbstractIntegrationTest;
import com.example.trayecticoservidor.model.TarifaAresep;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class TarifaAresepDAOTest extends AbstractIntegrationTest {

    @Autowired
    private TarifaAresepDAO dao;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void importarLasMismasTarifasDosVecesNoAgregaFilas() {
        long cantidadInicial = contarFilas();
        List<TarifaAresep> tarifas = List.of(
                tarifa("TEST-T0001", "350.00", LocalDate.of(2026, 9, 4)),
                tarifa("TEST-T0002", "500.00", LocalDate.of(2026, 9, 4)));

        dao.insertarOActualizarTodas(tarifas);
        assertEquals(cantidadInicial + 2, contarFilas());

        dao.insertarOActualizarTodas(tarifas);
        assertEquals(cantidadInicial + 2, contarFilas());
    }

    @Test
    void importarUnaTarifaAnteriorConservaLaFilaActual() {
        dao.insertarOActualizarTodas(List.of(
                tarifa("TEST-T0001", "350.00", LocalDate.of(2026, 9, 4))));
        var filaActual = jdbc.queryForMap(
                "SELECT * FROM tarifas_aresep WHERE codigo_fraccion = ?", "TEST-T0001");

        dao.insertarOActualizarTodas(List.of(
                tarifa("TEST-T0001", "250.00", LocalDate.of(2026, 8, 1))));

        assertEquals(filaActual, jdbc.queryForMap(
                "SELECT * FROM tarifas_aresep WHERE codigo_fraccion = ?", "TEST-T0001"));
    }

    @Test
    void importarUnaTarifaMasRecienteActualizaLaFila() {
        dao.insertarOActualizarTodas(List.of(
                tarifa("TEST-T0001", "350.00", LocalDate.of(2026, 9, 4))));

        dao.insertarOActualizarTodas(List.of(
                tarifa("TEST-T0001", "400.00", LocalDate.of(2026, 10, 1))));

        assertEquals(new BigDecimal("400.00"), jdbc.queryForObject(
                "SELECT tarifa_regular FROM tarifas_aresep WHERE codigo_fraccion = ?",
                BigDecimal.class, "TEST-T0001"));
    }

    private long contarFilas() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tarifas_aresep", Long.class);
    }

    private TarifaAresep tarifa(String codigoFraccion, String tarifaRegular, LocalDate fechaVigencia) {
        return new TarifaAresep("I-1", "DESAMPARADOS-MORAVIA", "R0002", "DESAMPARADOS-MORAVIA",
                codigoFraccion, "DESAMPARADOS-MORAVIA", new BigDecimal("13.15"),
                new BigDecimal(tarifaRegular), new BigDecimal("0.00"), "RE-001", fechaVigencia,
                "100", null, fechaVigencia, fechaVigencia, "ET-001", "Operador de prueba",
                "Regular", "San Jose");
    }
}