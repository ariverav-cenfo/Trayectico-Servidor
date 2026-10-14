package ctpcliente.util;

import ctpcliente.model.Envolvente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CqlTest {

    @Test
    void escapaComillasEnLiterales() {
        assertEquals("descripcion = 'O''Higgins'", Cql.igual("descripcion", "O'Higgins"));
        assertEquals("descripcion = 'x'' OR 1=1 OR ''a''=''a'",
                Cql.igual("descripcion", "x' OR 1=1 OR 'a'='a"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1campo", "campo;", "campo OR 1=1", "a.b", "campo'"})
    void rechazaCamposInvalidos(String campo) {
        assertThrows(IllegalArgumentException.class, () -> Cql.campo(campo));
    }

    @Test
    void operadores() {
        assertEquals("longitud = 5", Cql.igual("longitud", 5));
        assertEquals("estado <> 'activo'", Cql.distinto("estado", "activo"));
        assertEquals("longitud > 50000.0", Cql.mayorQue("longitud", 50_000));
        assertEquals("longitud < 1.5", Cql.menorQue("longitud", 1.5));
        assertEquals("tipo_caseta IN ('2','3')", Cql.en("tipo_caseta", List.of("2", "3")));
        assertEquals("bahia IS NULL", Cql.esNulo("bahia"));
    }

    @Test
    void comodines() {
        assertEquals("identificador_ramal LIKE '0002-%'", Cql.empiezaCon("identificador_ramal", "0002-"));
        assertEquals("descripcion ILIKE '%50_%'", Cql.contiene("descripcion", "50%"));
        assertEquals("descripcion ILIKE '%a_b_c%'", Cql.contiene("descripcion", "a\\b_c"));
        assertEquals("descripcion ILIKE '%O''Higgins%'", Cql.contiene("descripcion", "O'Higgins"));
        assertEquals("a LIKE 'x_y'", Cql.como("a", "x_y"));
    }

    @Test
    void combinaciones() {
        assertEquals("(a = 1) AND (b = 2)", Cql.y(Cql.igual("a", 1), Cql.igual("b", 2)));
        assertEquals("(a = 1) OR (b = 2)", Cql.o(Cql.igual("a", 1), Cql.igual("b", 2)));
        assertEquals("a = 1", Cql.y(Cql.igual("a", 1)));
        assertEquals("NOT (a = 1)", Cql.no(Cql.igual("a", 1)));
        assertThrows(IllegalArgumentException.class, Cql::y);
    }

    @Test
    void bbox() {
        assertEquals("BBOX(geometria_crtm05,490000.0,1097000.0,492500.0,1099500.0,'EPSG:5367')",
                Cql.bbox("geometria_crtm05", new Envolvente(490_000, 1_097_000, 492_500, 1_099_500), "EPSG:5367"));
        assertThrows(IllegalArgumentException.class,
                () -> Cql.bbox("g", new Envolvente(0, 0, 1, 1), "EPSG:4326')--"));
    }

    @Test
    void listaVaciaYNulos() {
        assertThrows(IllegalArgumentException.class, () -> Cql.en("a", List.of()));
        assertThrows(IllegalArgumentException.class, () -> Cql.literal(null));
        assertThrows(IllegalArgumentException.class, () -> Cql.mayorQue("a", Double.NaN));
    }
}
