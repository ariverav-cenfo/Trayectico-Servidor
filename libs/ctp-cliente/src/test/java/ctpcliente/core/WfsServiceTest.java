package ctpcliente.core;

import ctpcliente.CtpConfig;
import ctpcliente.auth.CtpCredenciales;
import ctpcliente.error.CtpOwsException;
import ctpcliente.error.CtpRespuestaVaciaException;
import ctpcliente.model.ColeccionElementos;
import ctpcliente.model.Elemento;
import ctpcliente.model.Envolvente;
import ctpcliente.model.Geometria;
import ctpcliente.soporte.Fixtures;
import ctpcliente.soporte.TransporteSimulado;
import ctpcliente.util.Cql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WfsServiceTest {

    private TransporteSimulado transporte;
    private WfsService wfs;

    @BeforeEach
    void preparar() {
        transporte = new TransporteSimulado();
        wfs = new WfsService(transporte, CtpConfig.BASE_GEO, new CtpCredenciales("token-prueba"));
    }

    private static ConsultaWfs desplazamientos0002() {
        return ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS)
                .filtro(Cql.empiezaCon("identificador_desplazamiento", "0002-"))
                .construir();
    }

    @Test
    void obtenerArmaLaPeticionYLeeGeoJson() {
        transporte.cuandoParametro("request", "GetFeature").fixture("wfs/desplazamientos_0002.json");

        ColeccionElementos c = wfs.obtener(desplazamientos0002());

        Map<String, String> q = transporte.ultima().consulta();
        assertAll(
                () -> assertEquals("token-prueba", q.get("s")),
                () -> assertEquals("WFS", q.get("service")),
                () -> assertEquals("1.1.0", q.get("version")),
                () -> assertEquals("CTP_PUBLICO:desplazamientos", q.get("typeName")),
                () -> assertEquals("EPSG:4326", q.get("srsName")),
                () -> assertEquals("application/json", q.get("outputFormat")),
                () -> assertEquals("identificador_desplazamiento LIKE '0002-%'", q.get("CQL_FILTER")),
                () -> assertEquals(3, c.tamano()),
                () -> assertEquals(3L, c.total().orElseThrow()));
        Elemento e = c.elementos().getFirst();
        assertInstanceOf(Geometria.Linea.class, e.geometria());
        assertTrue(e.texto("identificador_desplazamiento").orElseThrow().startsWith("0002-"));
        assertTrue(e.entero("longitud").orElseThrow() > 1000);
        double lon = e.geometria().vertices().getFirst().longitud();
        assertTrue(lon > -84.2 && lon < -84.0);
    }

    @Test
    void contarLeeNumberOfFeaturesYQuitaPaginacion() {
        transporte.cuandoParametro("resultType", "hits").fixture("wfs/hits.xml");

        long n = wfs.contar(ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).maximo(5).inicio(10)
                .ordenarPor("id", false).propiedades("id").construir());

        Map<String, String> q = transporte.ultima().consulta();
        assertEquals(3070, n);
        assertTrue(!q.containsKey("maxFeatures") && !q.containsKey("startIndex") && !q.containsKey("sortBy"));
    }

    @Test
    void recorrerProcesaElementosEnFlujo() {
        transporte.cuandoParametro("request", "GetFeature").fixture("wfs/paradas.json");
        List<Elemento> vistos = new ArrayList<>();

        long n = wfs.recorrer(ConsultaWfs.de(CapaWfs.PARADA_CONSULTABLE).construir(), vistos::add);

        assertEquals(3, n);
        assertEquals(3, vistos.size());
        assertInstanceOf(Geometria.Punto.class, vistos.getFirst().geometria());
    }

    @Test
    void recorrerDetectaExcepcionDeGeoServer() {
        transporte.cuandoParametro("request", "GetFeature").fixture("wfs/capa_rota.xml");

        CtpOwsException e = assertThrows(CtpOwsException.class,
                () -> wfs.recorrer(ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).construir(), x -> { }));
        assertTrue(e.getMessage().contains("does not exist"));
    }

    @Test
    void recorrerConCuerpoVacioLanzaRespuestaVacia() {
        transporte.cuandoParametro("request", "GetFeature").texto("  ");

        assertThrows(CtpRespuestaVaciaException.class,
                () -> wfs.recorrer(ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).construir(), x -> { }));
    }

    @Test
    void obtenerConExcepcionOwsConHttp200() {
        transporte.cuandoParametro("request", "GetFeature").fixture("wfs/vista_invalida.xml");

        CtpOwsException e = assertThrows(CtpOwsException.class, () -> wfs.obtener(
                ConsultaWfs.de(CapaWfs.DESPLAZAMIENTO_USA_PARADA).vista("parada", "1").construir()));
        assertTrue(e.getMessage().contains("Invalid value for parameter parada"));
    }

    @Test
    void paginarPideHastaUnaPaginaIncompleta() {
        transporte.cuando(p -> "GetFeature".equals(p.parametro("request"))).bytes(() -> {
            int inicio = Integer.parseInt(transporte.ultima().parametro("startIndex"));
            int tamano = inicio < 4 ? 2 : 1;
            return paginaSintetica(inicio, tamano);
        });

        List<Elemento> todos = wfs.paginar(ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).ordenarPor("id", false).construir(), 2)
                .toList();

        assertEquals(5, todos.size());
        assertEquals(List.of("0", "2", "4"), transporte.peticiones().stream().map(p -> p.parametro("startIndex")).toList());
        assertEquals("id A", transporte.ultima().parametro("sortBy"));
    }

    @Test
    void paginarSinOrdenFalla() {
        ConsultaWfs sinOrden = ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).construir();

        assertThrows(IllegalArgumentException.class, () -> wfs.paginar(sinOrden, 10));
    }

    @Test
    void capasPublicadasIncluyeTodasLasDelEnum() {
        transporte.cuandoParametro("request", "GetCapabilities").fixture("wfs/capabilities.xml");

        List<String> capas = wfs.capasPublicadas();

        for (CapaWfs c : CapaWfs.values()) {
            assertTrue(capas.contains(c.nombre()), c::nombre);
        }
    }

    @Test
    void esquemaIncluyeElCampoDeGeometria() {
        transporte.cuandoParametro("request", "DescribeFeatureType").fixture("wfs/describe_desplazamientos.xml");

        Map<String, String> campos = wfs.esquema(CapaWfs.DESPLAZAMIENTOS);

        assertEquals("GeometryPropertyType", campos.get(CapaWfs.DESPLAZAMIENTOS.campoGeometria()));
        assertEquals("int", campos.get("longitud"));
    }

    @Test
    void descargarGuardaElCuerpoEnElArchivo(@TempDir Path dir) throws Exception {
        transporte.cuandoParametro("outputFormat", "csv").texto("FID,id\n1,2\n");

        Path archivo = wfs.descargar(ConsultaWfs.de(CapaWfs.PARADA_CONSULTABLE).construir(), FormatoWfs.CSV,
                dir.resolve("paradas.csv"));

        assertArrayEquals("FID,id\n1,2\n".getBytes(), Files.readAllBytes(archivo));
    }

    @Test
    void envolventeYVistaSeSerializan() {
        transporte.cuandoParametro("request", "GetFeature").fixture("wfs/paradas.json");

        wfs.obtener(ConsultaWfs.de(CapaWfs.PARADAS_UBICACION)
                .envolvente(new Envolvente(-84.1, 9.9, -84.0, 10.0), ConsultaWfs.WGS84)
                .vista("provincia", 1).vista("canton", 101)
                .construir());

        Map<String, String> q = transporte.ultima().consulta();
        assertEquals("-84.1,9.9,-84.0,10.0,EPSG:4326", q.get("bbox"));
        assertEquals("canton:101;provincia:1;", q.get("VIEWPARAMS"));
    }

    private static byte[] paginaSintetica(int inicio, int tamano) {
        String features = String.join(",", IntStream.range(inicio, inicio + tamano)
                .mapToObj(i -> "{\"type\":\"Feature\",\"id\":\"d." + i
                        + "\",\"geometry\":null,\"properties\":{\"id\":" + i + "}}")
                .toList());
        return ("{\"type\":\"FeatureCollection\",\"features\":[" + features + "]}").getBytes();
    }

    @Test
    void fixtureDeCapacidadesEsXml() {
        assertTrue(Fixtures.texto("wfs/capabilities.xml").startsWith("<?xml"));
    }
}
