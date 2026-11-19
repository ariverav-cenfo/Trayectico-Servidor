package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.EquivalenciaRamalDAO;
import com.example.trayecticoservidor.dto.TarifaRamalDTO;
import com.example.trayecticoservidor.model.TarifaRamal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link TarifaServiceImpl} (SCRUM-116), con el DAO simulado.
 * No usan base de datos.
 */
class TarifaServiceImplTest {

    private final EquivalenciaRamalDAO equivalenciaRamalDAO = mock(EquivalenciaRamalDAO.class);
    private final TarifaServiceImpl servicio = new TarifaServiceImpl(equivalenciaRamalDAO);

    /** Cada tarifa del DAO se convierte al DTO; con varias fracciones la tarifa no es exacta. */
    @Test
    void convierteLasTarifasDelDaoAlFormatoDeLaApi() {
        when(equivalenciaRamalDAO.buscarTarifasPorCodigoRuta("0002")).thenReturn(Map.of(
                "0002-A", new TarifaRamal(new BigDecimal("240.00"), 1),
                "0002-B", new TarifaRamal(new BigDecimal("500.00"), 3)));

        Map<String, TarifaRamalDTO> tarifas = servicio.obtenerTarifasDeRuta("0002");

        assertEquals(2, tarifas.size());
        assertEquals(new TarifaRamalDTO(new BigDecimal("240.00"), true), tarifas.get("0002-A"));
        assertEquals(new TarifaRamalDTO(new BigDecimal("500.00"), false), tarifas.get("0002-B"));
    }

    /** Si la ruta no tiene ramales con tarifa, el mapa queda vacío. */
    @Test
    void sinTarifasDevuelveUnMapaVacio() {
        when(equivalenciaRamalDAO.buscarTarifasPorCodigoRuta("0007")).thenReturn(Map.of());

        assertTrue(servicio.obtenerTarifasDeRuta("0007").isEmpty());
    }
}