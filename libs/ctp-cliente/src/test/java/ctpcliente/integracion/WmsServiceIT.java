package ctpcliente.integracion;

import ctpcliente.CtpCliente;
import ctpcliente.core.CapaWfs;
import ctpcliente.core.ConsultaWfs;
import ctpcliente.core.ConsultaWms;
import ctpcliente.core.WmsService;
import ctpcliente.error.CtpOwsException;
import ctpcliente.model.ColeccionElementos;
import ctpcliente.model.Envolvente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PruebaIntegracion
class WmsServiceIT {

    private static final Envolvente CENTRO_SJ = new Envolvente(-84.09, 9.925, -84.065, 9.945);

    @ParameterizedTest
    @EnumSource(value = CapaWfs.class, names = {"DESPLAZAMIENTOS", "PARADA_CONSULTABLE", "TRAMO_CARRETERA_NACIONAL",
            "PARADAS_TERMINALES_UBICACION"})
    void getMapDevuelvePng(CapaWfs capa, CtpCliente ctp) {
        byte[] png = ctp.wms().mapa(ConsultaWms.de(capa).envolvente(CENTRO_SJ, ConsultaWfs.WGS84).tamano(256, 256).construir());

        assertTrue(WmsService.esPng(png), capa::name);
        assertTrue(png.length > 200);
    }

    @Test
    void getMapConVistaResaltaUnaRuta(CtpCliente ctp) {
        byte[] png = ctp.wms().mapa(ConsultaWms.de("personalizada_desplazamientos_de_ruta_ramal")
                .envolvente(CENTRO_SJ, ConsultaWfs.WGS84).vista("ruta", 56).construir());

        assertTrue(WmsService.esPng(png));
    }

    @Test
    void getFeatureInfoEncuentraParadas(CtpCliente ctp) {
        ColeccionElementos c = ctp.wms().elementosEnPixel(ConsultaWms.de(CapaWfs.PARADAS_X_DESPLAZAMIENTO)
                .envolvente(CENTRO_SJ, ConsultaWfs.WGS84).tamano(900, 700).construir(), 450, 350, 5);

        assertFalse(c.estaVacia());
        assertTrue(c.elementos().stream().allMatch(e -> Limites.enCostaRica(e.geometria().vertices().getFirst())));
    }

    @Test
    void capaInexistenteEsExcepcionOws(CtpCliente ctp) {
        assertThrows(CtpOwsException.class, () -> ctp.wms().mapa(ConsultaWms.de("no_existe")
                .envolvente(CENTRO_SJ, ConsultaWfs.WGS84).construir()));
    }
}
