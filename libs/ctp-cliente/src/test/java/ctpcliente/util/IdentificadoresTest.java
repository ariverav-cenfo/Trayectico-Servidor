package ctpcliente.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentificadoresTest {

    @ParameterizedTest
    @CsvSource({
            "0002, true, false, false",
            "0002-A, false, true, false",
            "0002-A-1, false, false, true",
            "0736-ZD-2, false, false, true",
            "02 88-A-3, false, false, false",
            "736, false, false, false"
    })
    void clasificaCodigos(String codigo, boolean ruta, boolean ramal, boolean desplazamiento) {
        assertEquals(ruta, Identificadores.esRuta(codigo));
        assertEquals(ramal, Identificadores.esRamal(codigo));
        assertEquals(desplazamiento, Identificadores.esDesplazamiento(codigo));
    }

    @Test
    void descomponeElCodigoDeParada() {
        Identificadores.CodigoParada p = Identificadores.parada("PBA-10104-054").orElseThrow();

        assertEquals("PBA", p.prefijo());
        assertEquals("1", p.provincia());
        assertEquals("101", p.canton());
        assertEquals("10104", p.distrito());
        assertEquals(54, p.consecutivo());
        assertTrue(Identificadores.esParada("PBA-40101-006"));
        assertFalse(Identificadores.esParada("PBA-1010-054"));
        assertEquals(Optional.empty(), Identificadores.parada(null));
    }

    @Test
    void fraccionamiento() {
        assertTrue(Identificadores.esFraccionamiento("0134-FT-06"));
        assertFalse(Identificadores.esFraccionamiento("0134-F-06"));
    }

    @Test
    void rutaYRamalDeUnCodigo() {
        assertEquals(Optional.of("0002"), Identificadores.rutaDe("0002-A-1"));
        assertEquals(Optional.of("0002-A"), Identificadores.ramalDe("0002-A-1"));
        assertEquals(Optional.empty(), Identificadores.rutaDe("A-1"));
        assertEquals(Optional.empty(), Identificadores.ramalDe("0002-A"));
        assertEquals(Optional.empty(), Identificadores.ramalDe(null));
    }

    @Test
    void separaNombresDelCatalogo() {
        assertArrayEquals(new String[] {"0002-A", "SAN JOSÉ - SABANA CEMENTERIO"},
                Identificadores.separarNombre("0002-A,   SAN JOSÉ - SABANA CEMENTERIO "));
        assertArrayEquals(new String[] {"0002", ""}, Identificadores.separarNombre(" 0002 "));
    }
}
