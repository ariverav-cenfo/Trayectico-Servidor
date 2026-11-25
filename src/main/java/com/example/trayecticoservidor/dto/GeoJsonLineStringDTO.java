package com.example.trayecticoservidor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Proposito: Maneja el GeoJson LineString definido en SCRUM88, representa las coordenadas como arreglos, necesario para presentar los detalles del viaje en el mapa
 * @param type tipo de geometria geografica, LineString segun documento de SCRUM88
 * @param coordinates La lista que contiene las coordenadas
 */
public record GeoJsonLineStringDTO(
        @JsonProperty("type") String type,
        @JsonProperty("coordinates")List<List<BigDecimal>> coordinates
        ) {
    public GeoJsonLineStringDTO(List<List<BigDecimal>> coordenadas){
        this("LineString",coordenadas);
    }
}