package com.example.trayecticoservidor.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Representa una tarifa de transporte público publicada por ARESEP.
 *
 * @param codigoRuta Código de la ruta.
 * @param nombreRuta Nombre de la ruta.
 * @param codigoRamal Código del ramal.
 * @param nombreRamal Nombre del ramal.
 * @param codigoFraccion Código de la fracción tarifaria.
 * @param nombreFraccion Nombre de la fracción tarifaria.
 * @param kilometrosPromedioViaje Kilómetros promedio del viaje.
 * @param tarifaRegular Tarifa regular.
 * @param tarifaAdultoMayor Tarifa para adulto mayor.
 * @param resolucion Número de resolución de ARESEP.
 * @param fechaResolucion Fecha de la resolución.
 * @param numeroGaceta Número de La Gaceta.
 * @param suplementoGaceta Suplemento de La Gaceta.
 * @param fechaGaceta Fecha de publicación en La Gaceta.
 * @param fechaVigencia Fecha de inicio de vigencia de la tarifa.
 * @param numeroExpediente Número de expediente.
 * @param operadores Operadores asociados a la tarifa.
 * @param tipoServicio Tipo de servicio de transporte.
 * @param provincia Provincia correspondiente.
 */
public record TarifaAresep(
        String codigoRuta,
        String nombreRuta,
        String codigoRamal,
        String nombreRamal,
        String codigoFraccion,
        String nombreFraccion,
        BigDecimal kilometrosPromedioViaje,
        BigDecimal tarifaRegular,
        BigDecimal tarifaAdultoMayor,
        String resolucion,
        LocalDate fechaResolucion,
        String numeroGaceta,
        String suplementoGaceta,
        LocalDate fechaGaceta,
        LocalDate fechaVigencia,
        String numeroExpediente,
        String operadores,
        String tipoServicio,
        String provincia) {
}