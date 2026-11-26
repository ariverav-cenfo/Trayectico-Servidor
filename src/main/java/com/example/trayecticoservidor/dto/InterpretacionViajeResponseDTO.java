package com.example.trayecticoservidor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InterpretacionViajeResponseDTO(
        PuntoViajeDTO origen,
        PuntoViajeDTO destino,

        @JsonProperty("pregunta_aclaracion")
        String preguntaAclaracion
) {}