package com.example.trayecticoservidor.dto;

/**
 * Representa un lugar geocodificado mediante Photon.
 *
 * @param nombre Nombre del lugar.
 * @param detalle Información geográfica adicional.
 * @param lat Latitud del lugar.
 * @param lon Longitud del lugar.
 */
public record LugarGeocodificadoDTO(
        String nombre,
        String detalle,
        double lat,
        double lon
) {}