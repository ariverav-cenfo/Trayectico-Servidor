package com.example.trayecticoservidor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Se supone que se llama parada response pero se me fue el typo.
 * Proposito: Contiene la informacion de las paradas, coordenadas, nombre y codigo
 * @param codigoParada
 * @param nombreParada
 * @param lat coordenadas latitud parada
 * @param lon coordenadas longitud parada
 */
public record ParaResponseDTO(
        @JsonProperty("codigo_parada") String codigoParada,
        @JsonProperty("nombre_parada") String nombreParada,
        @JsonProperty("lat")BigDecimal lat,
        @JsonProperty("lon")BigDecimal lon
        ) {
}
