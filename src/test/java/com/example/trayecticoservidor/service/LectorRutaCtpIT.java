package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dto.DesplazamientoImportado;
import com.example.trayecticoservidor.dto.ParadaImportada;
import com.example.trayecticoservidor.dto.RutaImportada;
import ctpcliente.CtpCliente;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Llama al CTP real: necesita internet, pero no la base de datos
// Se corre a mano desde IntelliJ. Como termina en IT, el CI no la ejecuta
class LectorRutaCtpIT {

    @Test
    void leeLaRuta0002Real() {
        try (CtpCliente ctp = CtpCliente.crear()) {
            RutaImportada r = new LectorRutaCtpService(ctp).leer("0002");

            assertEquals("0002", r.codigo());
            assertFalse(r.ramales().isEmpty());

            DesplazamientoImportado d = r.ramales().getFirst().desplazamientos().getFirst();
            assertFalse(d.paradas().isEmpty());
            assertFalse(d.trazado().isEmpty());
            assertNotNull(d.sentido());
            assertNotNull(d.estadoCtp());
            assertTrue(d.longitudM() != null && d.longitudM() > 0);

            ParadaImportada p = d.paradas().getFirst();
            assertNotNull(p.distrito(), "código de parada sin distrito: " + p.codigo());

            // Si las coordenadas están en CRTM05, x anda entre 200 000 y 700 000 metros.
            assertTrue(p.x() > 200_000 && p.x() < 700_000, "x fuera de CRTM05: " + p.x());

            System.out.println(r);
        }
    }
}
