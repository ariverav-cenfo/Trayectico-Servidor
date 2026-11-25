package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.model.TramoRutaInterno;

import java.math.BigDecimal;
import java.util.List;

/**
 * Proposito: DAO para la busqueda de opciones, contiene los datos del request para realizar la busqueda en la DB
 */

public interface CalculoViajeDAO {
    List<List<TramoRutaInterno>> buscarOpcionesViaje(
            BigDecimal origenLat,
            BigDecimal origenLon,
            BigDecimal destinoLat,
            BigDecimal destinoLon,
            double maxDistanciaCaminataMetros
    );
}
