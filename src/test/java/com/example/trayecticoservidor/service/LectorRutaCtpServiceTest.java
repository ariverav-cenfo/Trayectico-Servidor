package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dto.DesplazamientoImportado;
import com.example.trayecticoservidor.dto.ParadaImportada;
import com.example.trayecticoservidor.dto.RamalImportado;
import com.example.trayecticoservidor.dto.RutaImportada;
import ctpcliente.CtpCliente;
import ctpcliente.model.Coordenada;
import ctpcliente.model.Desplazamiento;
import ctpcliente.model.ParadaEnRecorrido;
import ctpcliente.model.Ramal;
import ctpcliente.model.Ruta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

// Sin internet ni base de datos: el CTP se simula. Corre en el CI.
class LectorRutaCtpServiceTest {

    private CtpCliente ctp;
    private LectorRutaCtpService lector;

    @BeforeEach
    void preparar() {
        ctp = mock(CtpCliente.class, RETURNS_DEEP_STUBS);

        Ruta ruta = new Ruta(56, "0002");
        Ramal ramal = new Ramal(244, "0002-A", "SAN JOSÉ - SABANA CEMENTERIO");

        Desplazamiento desp = mock(Desplazamiento.class);
        when(desp.id()).thenReturn(539);   // si da error, cambiar a 539L
        when(desp.codigo()).thenReturn("0002-A-2");

        when(ctp.ids().ruta("0002")).thenReturn(ruta);
        when(ctp.visor().ramales(56)).thenReturn(List.of(ramal));
        when(ctp.visor().desplazamientos(56, 244)).thenReturn(List.of(desp));

        // spy: el lector real, pero con la consulta al WFS reemplazada en cada prueba.
        lector = spy(new LectorRutaCtpService(ctp));
    }

    private void recorridoConEstado(String estado) {
        doReturn(new LectorRutaCtpService.DatosWfs("1-2", 5000, estado, List.of()))
                .when(lector).datosWfs("0002-A-2");
    }

    private void paradasDelRecorrido(ParadaEnRecorrido... paradas) {
        when(ctp.consultas().paradasDeRecorrido(56, 244, 539)).thenReturn(List.of(paradas));
    }

    private static ParadaEnRecorrido parada(long id, Optional<Coordenada> ubicacion) {
        return new ParadaEnRecorrido(id, "PBA-10104-054", "EN LA CCSS", "FRENTE A LA CCSS",
                1, null, null, "0002-A", "0002", "1", "101", "10104", null, ubicacion);
    }

    @Test
    void convierteLaRutaDelCtpEnRutaImportada() {
        recorridoConEstado("activo");
        paradasDelRecorrido(parada(1001L, Optional.of(new Coordenada(491500.0, 1098200.0))));

        RutaImportada r = lector.leer("0002");

        assertEquals(56, r.ctpId());
        assertEquals("0002", r.codigo());

        RamalImportado ra = r.ramales().getFirst();
        assertEquals("0002-A", ra.codigo());
        assertEquals("SAN JOSÉ - SABANA CEMENTERIO", ra.descripcion());

        DesplazamientoImportado d = ra.desplazamientos().getFirst();
        assertEquals(539, d.ctpId());
        assertEquals("1-2", d.sentido());
        assertEquals(5000, d.longitudM());
        assertEquals("activo", d.estadoCtp());

        ParadaImportada p = d.paradas().getFirst();
        assertEquals("PBA-10104-054", p.codigo());
        assertEquals(1001L, p.ctpId());
        assertEquals(1, p.orden());
        assertEquals("EN LA CCSS", p.descripcion());
        assertEquals(491500.0, p.x());
        assertEquals("1", p.provincia());
        assertEquals("101", p.canton());
        assertEquals("10104", p.distrito());
    }

    @Test
    void recorridoInactivoNoSeImportaNiSePidenSusParadas() {
        recorridoConEstado("inactivo");

        RutaImportada r = lector.leer("0002");

        assertTrue(r.ramales().getFirst().desplazamientos().isEmpty());
        verify(ctp.consultas(), never()).paradasDeRecorrido(anyInt(), anyInt(), anyInt());
    }

    @Test
    void recorridoSinDatosEnElWfsNoSeImporta() {
        recorridoConEstado(null);

        RutaImportada r = lector.leer("0002");

        assertTrue(r.ramales().getFirst().desplazamientos().isEmpty());
    }

    @Test
    void idCeroDeLaParadaSeGuardaComoNull() {
        recorridoConEstado("activo");
        paradasDelRecorrido(parada(0L, Optional.of(new Coordenada(491500.0, 1098200.0))));

        ParadaImportada p = lector.leer("0002")
                .ramales().getFirst().desplazamientos().getFirst().paradas().getFirst();

        assertNull(p.ctpId());
    }

    @Test
    void paradaSinUbicacionDetieneLaLectura() {
        recorridoConEstado("activo");
        paradasDelRecorrido(parada(1001L, Optional.empty()));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> lector.leer("0002"));
        assertTrue(e.getMessage().contains("PBA-10104-054"));
    }

    @Test
    void reconoceElEstadoActivoSinImportarMayusculasNiEspacios() {
        assertTrue(LectorRutaCtpService.esActivo(" Activo "));
        assertFalse(LectorRutaCtpService.esActivo("inactivo"));
        assertFalse(LectorRutaCtpService.esActivo(null));
    }
}
