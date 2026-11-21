package com.example.trayecticoservidor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Proposito: Representa una opcion de viaje, incluye la distancia caminando, numero de transbordos, costo,etc
 * @param transbordos
 * @param distanciaCaminandoMetros
 * @param costoTotal
 * @param tramos Lista de los tramos a tomar para el viaje
 */
public record OpcionesViajeResponseDTO(
        @JsonProperty("transbordos") Integer transbordos,
        @JsonProperty("distancia_caminando_metros") Integer distanciaCaminandoMetros,
        @JsonProperty("costo_total")BigDecimal costoTotal,
        @JsonProperty("tramos")List<TramoResponseDTO> tramos
        ) {
}
