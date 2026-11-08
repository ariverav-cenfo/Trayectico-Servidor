package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.AbstractIntegrationTest;
import com.example.trayecticoservidor.model.EquivalenciaRamal;
import com.example.trayecticoservidor.model.TarifaAresep;
import com.example.trayecticoservidor.model.TarifaRamal;
import com.example.trayecticoservidor.model.TipoCoincidencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link EquivalenciaRamalDAO} contra PostgreSQL con Testcontainers.
 * Cada prueba corre en una transacción que se revierte al terminar.
 */
@Transactional
class EquivalenciaRamalDAOTest extends AbstractIntegrationTest {

    private static final int ID_RAMAL_PRUEBA = 900001;
    private static final int ID_RAMAL_INEXISTENTE = 900099;

    @Autowired
    private EquivalenciaRamalDAO dao;

    @Autowired
    private TarifaAresepDAO tarifaDao;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void crearRamalDePrueba() {
        jdbc.update("INSERT INTO rutas (codigo_ctp, id_ctp, nombre) VALUES (?, ?, ?)",
                "TEST-9001", 900001, "Ruta de prueba");
        Long rutaId = jdbc.queryForObject(
                "SELECT id FROM rutas WHERE codigo_ctp = ?", Long.class, "TEST-9001");
        jdbc.update("INSERT INTO ramales (ruta_id, codigo_ctp, id_ctp, descripcion) VALUES (?, ?, ?, ?)",
                rutaId, "TEST-9001-A", ID_RAMAL_PRUEBA, "Ramal de prueba");
    }

    @Test
    void cargarLasMismasEquivalenciasDosVecesNoAgregaFilas() {
        long cantidadInicial = dao.contar();
        List<EquivalenciaRamal> equivalencias = List.of(equivalencia(ID_RAMAL_PRUEBA, 1.0));

        dao.insertarOActualizarTodas(equivalencias);
        assertEquals(cantidadInicial + 1, dao.contar());

        dao.insertarOActualizarTodas(equivalencias);
        assertEquals(cantidadInicial + 1, dao.contar());
    }

    @Test
    void unaEquivalenciaNuevaQuedaConElPagoElectronicoSinVerificar() {
        dao.insertarOActualizarTodas(List.of(equivalencia(ID_RAMAL_PRUEBA, 1.0)));

        assertNull(jdbc.queryForObject(
                "SELECT acepta_pago_electronico FROM equivalencias_ramales WHERE id_ramal_ctp = ?",
                Boolean.class, ID_RAMAL_PRUEBA));
    }

    @Test
    void recargarConservaElPagoElectronicoIngresadoManualmente() {
        dao.insertarOActualizarTodas(List.of(equivalencia(ID_RAMAL_PRUEBA, 0.90)));
        jdbc.update("UPDATE equivalencias_ramales SET acepta_pago_electronico = TRUE WHERE id_ramal_ctp = ?",
                ID_RAMAL_PRUEBA);

        dao.insertarOActualizarTodas(List.of(equivalencia(ID_RAMAL_PRUEBA, 1.0)));

        assertEquals(Boolean.TRUE, jdbc.queryForObject(
                "SELECT acepta_pago_electronico FROM equivalencias_ramales WHERE id_ramal_ctp = ?",
                Boolean.class, ID_RAMAL_PRUEBA));
        assertEquals(new BigDecimal("1.00"), jdbc.queryForObject(
                "SELECT puntaje FROM equivalencias_ramales WHERE id_ramal_ctp = ?",
                BigDecimal.class, ID_RAMAL_PRUEBA));
    }

    @Test
    void rechazaUnRamalQueNoEstaImportado() {
        assertThrows(DataIntegrityViolationException.class, () ->
                dao.insertarOActualizarTodas(List.of(equivalencia(ID_RAMAL_INEXISTENTE, 1.0))));
    }

    @Test
    void rechazaUnTipoDeCoincidenciaDeRuta() {
        EquivalenciaRamal deRuta = new EquivalenciaRamal("TEST-9001", ID_RAMAL_PRUEBA, "TEST-9001-A",
                "Ramal de prueba", "99", "TEST-R9001", "RAMAL DE PRUEBA", TipoCoincidencia.EXACTA, 1.0);

        assertThrows(DataIntegrityViolationException.class, () ->
                dao.insertarOActualizarTodas(List.of(deRuta)));
    }

    @Test
    void buscaLosRamalesImportadosYLosQueTienenTarifa() {
        tarifaDao.insertarOActualizarTodas(List.of(tarifa("TEST-R9001", "TEST-T9001", "350.00")));

        assertTrue(dao.buscarIdsRamalesCtp().contains(ID_RAMAL_PRUEBA));
        assertFalse(dao.buscarIdsRamalesCtp().contains(ID_RAMAL_INEXISTENTE));
        assertTrue(dao.buscarCodigosRamalAresepConTarifa().contains("TEST-R9001"));
        assertFalse(dao.buscarCodigosRamalAresepConTarifa().contains("TEST-R9999"));
    }

    /** Ramal con un solo fraccionamiento: la tarifa es exacta. */
    @Test
    void unRamalConUnaFraccionTieneTarifaExacta() {
        dao.insertarOActualizarTodas(List.of(equivalencia(ID_RAMAL_PRUEBA, 1.0)));
        tarifaDao.insertarOActualizarTodas(List.of(tarifa("TEST-R9001", "TEST-T9001", "350.00")));

        TarifaRamal tarifa = dao.buscarTarifaRamal(ID_RAMAL_PRUEBA).orElseThrow();

        assertEquals(new BigDecimal("350.00"), tarifa.tarifaViajeCompleto());
        assertEquals(1, tarifa.fracciones());
        assertTrue(tarifa.esExacta());
    }

    /** Ramal con dos fraccionamientos: se toma la tarifa más alta y no es exacta. */
    @Test
    void unRamalConVariasFraccionesDevuelveLaTarifaMasAltaYNoEsExacta() {
        dao.insertarOActualizarTodas(List.of(equivalencia(ID_RAMAL_PRUEBA, 1.0)));
        tarifaDao.insertarOActualizarTodas(List.of(
                tarifa("TEST-R9001", "TEST-T9001", "350.00"),
                tarifa("TEST-R9001", "TEST-T9002", "500.00")));

        TarifaRamal tarifa = dao.buscarTarifaRamal(ID_RAMAL_PRUEBA).orElseThrow();

        assertEquals(new BigDecimal("500.00"), tarifa.tarifaViajeCompleto());
        assertEquals(2, tarifa.fracciones());
        assertFalse(tarifa.esExacta());
    }

    /** Sin equivalencia, o con equivalencia pero sin tarifas en ARESEP, no hay tarifa. */
    @Test
    void sinEquivalenciaOSinTarifasNoHayTarifa() {
        assertTrue(dao.buscarTarifaRamal(ID_RAMAL_PRUEBA).isEmpty());

        dao.insertarOActualizarTodas(List.of(equivalencia(ID_RAMAL_PRUEBA, 1.0)));

        assertTrue(dao.buscarTarifaRamal(ID_RAMAL_PRUEBA).isEmpty());
    }

    private static EquivalenciaRamal equivalencia(int idRamalCtp, double puntaje) {
        return new EquivalenciaRamal("TEST-9001", idRamalCtp, "TEST-9001-A", "Ramal de prueba",
                "99", "TEST-R9001", "RAMAL DE PRUEBA", TipoCoincidencia.NOMBRE_EXACTO, puntaje);
    }

    private static TarifaAresep tarifa(String codigoRamal, String codigoFraccion, String tarifaRegular) {
        LocalDate vigencia = LocalDate.of(2026, 9, 4);
        return new TarifaAresep("99", "RUTA DE PRUEBA", codigoRamal, "RAMAL DE PRUEBA",
                codigoFraccion, "RAMAL DE PRUEBA", new BigDecimal("5.00"),
                new BigDecimal(tarifaRegular), new BigDecimal("0.00"), "RE-001", vigencia,
                "100", null, vigencia, vigencia, "ET-001", "Operador de prueba",
                "Regular", "San Jose");
    }
}