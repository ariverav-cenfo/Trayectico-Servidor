package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.CalculoViajeDAO;
import com.example.trayecticoservidor.dto.LocalizacionRequestDTO;
import com.example.trayecticoservidor.dto.TramoResponseDTO;
import com.example.trayecticoservidor.dto.ViajeRequestDTO;
import com.example.trayecticoservidor.dto.ViajeResponseDTO;
import com.example.trayecticoservidor.model.TramoRutaInterno;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalculoViajeServiceImplTest {

    @Test
    void geoJsonInvalido_noLanzaExcepcion_yElTramoSaleSinGeometria() {
        TramoRutaInterno tramo = new TramoRutaInterno(
                "bus", 1000, "esto no es GeoJSON",
                "0010", "Ruta prueba", "0010-F", "Ramal prueba", "0010-F-1",
                "P1", "Parada 1", new BigDecimal("9.93650"), new BigDecimal("-84.09485"),
                "P2", "Parada 2", new BigDecimal("9.95890"), new BigDecimal("-84.12055"),
                null, null);

        CalculoViajeDAO dao = (oLat, oLon, dLat, dLon, max) -> List.of(List.of(tramo));
        CalculoViajeService service = new CalculoViajeServiceImpl(dao, JsonMapper.builder().build());

        ViajeRequestDTO request = new ViajeRequestDTO(
                new LocalizacionRequestDTO(new BigDecimal("9.93650"), new BigDecimal("-84.09485")),
                new LocalizacionRequestDTO(new BigDecimal("9.95890"), new BigDecimal("-84.12055")));

        ViajeResponseDTO respuesta = assertDoesNotThrow(() -> service.calcularViaje(request));

        TramoResponseDTO tramoBus = respuesta.opciones().get(0).tramos().stream()
                .filter(t -> "bus".equals(t.tipo()))
                .findFirst()
                .orElseThrow();
        assertNull(tramoBus.geometria());
    }
}