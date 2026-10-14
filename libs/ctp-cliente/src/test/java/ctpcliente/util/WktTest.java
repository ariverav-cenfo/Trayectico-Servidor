package ctpcliente.util;

import ctpcliente.model.Coordenada;
import ctpcliente.model.Geometria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WktTest {

    @Test
    void punto() {
        Geometria g = Wkt.leer("POINT(491641.116 1098301.244)");

        assertEquals(new Geometria.Punto(new Coordenada(491641.116, 1098301.244)), g);
    }

    @Test
    void puntoConEspacioYMinusculas() {
        assertInstanceOf(Geometria.Punto.class, Wkt.leer("  point (1 2)  "));
    }

    @Test
    void linea() {
        Geometria.Linea l = (Geometria.Linea) Wkt.leer("LINESTRING(1 2, 3 4,5 6)");

        assertEquals(List.of(new Coordenada(1, 2), new Coordenada(3, 4), new Coordenada(5, 6)), l.puntos());
    }

    @Test
    void multiLinea() {
        Geometria.MultiLinea m = (Geometria.MultiLinea) Wkt.leer("MULTILINESTRING((1 2,3 4),(5 6,7 8,9 10))");

        assertEquals(2, m.lineas().size());
        assertEquals(5, m.vertices().size());
    }

    @Test
    void poligonoYEnvolvente() {
        Geometria g = Wkt.leer("POLYGON((0 0,4 0,4 3,0 3,0 0))");

        assertInstanceOf(Geometria.Poligono.class, g);
        assertEquals(4, g.envolvente().maxX());
        assertEquals(3, g.envolvente().maxY());
    }

    @Test
    void multiPuntoEnAmbasFormas() {
        assertEquals(2, Wkt.leer("MULTIPOINT((1 2),(3 4))").vertices().size());
        assertEquals(2, Wkt.leer("MULTIPOINT(1 2, 3 4)").vertices().size());
    }

    @Test
    void ignoraLaTerceraDimension() {
        assertEquals(new Coordenada(1, 2), Wkt.leer("POINT(1 2 3)").vertices().getFirst());
    }

    @Test
    void notacionCientifica() {
        assertEquals(new Coordenada(1.5e5, -2.0e-3), Wkt.leer("POINT(1.5E5 -2.0e-3)").vertices().getFirst());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "POINT", "POINT(1)", "POINT(1 2", "POINT(1 2) x", "CIRCLE(1 2)", "LINESTRING(a b)"})
    void textosInvalidos(String wkt) {
        assertThrows(IllegalArgumentException.class, () -> Wkt.leer(wkt));
    }

    @Test
    void nulo() {
        assertThrows(IllegalArgumentException.class, () -> Wkt.leer(null));
    }
}
