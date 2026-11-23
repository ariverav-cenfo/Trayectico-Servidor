package com.example.trayecticoservidor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * El cuerpo del request esperado de la busqueda de rutas
 * @param origen punto de origen del viaje
 * @param destino punto de destino del viaje
 */
public record ViajeRequestDTO(
        @JsonProperty("origen") LocalizacionRequestDTO origen,
        @JsonProperty("destino") LocalizacionRequestDTO destino
) {}

