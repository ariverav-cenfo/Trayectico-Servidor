package ctpcliente.core;

import ctpcliente.CtpConfig;
import ctpcliente.error.CtpNoEncontradoException;
import ctpcliente.soporte.TransporteSimulado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResolutorIdsTest {

    private TransporteSimulado transporte;
    private AtomicReference<Instant> ahora;
    private ResolutorIds ids;

    @BeforeEach
    void preparar() {
        transporte = new TransporteSimulado()
                .cuandoParametro("action", "getrutas").fixture("visor/getrutas.json")
                .cuandoParametro("action", "getramales").fixture("visor/getramales_56.json")
                .cuandoParametro("action", "getdesplazamientos").fixture("visor/getdesplazamientos_56_244.json")
                .cuandoParametro("action", "getredvialnacional").fixture("visor/getredvialnacional.json");
        ahora = new AtomicReference<>(Instant.parse("2026-10-03T12:00:00Z"));
        Clock reloj = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zona) {
                return this;
            }

            @Override
            public Instant instant() {
                return ahora.get();
            }
        };
        ids = new ResolutorIds(new VisorService(transporte, CtpConfig.BASE_VISOR), Duration.ofHours(1), reloj);
    }

    private long llamadas(String accion) {
        return transporte.peticiones().stream().filter(p -> accion.equals(p.parametro("action"))).count();
    }

    @Test
    void resuelveCodigosAIdsInternos() {
        assertEquals(56, ids.ruta("0002").id());
        assertEquals(244, ids.ramal("0002-A").id());
        assertEquals(539, ids.desplazamiento("0002-A-2").id());
        assertEquals("331", ids.carretera(1).id());
    }

    @Test
    void usaCacheDentroDeLaVigencia() {
        ids.ruta("0002");
        ids.ruta("0134");
        ids.ramal("0002-A");
        ids.ramal("0002-B");

        assertEquals(1, llamadas("getrutas"));
        assertEquals(1, llamadas("getramales"));
    }

    @Test
    void recargaAlVencer() {
        ids.ruta("0002");
        ahora.set(ahora.get().plus(Duration.ofHours(2)));
        ids.ruta("0002");

        assertEquals(2, llamadas("getrutas"));
    }

    @Test
    void codigoInexistente() {
        assertThrows(CtpNoEncontradoException.class, () -> ids.ruta("9999"));
        assertThrows(CtpNoEncontradoException.class, () -> ids.ramal("0002-Z"));
        assertThrows(CtpNoEncontradoException.class, () -> ids.carretera(9999));
    }

    @Test
    void codigoMalFormado() {
        assertThrows(IllegalArgumentException.class, () -> ids.ramal("A-0002"));
        assertThrows(IllegalArgumentException.class, () -> ids.desplazamiento("0002-A"));
    }

    @Test
    void limpiarFuerzaRecarga() {
        ids.ruta("0002");
        ids.limpiar();
        ids.ruta("0002");

        assertEquals(2, llamadas("getrutas"));
    }
}
