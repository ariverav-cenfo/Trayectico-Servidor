package ctpcliente;

import ctpcliente.auth.CtpCredenciales;
import ctpcliente.core.CapaWfs;
import ctpcliente.core.ConsultaWfs;
import ctpcliente.soporte.TransporteSimulado;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CtpClienteTest {

    @Test
    void conectaLosServiciosConLaConfiguracion() {
        TransporteSimulado t = new TransporteSimulado()
                .cuandoParametro("action", "getrutas").fixture("visor/getrutas.json")
                .cuandoParametro("request", "GetFeature").fixture("wfs/paradas.json");
        CtpConfig config = CtpConfig.constructor()
                .baseVisor(URI.create("https://visor.prueba"))
                .baseGeo(URI.create("https://geo.prueba/proxy"))
                .credenciales(new CtpCredenciales("TOKEN"))
                .construir();

        try (CtpCliente ctp = CtpCliente.con(config, t, Clock.systemUTC())) {
            assertEquals(56, ctp.ids().ruta("0002").id());
            assertEquals("visor.prueba", t.ultima().uri().getHost());

            ctp.wfs().obtener(ConsultaWfs.de(CapaWfs.PARADA_CONSULTABLE).construir());
            assertEquals("geo.prueba", t.ultima().uri().getHost());
            assertEquals("TOKEN", t.ultima().parametro("s"));
            assertTrue(ctp.wms().urlLeyenda(CapaWfs.DESPLAZAMIENTOS).toString().startsWith("https://geo.prueba/proxy?s=TOKEN"));
            assertTrue(ctp.consultas() != null && ctp.visor() != null);
        }
    }

    @Test
    void configuracionPorDefectoYValidaciones() {
        CtpConfig c = CtpConfig.porDefecto();

        assertEquals(CtpConfig.BASE_VISOR, c.baseVisor());
        assertEquals(3, c.reintentos());
        assertEquals(c, c.aConstructor().construir());
        assertThrows(IllegalArgumentException.class, () -> CtpConfig.constructor().reintentos(-1).construir());
        assertThrows(IllegalArgumentException.class, () -> CtpConfig.constructor().tamanoMaximo(0).construir());
        assertEquals(Duration.ofHours(12), CtpCliente.VIGENCIA_CATALOGOS);
    }
}
