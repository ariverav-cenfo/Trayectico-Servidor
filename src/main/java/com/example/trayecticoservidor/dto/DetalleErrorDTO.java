package com.example.trayecticoservidor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Proposito: Contiene los detalles de los errores que se envien al frontend
 * @param campo
 * @param mensaje
 */
public record DetalleErrorDTO(
        @JsonProperty("campo") String campo,
        @JsonProperty("mensaje") String mensaje
) {
}
