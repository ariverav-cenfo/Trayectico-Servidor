package ctpcliente.http;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ParametrosTest {

    @Test
    void codificaEnUtf8ConEspaciosComoPorcentaje20() {
        String s = Parametros.nuevos().con("a", "San José").con("filtros[0][valor]", "x'y").codificar();

        assertEquals("a=San%20Jos%C3%A9&filtros%5B0%5D%5Bvalor%5D=x%27y", s);
    }

    @Test
    void omiteValoresNulosYConservaElOrden() {
        Parametros p = Parametros.nuevos().con("b", 2).con("x", null).con("a", 1);

        assertEquals("b=2&a=1", p.codificar());
        assertNull(p.valor("x"));
        assertEquals("2", p.valor("b"));
    }

    @Test
    void aplicarARespetaConsultaExistente() {
        URI base = URI.create("https://h/p?s=T");

        assertEquals("https://h/p?s=T&a=1", Parametros.nuevos().con("a", 1).aplicarA(base).toString());
        assertEquals("https://h/x?a=1", Parametros.nuevos().con("a", 1).aplicarA(URI.create("https://h/x")).toString());
        assertSame(base, Parametros.nuevos().aplicarA(base));
    }
}
