package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.client.PhotonClient;
import com.example.trayecticoservidor.dto.LugarGeocodificadoDTO;
import com.example.trayecticoservidor.exception.GeocodificacionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeocodificacionServiceTest {

    private PhotonClient photonClient;
    private GeocodificacionService geocodificacionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        photonClient = mock(PhotonClient.class);
        objectMapper = new ObjectMapper();

        geocodificacionService =
                new GeocodificacionService(photonClient);
    }

    @Test
    void buscarLugar_debeRetornarLugarCuandoPhotonEncuentraUnaCoincidencia()
            throws Exception {

        String respuestaPhoton = """
                {
                  "features": [
                    {
                      "type": "Feature",
                      "properties": {
                        "osm_key": "amenity",
                        "name": "Universidad CENFOTEC",
                        "district": "Catedral",
                        "city": "San José",
                        "state": "San José"
                      },
                      "geometry": {
                        "type": "Point",
                        "coordinates": [-84.08, 9.93]
                      }
                    }
                  ]
                }
                """;

        JsonNode respuesta = objectMapper.readTree(respuestaPhoton);

        when(photonClient.buscar("Universidad CENFOTEC"))
                .thenReturn(respuesta);

        List<LugarGeocodificadoDTO> resultado =
                geocodificacionService.buscarLugar("Universidad CENFOTEC");

        assertEquals(1, resultado.size());

        LugarGeocodificadoDTO lugar = resultado.get(0);

        assertEquals("Universidad CENFOTEC", lugar.nombre());
        assertEquals("Catedral, San José", lugar.detalle());
        assertEquals(9.93, lugar.lat(), 0.000001);
        assertEquals(-84.08, lugar.lon(), 0.000001);

        verify(photonClient, times(1))
                .buscar("Universidad CENFOTEC");
    }

    @Test
    void buscarLugar_debeRetornarVariosLugaresCuandoPhotonEncuentraVariasCoincidencias()
            throws Exception {

        String respuestaPhoton = """
            {
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "osm_key": "amenity",
                    "name": "Universidad CENFOTEC",
                    "district": "Catedral",
                    "city": "San José",
                    "state": "San José"
                  },
                  "geometry": {
                    "type": "Point",
                    "coordinates": [-84.08, 9.93]
                  }
                },
                {
                  "type": "Feature",
                  "properties": {
                    "osm_key": "amenity",
                    "name": "Universidad CENFOTEC",
                    "district": "San Francisco",
                    "city": "Heredia",
                    "state": "Heredia"
                  },
                  "geometry": {
                    "type": "Point",
                    "coordinates": [-84.12, 9.99]
                  }
                }
              ]
            }
            """;

        JsonNode respuesta = objectMapper.readTree(respuestaPhoton);

        when(photonClient.buscar("Universidad CENFOTEC"))
                .thenReturn(respuesta);

        List<LugarGeocodificadoDTO> resultado =
                geocodificacionService.buscarLugar("Universidad CENFOTEC");

        assertEquals(2, resultado.size());

        assertEquals("Universidad CENFOTEC", resultado.get(0).nombre());
        assertEquals("Catedral, San José", resultado.get(0).detalle());
        assertEquals(9.93, resultado.get(0).lat(), 0.000001);
        assertEquals(-84.08, resultado.get(0).lon(), 0.000001);

        assertEquals("Universidad CENFOTEC", resultado.get(1).nombre());
        assertEquals("San Francisco, Heredia", resultado.get(1).detalle());
        assertEquals(9.99, resultado.get(1).lat(), 0.000001);
        assertEquals(-84.12, resultado.get(1).lon(), 0.000001);

        verify(photonClient, times(1))
                .buscar("Universidad CENFOTEC");
    }

    @Test
    void buscarLugar_debeRetornarListaVaciaCuandoPhotonNoEncuentraCoincidencias()
            throws Exception {

        String respuestaPhoton = """
            {
              "features": []
            }
            """;

        JsonNode respuesta = objectMapper.readTree(respuestaPhoton);

        when(photonClient.buscar("Atlantis"))
                .thenReturn(respuesta);

        List<LugarGeocodificadoDTO> resultado =
                geocodificacionService.buscarLugar("Atlantis");

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());

        verify(photonClient, times(1))
                .buscar("Atlantis");
    }

    @Test
    void buscarLugar_debeLanzarExcepcionCuandoPhotonFalla() {
        when(photonClient.buscar("CENFOTEC"))
                .thenThrow(new RuntimeException("Error de conexión"));

        GeocodificacionException excepcion = assertThrows(
                GeocodificacionException.class,
                () -> geocodificacionService.buscarLugar("CENFOTEC")
        );

        assertTrue(excepcion.getMessage().contains(
                "No fue posible consultar el servicio de geocodificación"
        ));

        verify(photonClient, times(1)).buscar("CENFOTEC");
    }
}