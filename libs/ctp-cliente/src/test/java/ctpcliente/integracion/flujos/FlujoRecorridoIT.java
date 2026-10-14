package ctpcliente.integracion.flujos;

import ctpcliente.CtpCliente;
import ctpcliente.core.CapaWfs;
import ctpcliente.core.ConsultaWfs;
import ctpcliente.core.ConsultaWms;
import ctpcliente.core.WmsService;
import ctpcliente.integracion.PruebaIntegracion;
import ctpcliente.model.Desplazamiento;
import ctpcliente.model.Elemento;
import ctpcliente.model.Envolvente;
import ctpcliente.model.Geometria;
import ctpcliente.model.ParadaEnRecorrido;
import ctpcliente.model.Ramal;
import ctpcliente.model.Ruta;
import ctpcliente.model.TipoParada;
import ctpcliente.util.Cql;
import ctpcliente.util.Crtm05;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Escenario encadenado por recorrido: A (ids) -> B (paradas en orden) -> WFS (trazado y paradas del
 * ramal) -> B (rutas por parada) -> WMS (mapa). Cada fila de {@code recorridos.csv} es un escenario.
 */
@PruebaIntegracion
class FlujoRecorridoIT {

    private static final double TOLERANCIA_METROS = 300;

    @ParameterizedTest(name = "{0} {1}")
    @CsvFileSource(resources = "/integracion/recorridos.csv", numLinesToSkip = 1, nullValues = "-")
    void recorridoCompleto(String codigo, String descripcion, String huecosConocidos, CtpCliente ctp, TestReporter reporte) {
        Bitacora b = new Bitacora(codigo + " " + descripcion);

        Ruta ruta = b.paso("A", "resolver codigo de ruta", () -> ctp.ids().ruta(codigo.substring(0, 4)),
                r -> "id " + r.id());
        Ramal ramal = b.paso("A", "resolver ramal", () -> ctp.ids().ramal(codigo.substring(0, 6)),
                r -> "id " + r.id() + " " + r.descripcion());
        Desplazamiento desp = b.paso("A", "resolver desplazamiento", () -> ctp.ids().desplazamiento(codigo),
                d -> "id " + d.id() + " sentido " + d.sentido());

        List<ParadaEnRecorrido> paradas = b.paso("B", "paradas del recorrido en orden",
                () -> ctp.consultas().paradasDeRecorrido(ruta.id(), ramal.id(), desp.id()), l -> l.size() + " paradas");
        verificarSecuencia(codigo, paradas, huecosConocidos, b);

        Elemento trazado = b.paso("WFS", "trazado por codigo (CRTM05)", () -> unico(ctp.wfs().obtener(
                ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).srs(ConsultaWfs.CRTM05)
                        .filtro(Cql.igual("identificador_desplazamiento", codigo)).construir()).elementos()),
                e -> e.geometria().vertices().size() + " vertices, " + e.entero("longitud").orElse(0L) + " m");
        assertTrue(trazado.geometria() instanceof Geometria.Linea || trazado.geometria() instanceof Geometria.MultiLinea);
        verificarParadasSobreElTrazado(paradas, trazado.geometria().envolvente(), b);

        Elemento porIds = b.paso("WFS", "trazado por ids internos (VIEWPARAMS)", () -> unico(ctp.wfs().obtener(
                ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS_PERSONALIZADA)
                        .vista("ruta", ruta.id()).vista("ramal", ramal.id()).vista("desplazamiento", desp.id())
                        .propiedades("identificador_desplazamiento").construir()).elementos()),
                e -> e.texto("identificador_desplazamiento").orElse("?"));
        assertEquals(codigo, porIds.texto("identificador_desplazamiento").orElseThrow());

        Set<Long> idsB = b.paso("B", "paradas de todo el ramal",
                () -> ctp.consultas().paradasDeRamal(ruta.id(), ramal.id()).stream().map(ParadaEnRecorrido::id)
                        .collect(Collectors.toCollection(TreeSet::new)), s -> s.size() + " ids");
        Set<Long> idsWfs = b.paso("WFS", "paradas del ramal por CQL", () -> ctp.wfs().obtener(
                ConsultaWfs.de(CapaWfs.PARADAS_DESPLAZAMIENTO_RUTA_RAMAL).propiedades("id")
                        .filtro(Cql.igual("identificador_ramal", ramal.codigo())).construir()).elementos().stream()
                .map(e -> e.entero("id").orElseThrow()).collect(Collectors.toCollection(TreeSet::new)), s -> s.size() + " ids");
        assertEquals(idsB, idsWfs, "B y WFS deben listar las mismas paradas del ramal");
        b.verificacion("B y WFS coinciden en paradas del ramal", "iguales");

        String primera = paradas.getFirst().codigo();
        List<String> rutasDeLaParada = b.paso("B", "rutas que pasan por la primera parada",
                () -> ctp.consultas().rutasPorParada(primera), l -> primera + " -> " + l);
        assertTrue(rutasDeLaParada.contains(ruta.codigo()));

        Envolvente cajaWgs = Crtm05.aWgs84(trazado.geometria().envolvente());
        byte[] png = b.paso("WMS", "mapa de la ruta resaltada", () -> ctp.wms().mapa(
                ConsultaWms.de("personalizada_desplazamientos_de_ruta_ramal").envolvente(cajaWgs, ConsultaWfs.WGS84)
                        .vista("ruta", ruta.id()).tamano(400, 300).construir()), x -> x.length + " bytes PNG");
        assertTrue(WmsService.esPng(png));

        b.publicar(reporte, codigo);
    }

    private static void verificarSecuencia(String codigo, List<ParadaEnRecorrido> paradas, String huecosConocidos,
                                           Bitacora b) {
        assertFalse(paradas.isEmpty(), codigo);
        List<Integer> ordenes = paradas.stream().map(ParadaEnRecorrido::orden).toList();
        Set<Integer> presentes = Set.copyOf(ordenes);
        Set<Integer> huecos = IntStream.rangeClosed(1, ordenes.getLast()).filter(i -> !presentes.contains(i)).boxed()
                .collect(Collectors.toCollection(TreeSet::new));
        Set<Integer> permitidos = huecosConocidos == null ? Set.of()
                : Arrays.stream(huecosConocidos.split("\\|")).map(String::strip).map(Integer::valueOf)
                        .collect(Collectors.toSet());
        assertAll(
                () -> assertEquals(1, ordenes.getFirst(), "la primera parada tiene orden 1"),
                () -> assertEquals(ordenes.size(), presentes.size(), "ordenes sin repetir"),
                () -> assertTrue(permitidos.containsAll(huecos), () -> "huecos nuevos en el orden: " + huecos),
                () -> assertEquals(TipoParada.INICIAL, paradas.getFirst().tipo()),
                () -> assertEquals(TipoParada.FINAL, paradas.getLast().tipo()),
                () -> assertEquals(1, paradas.stream().map(ParadaEnRecorrido::sentido).distinct().count()));
        b.verificacion("secuencia de orden valida", huecos.isEmpty() ? "1.." + ordenes.getLast() : "huecos conocidos " + huecos);
    }

    private static void verificarParadasSobreElTrazado(List<ParadaEnRecorrido> paradas, Envolvente caja, Bitacora b) {
        Envolvente ampliada = new Envolvente(caja.minX() - TOLERANCIA_METROS, caja.minY() - TOLERANCIA_METROS,
                caja.maxX() + TOLERANCIA_METROS, caja.maxY() + TOLERANCIA_METROS);
        List<String> fuera = paradas.stream()
                .filter(p -> p.ubicacion().map(u -> !ampliada.contiene(u)).orElse(true))
                .map(ParadaEnRecorrido::codigo)
                .toList();
        assertEquals(List.of(), fuera, "paradas a mas de " + TOLERANCIA_METROS + " m del trazado");
        b.verificacion("paradas dentro del trazado +" + (int) TOLERANCIA_METROS + " m", paradas.size() + "/" + paradas.size());
    }

    private static Elemento unico(List<Elemento> elementos) {
        assertEquals(1, elementos.size(), "se esperaba un elemento");
        return elementos.getFirst();
    }
}
