package com.example.trayecticoservidor.util;


import ctpcliente.model.Coordenada;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

// Convierte una lista de puntos en el texto que entiende PostGIS: LINESTRING(x yt, x y, ..)
public final class GeometriaWkt {

    private GeometriaWkt() { }

    // null si no hay al menos dos puntos: un LINESTRING de un solo punto es inválido
    public static String linea(List<Coordenada> puntos) {
        if (puntos == null || puntos.size() < 2) {
            return null;
        }
        // Locale.ROOT es obligatorio: con el idioma de Costa Rica, Java escribe "491500,5 y PostGIS no lo entiende"
        return puntos.stream()
                .map(coordenada -> String.format(Locale.ROOT, "%.3f %.3f", coordenada.x(), coordenada.y()))
                .collect(Collectors.joining(", ", "LINESTRING(", ")"));
    }
}
