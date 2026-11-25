package com.example.trayecticoservidor.dto;

import com.example.trayecticoservidor.model.TarifaRamal;

import java.math.BigDecimal;

/**
 * Tarifa de un ramal tal como la recibe el cliente (RF-TP-01).
 *
 * <p>El monto viene del pliego tarifario de ARESEP. Si el ramal tiene un solo fraccionamiento,
 * el monto es exacto; si tiene varios, es la tarifa del viaje completo (la más alta) y lo que
 * paga el usuario depende de dónde suba y baje.</p>
 *
 * @param montoColones Tarifa regular en colones costarricenses.
 * @param exacta {@code true} si el ramal tiene un solo fraccionamiento.
 */
public record TarifaRamalDTO(BigDecimal montoColones, boolean exacta) {

    /**
     * Convierte la tarifa del modelo al formato de la API.
     *
     * @param tarifa Tarifa calculada a partir de los fraccionamientos de ARESEP.
     * @return la tarifa lista para enviarse al cliente.
     */
    public static TarifaRamalDTO desde(TarifaRamal tarifa) {
        return new TarifaRamalDTO(tarifa.tarifaViajeCompleto(), tarifa.esExacta());
    }
}