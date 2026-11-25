package com.example.trayecticoservidor.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Proposito: Presentacion de errores para la API segun el doc de SCRUM88
 * @param codigoHttp
 * @param codigoError
 * @param mensaje
 * @param detalles
 * @param timestamp
 * @param path
 */
public record ErrorResponseDTO(
        @JsonProperty("codigo_http") Integer codigoHttp,
        @JsonProperty("codigo_error") String codigoError,
        @JsonProperty("mensaje") String mensaje,
        @JsonProperty("detalles")List<DetalleErrorDTO> detalles,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("timestamp")LocalDateTime timestamp,
        @JsonProperty("path") String path
        ) {
}
