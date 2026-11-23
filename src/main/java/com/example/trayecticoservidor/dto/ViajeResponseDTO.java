package com.example.trayecticoservidor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Proposito: Response con la lista de opciones para el viaje deseado
 * @param opciones La lista de opciones para el viaje
 */
public record ViajeResponseDTO(
        @JsonProperty("opciones")List<OpcionesViajeResponseDTO>opciones
        ) {
}
