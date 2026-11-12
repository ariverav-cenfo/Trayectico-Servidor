package com.example.trayecticoservidor.model;

import java.math.BigDecimal;

/**
 * Tarifa de un ramal del CTP según ARESEP.
 *
 * <p>ARESEP publica una tarifa por fraccionamiento (tramo). Si el ramal tiene un solo
 * fraccionamiento, esa es la tarifa exacta del ramal. Si tiene varios, se toma la del viaje
 * completo (la más alta) y la tarifa no es exacta: depende de dónde suba y baje el usuario.</p>
 *
 * @param tarifaViajeCompleto Tarifa regular más alta entre los fraccionamientos del ramal.
 * @param fracciones Cantidad de fraccionamientos que ARESEP publica para el ramal.
 */
public record TarifaRamal(BigDecimal tarifaViajeCompleto, int fracciones) {

    /**
     * Indica si la tarifa es exacta, es decir, si el ramal tiene un solo fraccionamiento.
     *
     * @return {@code true} si el ramal tiene exactamente un fraccionamiento.
     */
    public boolean esExacta() {
        return fracciones == 1;
    }
}