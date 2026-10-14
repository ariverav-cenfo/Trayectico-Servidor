package ctpcliente.core;

import ctpcliente.CtpConfig;
import ctpcliente.error.CtpRespuestaVaciaException;
import ctpcliente.error.CtpServicioException;
import ctpcliente.model.Desplazamiento;
import ctpcliente.model.ParadaCatalogo;
import ctpcliente.model.Ramal;
import ctpcliente.model.Ruta;
import ctpcliente.model.RutaNacional;
import ctpcliente.soporte.TransporteSimulado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisorServiceTest {

    private TransporteSimulado transporte;
    private VisorService visor;

    @BeforeEach
    void preparar() {
        transporte = new TransporteSimulado()
                .cuandoParametro("action", "getrutas").fixture("visor/getrutas.json")
                .cuandoParametro("action", "getramales").fixture("visor/getramales_56.json")
                .cuandoParametro("action", "getdesplazamientos").fixture("visor/getdesplazamientos_56_244.json")
                .cuandoParametro("action", "getparadas").fixture("visor/getparadas.json")
                .cuandoParametro("action", "getredvialnacional").fixture("visor/getredvialnacional.json")
                .cuandoParametro("action", "getredvialnacionalruar").fixture("visor/getredvialnacionalruar.json")
                .cuandoParametro("action", "provincias").fixture("visor/provincias.json")
                .cuandoParametro("action", "cantones").fixture("visor/cantones_1.json")
                .cuandoParametro("action", "distritos").fixture("visor/distritos_101.json");
        visor = new VisorService(transporte, CtpConfig.BASE_VISOR);
    }

    @Test
    void rutasNormalizaElCodigo() {
        List<Ruta> rutas = visor.rutas();

        assertTrue(rutas.contains(new Ruta(56, "0002")));
        assertTrue(rutas.contains(new Ruta(999, "0999")));
        assertEquals("/Visor/service/ctp", transporte.ultima().uri().getPath());
    }

    @Test
    void ramalesSeparaCodigoYDescripcionYEnviaElIdInterno() {
        List<Ramal> ramales = visor.ramales(56);

        assertEquals("56", transporte.ultima().parametro("ruta"));
        assertTrue(ramales.contains(new Ramal(244, "0002-A", "SAN JOSÉ - SABANA CEMENTERIO")));
    }

    @Test
    void desplazamientosIncluyeSentidoYEnvolvente() {
        List<Desplazamiento> lista = visor.desplazamientos(56, 244);

        Desplazamiento d = lista.stream().filter(x -> x.id() == 539).findFirst().orElseThrow();
        assertAll(
                () -> assertEquals("0002-A-2", d.codigo()),
                () -> assertEquals("1-2", d.sentido()),
                () -> assertTrue(d.envolvente().isPresent()),
                () -> assertEquals("244", transporte.ultima().parametro("ramal")));
    }

    @Test
    void paradasTraeUbicacionEnCrtm05() {
        ParadaCatalogo p = visor.paradas().getFirst();

        assertTrue(p.ubicacion().orElseThrow().x() > 250_000);
        assertTrue(p.ubicacion().orElseThrow().y() > 800_000);
    }

    @Test
    void rutasNacionalesDistingueIdDeNumero() {
        RutaNacional porId = visor.rutasNacionales().getFirst();
        RutaNacional porNumero = visor.rutasNacionalesPorNumero().getFirst();

        assertEquals(new RutaNacional("331", 1, porId.envolvente()), porId);
        assertEquals("1", porNumero.id());
        assertEquals(1, porNumero.numero());
    }

    @Test
    void divisionesAdministrativas() {
        assertEquals(7, visor.provincias().size());
        assertTrue(visor.cantones("1").stream().anyMatch(c -> c.id().equals("101")));
        assertEquals("1", transporte.ultima().parametro("provincia"));
        assertTrue(visor.distritos("101").stream().anyMatch(d -> d.id().equals("10104") && d.nombre().equals("Catedral")));
        assertEquals("/Visor/service/selectoresseriesdatos", transporte.ultima().uri().getPath());
    }

    @Test
    void cuerpoVacioLanzaRespuestaVacia() {
        transporte.cuandoParametro("action", "getrutas").texto("");

        assertThrows(CtpRespuestaVaciaException.class, visor::rutas);
    }

    @Test
    void resultFalseLanzaErrorDeServicio() {
        transporte.cuandoParametro("action", "getrutas").fixture("visor/resultado_falso.json");

        assertThrows(CtpServicioException.class, visor::rutas);
    }
}
