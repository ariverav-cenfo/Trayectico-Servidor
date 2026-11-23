package com.example.trayecticoservidor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Proposito: Representar las coordenadas geograficas (latitud y longitud)
 * Como se espera el request
 * @param lat latitud
 * @param lon longitud
 */
public record LocalizacionRequestDTO(
        @JsonProperty("lat") BigDecimal lat,
        @JsonProperty("lon") BigDecimal lon
) {}
