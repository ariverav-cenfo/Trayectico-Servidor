package com.example.trayecticoservidor.controller;

import com.example.trayecticoservidor.dto.InterpretacionViajeResponseDTO;
import com.example.trayecticoservidor.dto.PuntoViajeDTO;
import com.example.trayecticoservidor.service.InterpretacionViajeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InterpretacionViajeController.class)
@AutoConfigureMockMvc(addFilters = false)
class InterpretacionViajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InterpretacionViajeService interpretacionViajeService;

    @Test
    void interpretar_debeRetornarOrigenYDestinoCuandoElServicioLosIdentifica()
            throws Exception {

        String mensaje = "Quiero viajar de CENFOTEC a La Sabana";

        InterpretacionViajeResponseDTO respuesta =
                new InterpretacionViajeResponseDTO(
                        new PuntoViajeDTO(
                                "Universidad CENFOTEC",
                                9.93,
                                -84.08
                        ),
                        new PuntoViajeDTO(
                                "Parque Metropolitano La Sabana",
                                9.9355564,
                                -84.1045042
                        ),
                        null
                );

        when(interpretacionViajeService.extraerOrigenDestino(mensaje))
                .thenReturn(respuesta);

        mockMvc.perform(post("/api/viajes/interpretar")
                        .contentType("application/json")
                        .content("""
                                {
                                  "mensaje": "Quiero viajar de CENFOTEC a La Sabana"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origen.nombre")
                        .value("Universidad CENFOTEC"))
                .andExpect(jsonPath("$.origen.lat").value(9.93))
                .andExpect(jsonPath("$.origen.lon").value(-84.08))
                .andExpect(jsonPath("$.destino.nombre")
                        .value("Parque Metropolitano La Sabana"))
                .andExpect(jsonPath("$.destino.lat").value(9.9355564))
                .andExpect(jsonPath("$.destino.lon").value(-84.1045042))
                .andExpect(jsonPath("$.pregunta_aclaracion").isEmpty());

        verify(interpretacionViajeService, times(1))
                .extraerOrigenDestino(mensaje);
    }
}