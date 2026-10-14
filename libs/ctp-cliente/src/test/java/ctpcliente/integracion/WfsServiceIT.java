package ctpcliente.integracion;

import ctpcliente.CtpCliente;
import ctpcliente.core.CapaWfs;
import ctpcliente.core.ConsultaWfs;
import ctpcliente.core.FormatoWfs;
import ctpcliente.error.CtpOwsException;
import ctpcliente.model.ColeccionElementos;
import ctpcliente.model.Coordenada;
import ctpcliente.model.Elemento;
import ctpcliente.model.Envolvente;
import ctpcliente.util.Cql;
import ctpcliente.util.Crtm05;
import ctpcliente.util.Distancia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PruebaIntegracion
class WfsServiceIT {

    private static ConsultaWfs.Constructor consultaMinima(CapaWfs capa) {
        ConsultaWfs.Constructor c = ConsultaWfs.de(capa);
        if (capa == CapaWfs.PARADAS_PERSONALIZADA) {
            c.vista("ruta", 56).vista("conparadas", 1);
        }
        return c;
    }

    @Test
    void todasLasCapasDelEnumEstanPublicadas(CtpCliente ctp) {
        List<String> publicadas = ctp.wfs().capasPublicadas();

        for (CapaWfs c : CapaWfs.values()) {
            assertTrue(publicadas.contains(c.nombre()), c::nombre);
        }
    }

    @ParameterizedTest
    @EnumSource(CapaWfs.class)
    void cadaCapaTieneElementos(CapaWfs capa, CtpCliente ctp) {
        assertTrue(ctp.wfs().contar(consultaMinima(capa).construir()) > 0, capa::name);
    }

    @ParameterizedTest
    @EnumSource(CapaWfs.class)
    void cadaCapaDevuelveGeometriaEnCostaRica(CapaWfs capa, CtpCliente ctp) {
        ColeccionElementos c = ctp.wfs().obtener(consultaMinima(capa).maximo(2).construir());

        assertFalse(c.estaVacia(), capa::name);
        Elemento e = c.elementos().getFirst();
        assertTrue(Limites.enCostaRica(e.geometria().vertices().getFirst()), capa::name);
    }

    @ParameterizedTest
    @EnumSource(CapaWfs.class)
    void cadaCapaPublicaSuCampoDeGeometria(CapaWfs capa, CtpCliente ctp) {
        Map<String, String> esquema = ctp.wfs().esquema(capa);

        assertEquals("GeometryPropertyType", esquema.get(capa.campoGeometria()), capa::name);
        capa.campoId().ifPresent(id -> assertTrue(esquema.containsKey(id), capa::name));
    }

    @Test
    void filtroCqlYConteoCoinciden(CtpCliente ctp) {
        ConsultaWfs q = ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS)
                .filtro(Cql.empiezaCon("identificador_desplazamiento", "0002-"))
                .propiedades("identificador_desplazamiento")
                .construir();

        assertEquals(ctp.wfs().contar(q), ctp.wfs().obtener(q).tamano());
    }

    @Test
    void recorrerYObtenerDevuelvenLoMismo(CtpCliente ctp) {
        ConsultaWfs q = ConsultaWfs.de(CapaWfs.PARADAS_DESPLAZAMIENTO_RUTA_RAMAL)
                .filtro(Cql.igual("identificador_ramal", "0002-A"))
                .construir();
        List<Long> enFlujo = new ArrayList<>();

        long n = ctp.wfs().recorrer(q, e -> enFlujo.add(e.entero("id").orElseThrow()));

        assertEquals(ctp.wfs().obtener(q).tamano(), n);
        assertEquals(n, enFlujo.size());
    }

    @Test
    void paginarRecorreTodoSinRepetir(CtpCliente ctp) {
        ConsultaWfs q = ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS)
                .filtro(Cql.empiezaCon("identificador_desplazamiento", "04"))
                .propiedades("id")
                .ordenarPor("id", false)
                .construir();

        List<Long> ids = ctp.wfs().paginar(q, 40).map(e -> e.entero("id").orElseThrow()).toList();

        assertEquals(ctp.wfs().contar(q), ids.size());
        assertEquals(ids.size(), ids.stream().distinct().count());
    }

    @Test
    void envolventeEnWgs84YCqlBboxEnCrtm05SonEquivalentes(CtpCliente ctp) {
        Coordenada centro = Coordenada.lonLat(-84.0762, 9.9327);
        Envolvente caja = Distancia.alrededor(centro, 250);
        long porBbox = ctp.wfs().contar(ConsultaWfs.de(CapaWfs.PARADA_CONSULTABLE)
                .envolvente(caja, ConsultaWfs.WGS84).construir());
        long porCql = ctp.wfs().contar(ConsultaWfs.de(CapaWfs.PARADA_CONSULTABLE)
                .filtro(Cql.bbox(CapaWfs.PARADA_CONSULTABLE.campoGeometria(), caja, ConsultaWfs.WGS84)).construir());

        assertTrue(porBbox > 0);
        assertEquals(porBbox, porCql);
        Coordenada metros = Crtm05.desdeWgs84(centro);
        assertTrue(metros.x() > 490_000 && metros.x() < 493_000);
    }

    @Test
    void geometriaEnCrtm05CuandoSePide(CtpCliente ctp) {
        Elemento e = ctp.wfs().obtener(ConsultaWfs.de(CapaWfs.PARADA_CONSULTABLE)
                .srs(ConsultaWfs.CRTM05).maximo(1).construir()).elementos().getFirst();

        assertTrue(Limites.enCostaRica(Crtm05.aWgs84(e.geometria().vertices().getFirst())));
    }

    @Test
    void descargasEnTodosLosFormatos(@TempDir Path dir, CtpCliente ctp) throws Exception {
        ConsultaWfs q = ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS)
                .filtro(Cql.empiezaCon("identificador_desplazamiento", "0002-"))
                .construir();
        for (FormatoWfs f : FormatoWfs.values()) {
            Path archivo = ctp.wfs().descargar(q, f, dir.resolve("ruta0002." + f.extension()));
            assertTrue(Files.size(archivo) > 500, f::name);
        }
        assertTrue(Files.readString(dir.resolve("ruta0002.csv")).startsWith("FID,"));
    }

    @Test
    void elServidorNoEscapaComodines(CtpCliente ctp) {
        ConsultaWfs.Constructor base = ConsultaWfs.de(CapaWfs.PARADA_CONSULTABLE);
        long conBarra = ctp.wfs().contar(base.filtro("descripcion_corta ILIKE 'MAXI\\_PAL%'").construir());
        long sinBarra = ctp.wfs().contar(base.filtro("descripcion_corta ILIKE 'MAXI_PAL%'").construir());
        long conEspacio = ctp.wfs().contar(base.filtro(Cql.empiezaCon("descripcion_corta", "MAXI PAL")).construir());

        assertEquals(sinBarra, conBarra, "la barra invertida no escapa el comodin");
        assertTrue(conEspacio > 0 && conEspacio <= sinBarra);
    }

    @Test
    void ordenDescendenteDejaLosNulosPrimero(CtpCliente ctp) {
        ConsultaWfs.Constructor base = ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).propiedades("longitud")
                .ordenarPor("longitud", true).maximo(1);

        Elemento sinFiltro = ctp.wfs().obtener(base.construir()).elementos().getFirst();
        Elemento conFiltro = ctp.wfs().obtener(base.filtro(Cql.mayorQue("longitud", 0)).construir()).elementos().getFirst();

        assertTrue(sinFiltro.entero("longitud").isEmpty(), "PostgreSQL ordena los nulos primero en DESC");
        assertTrue(conFiltro.entero("longitud").orElseThrow() > 100_000);
    }

    @Test
    void vistaConValorInvalidoEsExcepcionOws(CtpCliente ctp) {
        assertThrows(CtpOwsException.class, () -> ctp.wfs().contar(ConsultaWfs.de(CapaWfs.DESPLAZAMIENTO_USA_PARADA)
                .vista("parada", "-5").construir()));
    }
}
