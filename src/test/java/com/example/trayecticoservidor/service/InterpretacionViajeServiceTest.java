package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dto.InterpretacionViajeResponseDTO;
import com.example.trayecticoservidor.dto.LugarGeocodificadoDTO;
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

        interpretacionViajeService = new InterpretacionViajeService(
                openAiChatModel,
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


    @Test
    void extraerOrigenDestino_debeSolicitarAclaracionCuandoHayVariosResultados()
            throws Exception {

        String respuestaIA = """
        {
          "origen": "CENFOTEC",
          "destino": "La Sabana",
          "preguntaAclaracion": null
        }
        """;

        ChatResponse respuestaModelo = new ChatResponse(
                List.of(new Generation(new AssistantMessage(respuestaIA)))
        );

        when(openAiChatModel.call(any(Prompt.class)))
                .thenReturn(respuestaModelo);

        LugarGeocodificadoDTO opcion1 = new LugarGeocodificadoDTO(
                "Universidad CENFOTEC", "Catedral, San José", 9.93, -84.08
        );

        LugarGeocodificadoDTO opcion2 = new LugarGeocodificadoDTO(
                "Universidad CENFOTEC", "San Francisco, Heredia", 9.99, -84.12
        );

        LugarGeocodificadoDTO destino = new LugarGeocodificadoDTO(
                "Parque Metropolitano La Sabana",
                "Mata Redonda, San José",
                9.9355564,
                -84.1045042
        );

        when(geocodificacionService.buscarLugar("CENFOTEC"))
                .thenReturn(List.of(opcion1, opcion2));

        when(geocodificacionService.buscarLugar("La Sabana"))
                .thenReturn(List.of(destino));

        InterpretacionViajeResponseDTO resultado =
                interpretacionViajeService.extraerOrigenDestino(
                        "Quiero viajar de CENFOTEC a La Sabana"
                );

        assertNull(resultado.origen());
        assertNotNull(resultado.destino());
        assertNotNull(resultado.preguntaAclaracion());

        assertTrue(resultado.preguntaAclaracion().contains("Catedral, San José"));
        assertTrue(resultado.preguntaAclaracion().contains("San Francisco, Heredia"));

        verify(geocodificacionService, times(1)).buscarLugar("CENFOTEC");
        verify(geocodificacionService, times(1)).buscarLugar("La Sabana");
    }


    @Test
    void extraerOrigenDestino_debeSolicitarAclaracionCuandoNoHayResultados()
            throws Exception {

        String respuestaIA = """
        {
          "origen": "Atlantis",
          "destino": "La Sabana",
          "preguntaAclaracion": null
        }
        """;

        ChatResponse respuestaModelo = new ChatResponse(
                List.of(new Generation(new AssistantMessage(respuestaIA)))
        );

        when(openAiChatModel.call(any(Prompt.class)))
                .thenReturn(respuestaModelo);

        LugarGeocodificadoDTO destino = new LugarGeocodificadoDTO(
                "Parque Metropolitano La Sabana",
                "Mata Redonda, San José",
                9.9355564,
                -84.1045042
        );

        when(geocodificacionService.buscarLugar("Atlantis"))
                .thenReturn(List.of());

        when(geocodificacionService.buscarLugar("La Sabana"))
                .thenReturn(List.of(destino));

        InterpretacionViajeResponseDTO resultado =
                interpretacionViajeService.extraerOrigenDestino(
                        "Quiero viajar de Atlantis a La Sabana"
                );

        assertNull(resultado.origen());
        assertNotNull(resultado.destino());
        assertNotNull(resultado.preguntaAclaracion());

        assertTrue(resultado.preguntaAclaracion().contains("No se encontró el origen"));

        verify(geocodificacionService, times(1)).buscarLugar("Atlantis");
        verify(geocodificacionService, times(1)).buscarLugar("La Sabana");
    }
}