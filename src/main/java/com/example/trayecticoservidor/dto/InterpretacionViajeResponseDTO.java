package com.example.trayecticoservidor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record InterpretacionViajeResponseDTO(
        PuntoViajeDTO origen,
        PuntoViajeDTO destino,
        List<LugarGeocodificadoDTO> opcionesOrigen,
        List<LugarGeocodificadoDTO> opcionesDestino,

        @JsonProperty("pregunta_aclaracion")
        String preguntaAclaracion
) {}