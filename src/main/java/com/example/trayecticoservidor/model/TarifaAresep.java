package com.example.trayecticoservidor.model;

import java.math.BigDecimal;
import java.time.LocalDate;

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