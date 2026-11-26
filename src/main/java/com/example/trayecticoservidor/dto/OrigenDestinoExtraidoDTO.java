package com.example.trayecticoservidor.dto;

/**
 * Representa el origen y destino identificados en una solicitud de viaje.
 *
 * @param origen Nombre del lugar de origen identificado.
 * @param destino Nombre del lugar de destino identificado.
 * @param preguntaAclaracion Pregunta necesaria cuando un lugar no puede
 *                           identificarse de forma única.
 */
public record OrigenDestinoExtraidoDTO(
        String origen,
        String destino,
        String preguntaAclaracion
) {
}
