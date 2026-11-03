package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.RutaDAO;
import com.example.trayecticoservidor.dto.RutaImportada;
import ctpcliente.error.CtpNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Sin internet ni base de datos. Corre en el CI
class ImportadorRutaTest {

    private final LectorRutaCtpService lector = mock(LectorRutaCtpService.class);
    private final RutaDAO rutaDAO = mock(RutaDAO.class);

    @Test
    void leeLaRutaPedidaYLaGuarda() {
        RutaImportada ruta = new RutaImportada(56, "0002", List.of());
        when(lector.leer("0002")).thenReturn(ruta);

        new ImportadorRuta(lector, rutaDAO, "0002").run(null);

        verify(lector).leer("0002");
        verify(rutaDAO).guardar(ruta);
    }

    /** Varias rutas: se importan todas, en el orden de la lista. */
    @Test
    void importaVariasRutasEnElOrdenDeLaLista() {
        RutaImportada r1 = rutaQueSeLee("0002");
        RutaImportada r2 = rutaQueSeLee("0090");
        RutaImportada r3 = rutaQueSeLee("0403");

        new ImportadorRuta(lector, rutaDAO, "0002,0090,0403").run(null);

        InOrder orden = inOrder(lector, rutaDAO);
        orden.verify(lector).leer("0002");
        orden.verify(rutaDAO).guardar(r1);
        orden.verify(lector).leer("0090");
        orden.verify(rutaDAO).guardar(r2);
        orden.verify(lector).leer("0403");
        orden.verify(rutaDAO).guardar(r3);
    }

    @Test
    void quitaLosEspaciosAlrededorDeCadaCodigo() {
        assertEquals(List.of("0002", "0090", "0403"), ImportadorRuta.codigos(" 0002 , 0090 ,0403 "));
    }

    @Test
    void ignoraLosElementosVacios() {
        assertEquals(List.of("0002", "0090"), ImportadorRuta.codigos("0002,,0090,"));
    }

    /** Un código repetido se importa una sola vez y se respeta su primera posición. */
    @Test
    void unCodigoRepetidoSeImportaUnaSolaVez() {
        rutaQueSeLee("0002");
        rutaQueSeLee("0090");

        new ImportadorRuta(lector, rutaDAO, "0002,0090, 0002").run(null);

        assertEquals(List.of("0002", "0090"), ImportadorRuta.codigos("0002,0090, 0002"));
        verify(lector, times(1)).leer("0002");
        verify(rutaDAO, times(2)).guardar(any());
    }

    /** Una lista sin códigos falla al crear el importador, antes de llamar al CTP o a la base. */
    @ParameterizedTest
    @ValueSource(strings = {"", " ", ",", " , , "})
    void unaListaSinCodigosFallaSinLlamarAlCtp(String valor) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new ImportadorRuta(lector, rutaDAO, valor));

        assertTrue(e.getMessage().contains("trayectico.importar-ruta"));
        verifyNoInteractions(lector, rutaDAO);
    }

    /** Si el CTP falla con una ruta, se importan las demás y al final se termina con error. */
    @Test
    void unaRutaQueFallaEnElCtpNoDetieneLasDemas() {
        rutaQueSeLee("0002");
        when(lector.leer("9999")).thenThrow(new CtpNoEncontradoException("Ruta no encontrada: 9999"));
        RutaImportada r3 = rutaQueSeLee("0090");

        ImportadorRuta importador = new ImportadorRuta(lector, rutaDAO, "0002,9999,0090");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> importador.run(null));

        verify(rutaDAO).guardar(r3);
        verify(rutaDAO, times(2)).guardar(any());
        assertTrue(e.getMessage().contains("9999 (Ruta no encontrada: 9999)"), e.getMessage());
        assertTrue(e.getMessage().contains("1 de 3"), e.getMessage());
    }

    /** Si la base rechaza una ruta, también se sigue con las demás. */
    @Test
    void unaRutaQueFallaAlGuardarNoDetieneLasDemas() {
        RutaImportada r1 = rutaQueSeLee("0002");
        RutaImportada r2 = rutaQueSeLee("0090");
        doThrow(new DataIntegrityViolationException("dato invalido")).when(rutaDAO).guardar(r1);

        ImportadorRuta importador = new ImportadorRuta(lector, rutaDAO, "0002,0090");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> importador.run(null));

        verify(rutaDAO).guardar(r2);
        assertTrue(e.getMessage().contains("0002 (dato invalido)"), e.getMessage());
    }

    /** Con una sola ruta que falla, el arranque termina con error y no se guarda nada. */
    @Test
    void unaSolaRutaQueFallaTerminaConError() {
        when(lector.leer("0002")).thenThrow(new IllegalStateException("La parada X viene sin ubicación"));

        ImportadorRuta importador = new ImportadorRuta(lector, rutaDAO, "0002");
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> importador.run(null));

        assertTrue(e.getMessage().contains("0002 (La parada X viene sin ubicación)"), e.getMessage());
        verifyNoInteractions(rutaDAO);
    }

    private RutaImportada rutaQueSeLee(String codigo) {
        RutaImportada ruta = new RutaImportada(Integer.parseInt(codigo), codigo, List.of());
        when(lector.leer(codigo)).thenReturn(ruta);
        return ruta;
    }
}