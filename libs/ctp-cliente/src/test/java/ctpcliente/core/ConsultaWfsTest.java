package ctpcliente.core;

import ctpcliente.http.Parametros;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultaWfsTest {

    @Test
    void valoresPorDefecto() {
        Parametros p = ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).construir().parametros();

        assertEquals("CTP_PUBLICO:desplazamientos", p.valor("typeName"));
        assertEquals("EPSG:4326", p.valor("srsName"));
        assertNull(p.valor("CQL_FILTER"));
        assertNull(p.valor("maxFeatures"));
    }

    @Test
    void conGeometriaAgregaElCampoSoloSiHayPropiedades() {
        Parametros sin = ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).conGeometria().construir().parametros();
        Parametros con = ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).propiedades("id").conGeometria().construir().parametros();

        assertNull(sin.valor("propertyName"));
        assertEquals("id,geometria_crtm05", con.valor("propertyName"));
    }

    @Test
    void ordenDescendente() {
        assertEquals("longitud D", ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).ordenarPor("longitud", true)
                .construir().parametros().valor("sortBy"));
    }

    @Test
    void paginaEsUnaCopia() {
        ConsultaWfs base = ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).filtro("estado = 'activo'").construir();
        ConsultaWfs pagina = base.pagina(100, 50);

        assertEquals("100", pagina.parametros().valor("startIndex"));
        assertEquals("estado = 'activo'", pagina.parametros().valor("CQL_FILTER"));
        assertTrue(base.inicio().isEmpty());
    }

    @Test
    void rechazaParametrosDeVistaNoAdmitidos() {
        assertThrows(IllegalArgumentException.class,
                () -> ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).vista("ruta", 56));
        assertThrows(IllegalArgumentException.class,
                () -> ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS_DE_RUTA).vista("ruta", "56;ramal:1"));
    }

    @Test
    void rechazaCamposConCaracteresPeligrosos() {
        assertThrows(IllegalArgumentException.class,
                () -> ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).propiedades("id,descripcion"));
        assertThrows(IllegalArgumentException.class,
                () -> ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).ordenarPor("id; drop", false));
        assertThrows(IllegalArgumentException.class, () -> ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).maximo(0));
        assertThrows(IllegalArgumentException.class, () -> ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS).inicio(-1));
    }

    @ParameterizedTest
    @EnumSource(CapaWfs.class)
    void todasLasCapasTienenGeometriaYNombreCalificado(CapaWfs capa) {
        assertTrue(capa.nombreCalificado().startsWith("CTP_PUBLICO:"));
        assertTrue(capa.campoGeometria().startsWith("geometria"));
    }
}
