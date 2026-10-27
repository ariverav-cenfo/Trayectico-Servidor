package com.example.trayecticoservidor.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UbicacionParadaTest {

    @Test
    void sacaLosCodigosInecDelCodigoDeParada() {
        UbicacionParada u = UbicacionParada.desdeCodigo(" PBA-10104-054 ");
        assertEquals("1", u.provincia());
        assertEquals("101", u.canton());
        assertEquals("10104", u.distrito());
    }

    @Test
    void codigoMalFormadoDevuelveVacio() {
        assertNull(UbicacionParada.desdeCodigo("PBA-1O104-054").distrito()); // letra O
        assertNull(UbicacionParada.desdeCodigo("PBA").distrito());
        assertNull(UbicacionParada.desdeCodigo(null).provincia());
    }
}
