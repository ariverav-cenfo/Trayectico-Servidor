package com.example.trayecticoservidor.exception;

/**
 * Excepción lanzada cuando ocurre un error al consultar
 * el servicio externo de geocodificación.
 */
public class GeocodificacionException extends RuntimeException {

    public GeocodificacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
