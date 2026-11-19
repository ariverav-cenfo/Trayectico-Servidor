package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dto.TarifaRamalDTO;

import java.util.Map;

/**
 * Servicio de consulta de las tarifas de ARESEP de los ramales del CTP (RF-TP-01).
 */
public interface TarifaService {

    /**
     * Obtiene las tarifas de todos los ramales de una ruta.
     *
     * @param codigoRuta código del CTP de la ruta, p. ej. {@code 0002}.
     * @return tarifa por código del CTP del ramal; los ramales sin tarifa disponible no aparecen.
     */
    Map<String, TarifaRamalDTO> obtenerTarifasDeRuta(String codigoRuta);
}