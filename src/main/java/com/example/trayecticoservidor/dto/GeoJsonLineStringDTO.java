package com.example.trayecticoservidor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Proposito: Maneja el GeoJson LineString definido en SCRUM88, representa las coordenadas como arreglos
 * @param tipo tipo de geometria geografica, LineString segun documento de SCRUM88
 * @param coordenadas La lista que contiene las coordenadas
 */
public record GeoJsonLineStringDTO(
        @JsonProperty("tipo") String tipo,
        @JsonProperty("coordenadas")List<List<BigDecimal>> coordenadas
        ) {
    public GeoJsonLineStringDTO(List<List<BigDecimal>> coordenadas){
        this("LineString",coordenadas);
    }
}