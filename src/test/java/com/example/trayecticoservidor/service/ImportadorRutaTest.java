package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.RutaDAO;
import com.example.trayecticoservidor.dto.RutaImportada;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

// Sin internet ni base de datos. Corre en el CI
class ImportadorRutaTest {

    @Test
    void leeLaRutaPedidaYLaGuarda() {
        LectorRutaCtpService lector = mock(LectorRutaCtpService.class);
        RutaDAO rutaDAO = mock(RutaDAO.class);
        RutaImportada ruta = new RutaImportada(56, "0002", List.of());
        when(lector.leer("0002")).thenReturn(ruta);

        new ImportadorRuta(lector, rutaDAO, "0002").run(null);

        verify(lector).leer("0002");
        verify(rutaDAO).guardar(ruta);
    }
}
