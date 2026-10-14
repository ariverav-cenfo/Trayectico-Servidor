package ctpcliente.core;

import ctpcliente.CtpConfig;
import ctpcliente.auth.CtpCredenciales;
import ctpcliente.error.CtpOwsException;
import ctpcliente.model.ColeccionElementos;
import ctpcliente.model.Envolvente;
import ctpcliente.model.Geometria;
import ctpcliente.soporte.TransporteSimulado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WmsServiceTest {

    private static final Envolvente CENTRO_SJ = new Envolvente(-84.09, 9.925, -84.065, 9.945);

    private TransporteSimulado transporte;
    private WmsService wms;

    @BeforeEach
    void preparar() {
        transporte = new TransporteSimulado();
        wms = new WmsService(transporte, CtpConfig.BASE_GEO, new CtpCredenciales("token-prueba"));
    }

    private static ConsultaWms consulta() {
        return ConsultaWms.de(CapaWfs.DESPLAZAMIENTOS).envolvente(CENTRO_SJ, ConsultaWfs.WGS84).tamano(64, 64).construir();
    }

    @Test
    void urlMapaIncluyeTodosLosParametros() {
        URI uri = wms.urlMapa(ConsultaWms.de("personalizada_desplazamientos_de_ruta_ramal")
                .envolvente(CENTRO_SJ, ConsultaWfs.WGS84).vista("ruta", 56).construir());

        Map<String, String> q = new TransporteSimulado.Peticion("GET", uri, null).consulta();
        assertAll(
                () -> assertEquals("token-prueba", q.get("s")),
                () -> assertEquals("WMS", q.get("service")),
                () -> assertEquals("1.1.1", q.get("version")),
                () -> assertEquals("GetMap", q.get("request")),
                () -> assertEquals("CTP_PUBLICO:personalizada_desplazamientos_de_ruta_ramal", q.get("layers")),
                () -> assertEquals("-84.09,9.925,-84.065,9.945", q.get("bbox")),
                () -> assertEquals("ruta:56;", q.get("VIEWPARAMS")),
                () -> assertEquals("", q.get("styles")));
    }

    @Test
    void mapaDevuelvePng() {
        transporte.cuandoParametro("request", "GetMap").fixture("wms/mapa.png");

        assertTrue(WmsService.esPng(wms.mapa(consulta())));
    }

    @Test
    void mapaConErrorWmsLanzaExcepcionOws() {
        transporte.cuandoParametro("request", "GetMap").fixture("wms/capa_inexistente.xml");

        CtpOwsException e = assertThrows(CtpOwsException.class, () -> wms.mapa(consulta()));
        assertEquals("LayerNotDefined", e.codigo());
        assertTrue(e.getMessage().contains("Could not find layer"));
        assertFalse(e.getMessage().contains("<"));
    }

    @Test
    void elementosEnPixelLeeGeoJson() {
        transporte.cuandoParametro("request", "GetFeatureInfo").fixture("wms/info_pixel.json");

        ColeccionElementos c = wms.elementosEnPixel(ConsultaWms.de(CapaWfs.PARADAS_X_DESPLAZAMIENTO)
                .envolvente(CENTRO_SJ, ConsultaWfs.WGS84).tamano(900, 700).construir(), 450, 350, 2);

        assertEquals(2, c.tamano());
        assertInstanceOf(Geometria.Punto.class, c.elementos().getFirst().geometria());
        assertEquals("450", transporte.ultima().parametro("x"));
        assertEquals("CTP_PUBLICO:paradas_x_desplazamiento", transporte.ultima().parametro("query_layers"));
    }

    @Test
    void pixelFueraDeLaImagenFalla() {
        assertThrows(IllegalArgumentException.class, () -> wms.elementosEnPixel(consulta(), 64, 0, 1));
        assertTrue(transporte.peticiones().isEmpty());
    }

    @Test
    void urlLeyenda() {
        URI uri = wms.urlLeyenda(CapaWfs.DESPLAZAMIENTOS);

        assertTrue(uri.toString().contains("request=GetLegendGraphic"));
        assertTrue(uri.toString().contains("layer=CTP_PUBLICO%3Adesplazamientos"));
    }

    @Test
    void validaciones() {
        assertThrows(IllegalArgumentException.class, () -> ConsultaWms.de("capa;DROP").construir());
        assertThrows(IllegalArgumentException.class,
                () -> ConsultaWms.de(CapaWfs.DESPLAZAMIENTOS).tamano(0, 10));
        assertThrows(IllegalArgumentException.class,
                () -> ConsultaWms.de(CapaWfs.DESPLAZAMIENTOS).vista("ruta", "56;x:1"));
        assertThrows(NullPointerException.class, () -> ConsultaWms.de(CapaWfs.DESPLAZAMIENTOS).construir());
        assertFalse(WmsService.esPng(new byte[] {1, 2}));
    }
}
