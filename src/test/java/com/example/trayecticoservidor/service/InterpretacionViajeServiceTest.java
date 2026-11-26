package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dto.InterpretacionViajeResponseDTO;
import com.example.trayecticoservidor.dto.LugarGeocodificadoDTO;
import com.example.trayecticoservidor.tool.GeocodificacionTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InterpretacionViajeServiceTest {

    private OpenAiChatModel openAiChatModel;
    private GeocodificacionService geocodificacionService;
    private InterpretacionViajeService interpretacionViajeService;

    @BeforeEach
    void setUp() {
        openAiChatModel = mock(OpenAiChatModel.class);

        when(openAiChatModel.getOptions())
                .thenReturn(OpenAiChatOptions.builder().build());

        geocodificacionService = mock(GeocodificacionService.class);

        GeocodificacionTool geocodificacionTool =
                new GeocodificacionTool(geocodificacionService);

        interpretacionViajeService = new InterpretacionViajeService(
                openAiChatModel,
                geocodificacionTool,
                geocodificacionService
        );
    }

    @Test
    void extraerOrigenDestino_debeIdentificarAmbosLugares()
            throws Exception {

        String mensaje =
                "Quiero viajar de CENFOTEC San Francisco de Heredia "
                        + "al Parque Metropolitano La Sabana";

        String respuestaIA = """
                {
                  "origen": "Universidad CENFOTEC San Francisco de Heredia",
                  "destino": "Parque Metropolitano La Sabana",
                  "preguntaAclaracion": null
                }
                """;

        ChatResponse respuestaModelo = new ChatResponse(
                List.of(
                        new Generation(
                                new AssistantMessage(respuestaIA)
                        )
                )
        );

        when(openAiChatModel.call(any(Prompt.class)))
                .thenReturn(respuestaModelo);

        LugarGeocodificadoDTO origen = new LugarGeocodificadoDTO(
                "Universidad CENFOTEC",
                "San Francisco, Heredia",
                9.99,
                -84.12
        );

        LugarGeocodificadoDTO destino = new LugarGeocodificadoDTO(
                "Parque Metropolitano La Sabana",
                "Mata Redonda, San José",
                9.9355564,
                -84.1045042
        );

        when(geocodificacionService.buscarLugar(
                "Universidad CENFOTEC San Francisco de Heredia"
        )).thenReturn(List.of(origen));

        when(geocodificacionService.buscarLugar(
                "Parque Metropolitano La Sabana"
        )).thenReturn(List.of(destino));

        InterpretacionViajeResponseDTO resultado =
                interpretacionViajeService.extraerOrigenDestino(mensaje);

        assertNotNull(resultado);

        assertNotNull(resultado.origen());
        assertEquals("Universidad CENFOTEC",
                resultado.origen().nombre());
        assertEquals(9.99, resultado.origen().lat(), 0.000001);
        assertEquals(-84.12, resultado.origen().lon(), 0.000001);

        assertNotNull(resultado.destino());
        assertEquals("Parque Metropolitano La Sabana",
                resultado.destino().nombre());
        assertEquals(9.9355564, resultado.destino().lat(), 0.000001);
        assertEquals(-84.1045042, resultado.destino().lon(), 0.000001);

        assertNull(resultado.preguntaAclaracion());

        verify(openAiChatModel, times(1))
                .call(any(Prompt.class));

        verify(geocodificacionService, times(1))
                .buscarLugar(
                        "Universidad CENFOTEC San Francisco de Heredia"
                );

        verify(geocodificacionService, times(1))
                .buscarLugar("Parque Metropolitano La Sabana");
    }

    @Test
    void extraerOrigenDestino_debeSolicitarAclaracionCuandoFaltaElOrigen()
            throws Exception {

        String mensaje = "Quiero viajar al Parque Metropolitano La Sabana";

        String respuestaIA = """
            {
              "origen": null,
              "destino": "Parque Metropolitano La Sabana",
              "preguntaAclaracion": null
            }
            """;

        ChatResponse respuestaModelo = new ChatResponse(
                List.of(
                        new Generation(
                                new AssistantMessage(respuestaIA)
                        )
                )
        );

        when(openAiChatModel.call(any(Prompt.class)))
                .thenReturn(respuestaModelo);

        LugarGeocodificadoDTO destino = new LugarGeocodificadoDTO(
                "Parque Metropolitano La Sabana",
                "Mata Redonda, San José",
                9.9355564,
                -84.1045042
        );

        when(geocodificacionService.buscarLugar(
                "Parque Metropolitano La Sabana"
        )).thenReturn(List.of(destino));

        InterpretacionViajeResponseDTO resultado =
                interpretacionViajeService.extraerOrigenDestino(mensaje);

        assertNotNull(resultado);

        assertNull(resultado.origen());

        assertNotNull(resultado.destino());
        assertEquals("Parque Metropolitano La Sabana",
                resultado.destino().nombre());

        assertNotNull(resultado.preguntaAclaracion());

        verify(openAiChatModel, times(1)).call(any(Prompt.class));

        verify(geocodificacionService, times(1))
                .buscarLugar("Parque Metropolitano La Sabana");
    }

    @Test
    void extraerOrigenDestino_debeSolicitarAclaracionCuandoFaltaElDestino()
            throws Exception {

        String mensaje = "Quiero salir desde CENFOTEC San Francisco de Heredia";

        String respuestaIA = """
            {
              "origen": "Universidad CENFOTEC San Francisco de Heredia",
              "destino": null,
              "preguntaAclaracion": null
            }
            """;

        ChatResponse respuestaModelo = new ChatResponse(
                List.of(
                        new Generation(
                                new AssistantMessage(respuestaIA)
                        )
                )
        );

        when(openAiChatModel.call(any(Prompt.class)))
                .thenReturn(respuestaModelo);

        LugarGeocodificadoDTO origen = new LugarGeocodificadoDTO(
                "Universidad CENFOTEC",
                "San Francisco, Heredia",
                9.99,
                -84.12
        );

        when(geocodificacionService.buscarLugar(
                "Universidad CENFOTEC San Francisco de Heredia"
        )).thenReturn(List.of(origen));

        InterpretacionViajeResponseDTO resultado =
                interpretacionViajeService.extraerOrigenDestino(mensaje);

        assertNotNull(resultado);

        assertNotNull(resultado.origen());
        assertEquals("Universidad CENFOTEC",
                resultado.origen().nombre());

        assertNull(resultado.destino());

        assertNotNull(resultado.preguntaAclaracion());

        verify(openAiChatModel, times(1)).call(any(Prompt.class));

        verify(geocodificacionService, times(1))
                .buscarLugar("Universidad CENFOTEC San Francisco de Heredia");
    }
}