package com.example.trayecticoservidor.dto;

/**
 * Representa la solicitud de interpretación de un viaje.
 *
 * @param mensaje Texto escrito por el usuario.
 */
public record InterpretacionViajeRequestDTO(
        String mensaje,
        PuntoViajeDTO origenSeleccionado,
        PuntoViajeDTO destinoSeleccionado
) {}