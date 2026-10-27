package com.example.trayecticoservidor.util;

import ctpcliente.model.Coordenada;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class GeometriaWktTest {

    @Test
    void usaPuntoDecimalAunqueElIdiomaUseComa() {
        Locale anterior = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("es-CR"));
        try {
            String wkt = GeometriaWkt.linea(List.of(
                    new Coordenada(491500.5, 1098200.0),
                    new Coordenada(491600.0, 1098300.25)));
            assertEquals("LINESTRING(491500.500 1098200.000, 491600.000 1098300.250)", wkt);
        } finally {
            Locale.setDefault(anterior);
        }
    }

    @Test
    void menosDeDosPuntosNoEsLinea() {
        assertNull(GeometriaWkt.linea(List.of(new Coordenada(491500.0, 1098200.0))));
        assertNull(GeometriaWkt.linea(List.of()));
        assertNull(GeometriaWkt.linea(null));
    }
}
