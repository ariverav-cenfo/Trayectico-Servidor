package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.model.TramoRutaInterno;

import java.math.BigDecimal;
import java.util.List;

public interface CalculoViajeDAO {
    List<List<TramoRutaInterno>> buscarOpcionesViaje(
            BigDecimal origenLat,
            BigDecimal origenLon,
            BigDecimal destinoLat,
            BigDecimal destinoLon,
            double maxDistanciaCaminataMetros
    );
}
