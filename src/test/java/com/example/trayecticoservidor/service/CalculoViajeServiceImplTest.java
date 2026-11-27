package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.CalculoViajeDAO;
import com.example.trayecticoservidor.dto.LocalizacionRequestDTO;
import com.example.trayecticoservidor.dto.TramoResponseDTO;
import com.example.trayecticoservidor.dto.ViajeRequestDTO;
import com.example.trayecticoservidor.dto.ViajeResponseDTO;
import com.example.trayecticoservidor.model.TramoRutaInterno;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import com.example.trayecticoservidor.dto.OpcionesViajeResponseDTO;

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
                null, null, null);

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
    @Test
    void viajeConTransbordo_sumaLaTarifaDeCadaTramoEnBus() {
        TramoRutaInterno primero = tramoBus("0010", "P1", "9.93650", "-84.09485", "P2", "9.95890", "-84.12055",
                new BigDecimal("355.00"), true, true);
        TramoRutaInterno segundo = tramoBus("0020", "P3", "9.95900", "-84.12060", "P4", "9.97000", "-84.13000",
                new BigDecimal("480.00"), false, null);

        ViajeResponseDTO respuesta = calcular(List.of(primero, segundo));
        OpcionesViajeResponseDTO opcion = respuesta.opciones().get(0);

        assertEquals(1, opcion.transbordos());
        assertEquals(0, new BigDecimal("835.00").compareTo(opcion.costoTotal()));

        List<TramoResponseDTO> buses = tramosEnBus(opcion);
        assertEquals(0, new BigDecimal("355.00").compareTo(buses.get(0).tarifa()));
        assertEquals(Boolean.TRUE, buses.get(0).tarifaExacta());
        assertEquals(Boolean.TRUE, buses.get(0).aceptaPagoElectronico());
        assertEquals(0, new BigDecimal("480.00").compareTo(buses.get(1).tarifa()));
        assertEquals(Boolean.FALSE, buses.get(1).tarifaExacta());
        assertNull(buses.get(1).aceptaPagoElectronico());

        // Los tramos a pie no suman al costo.
        opcion.tramos().stream()
                .filter(t -> "caminata".equals(t.tipo()))
                .forEach(t -> assertNull(t.tarifa()));
    }

    @Test
    void tramoSinTarifa_elViajeNoTieneCostoTotal() {
        TramoRutaInterno conTarifa = tramoBus("0010", "P1", "9.93650", "-84.09485", "P2", "9.95890", "-84.12055",
                new BigDecimal("355.00"), true, true);
        TramoRutaInterno sinTarifa = tramoBus("0020", "P3", "9.95900", "-84.12060", "P4", "9.97000", "-84.13000",
                null, null, null);

        OpcionesViajeResponseDTO opcion = calcular(List.of(conTarifa, sinTarifa)).opciones().get(0);

        assertNull(opcion.costoTotal());
        List<TramoResponseDTO> buses = tramosEnBus(opcion);
        assertEquals(0, new BigDecimal("355.00").compareTo(buses.get(0).tarifa()));
        assertNull(buses.get(1).tarifa());
        assertNull(buses.get(1).tarifaExacta());
    }

    /**
     * Corre el cálculo con un DAO falso que devuelve una sola opción con los tramos dados.
     */
    private static ViajeResponseDTO calcular(List<TramoRutaInterno> tramos) {
        CalculoViajeDAO dao = (oLat, oLon, dLat, dLon, max) -> List.of(tramos);
        CalculoViajeService service = new CalculoViajeServiceImpl(dao, JsonMapper.builder().build());
        ViajeRequestDTO request = new ViajeRequestDTO(
                new LocalizacionRequestDTO(new BigDecimal("9.93650"), new BigDecimal("-84.09485")),
                new LocalizacionRequestDTO(new BigDecimal("9.97000"), new BigDecimal("-84.13000")));
        return service.calcularViaje(request);
    }

    private static List<TramoResponseDTO> tramosEnBus(OpcionesViajeResponseDTO opcion) {
        return opcion.tramos().stream().filter(t -> "bus".equals(t.tipo())).toList();
    }

    private static TramoRutaInterno tramoBus(String codigoRuta,
                                             String abordaje, String latAbordaje, String lonAbordaje,
                                             String descenso, String latDescenso, String lonDescenso,
                                             BigDecimal tarifa, Boolean tarifaExacta, Boolean aceptaPagoElectronico) {
        return new TramoRutaInterno(
                "bus", 1000, "{\"type\":\"LineString\",\"coordinates\":[[-84.1,9.9],[-84.2,9.95]]}",
                codigoRuta, "Ruta " + codigoRuta, codigoRuta + "-A", "Ramal " + codigoRuta, codigoRuta + "-A-1",
                abordaje, "Parada " + abordaje, new BigDecimal(latAbordaje), new BigDecimal(lonAbordaje),
                descenso, "Parada " + descenso, new BigDecimal(latDescenso), new BigDecimal(lonDescenso),
                tarifa, tarifaExacta, aceptaPagoElectronico);
    }
}