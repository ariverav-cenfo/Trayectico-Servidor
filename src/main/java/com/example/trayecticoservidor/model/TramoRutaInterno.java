package com.example.trayecticoservidor.model;

import java.math.BigDecimal;

/**
 * Model para mapear los queries de la DB para los Response
 * @param tipo
 * @param distanciaMetros
 * @param geoJsonGeometry
 * @param codigoRuta
 * @param nombreRuta
 * @param codigoRamal
 * @param nombreRamal
 * @param codigoRecorrido
 * @param paradaAbordajeCodigo
 * @param paradaAbordajeNombre
 * @param paradaAbordajeLat
 * @param paradaAbordajeLon
 * @param paradaDescensoCodigo
 * @param paradaDescensoNombre
 * @param paradaDescensoLat
 * @param paradaDescensoLon
 * @param tarifa Tarifa regular del ramal según ARESEP; null si no hay tarifa
 * @param tarifaExacta true si el ramal tiene un solo fraccionamiento; null si no hay tarifa
 * @param aceptaPagoElectronico
 */
public record TramoRutaInterno(
        String tipo,
        Integer distanciaMetros,
        String geoJsonGeometry,
        String codigoRuta,
        String nombreRuta,
        String codigoRamal,
        String nombreRamal,
        String codigoRecorrido,
        String paradaAbordajeCodigo,
        String paradaAbordajeNombre,
        BigDecimal paradaAbordajeLat,
        BigDecimal paradaAbordajeLon,
        String paradaDescensoCodigo,
        String paradaDescensoNombre,
        BigDecimal paradaDescensoLat,
        BigDecimal paradaDescensoLon,
        BigDecimal tarifa,
        Boolean tarifaExacta,
        Boolean aceptaPagoElectronico
) {
}
