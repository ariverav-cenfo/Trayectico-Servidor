package ctpcliente.integracion;

import ctpcliente.CtpCliente;
import ctpcliente.model.Desplazamiento;
import ctpcliente.model.Division;
import ctpcliente.model.ParadaCatalogo;
import ctpcliente.model.Ramal;
import ctpcliente.model.Ruta;
import ctpcliente.model.RutaNacional;
import ctpcliente.util.Crtm05;
import ctpcliente.util.Identificadores;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PruebaIntegracion
class VisorServiceIT {

    @Test
    void rutasTieneCientosDeRutasConIdUnico(CtpCliente ctp) {
        List<Ruta> rutas = ctp.visor().rutas();

        assertTrue(rutas.size() > 400, () -> "rutas: " + rutas.size());
        assertEquals(rutas.size(), rutas.stream().map(Ruta::id).distinct().count());
        assertTrue(rutas.stream().filter(r -> Identificadores.esRuta(r.codigo())).count() > rutas.size() * 0.95);
    }

    @Test
    void ramalesYDesplazamientosDeLaRuta0002(CtpCliente ctp) {
        int idRuta = ctp.ids().ruta("0002").id();
        List<Ramal> ramales = ctp.visor().ramales(idRuta);

        assertTrue(ramales.stream().allMatch(r -> r.codigo().startsWith("0002-")));
        Ramal a = ramales.stream().filter(r -> r.codigo().equals("0002-A")).findFirst().orElseThrow();
        List<Desplazamiento> desplazamientos = ctp.visor().desplazamientos(idRuta, a.id());
        assertTrue(desplazamientos.stream().allMatch(d -> d.codigo().startsWith("0002-A-")));
        assertTrue(desplazamientos.stream().allMatch(d -> d.envolvente().isPresent()));
    }

    @Test
    void paradasDelCatalogoEstanEnCostaRica(CtpCliente ctp) {
        List<ParadaCatalogo> paradas = ctp.visor().paradas();

        assertTrue(paradas.size() > 30_000);
        long fuera = paradas.stream()
                .flatMap(p -> p.ubicacion().stream())
                .map(Crtm05::aWgs84)
                .filter(c -> !Limites.enCostaRica(c))
                .count();
        assertTrue(fuera < paradas.size() / 1000, () -> "paradas fuera de Costa Rica: " + fuera);
    }

    @Test
    void lasDosListasDeRutasNacionalesCoincidenEnNumero(CtpCliente ctp) {
        List<RutaNacional> porId = ctp.visor().rutasNacionales();
        List<RutaNacional> porNumero = ctp.visor().rutasNacionalesPorNumero();

        assertEquals(porId.size(), porNumero.size());
        assertTrue(porNumero.stream().allMatch(r -> r.id().equals(Integer.toString(r.numero()))));
        assertEquals("331", ctp.ids().carretera(1).id());
    }

    @Test
    void divisionAdministrativaCompleta(CtpCliente ctp) {
        assertEquals(7, ctp.visor().provincias().size());
        List<Division> cantones = ctp.visor().cantones("1");
        assertEquals(20, cantones.size());
        assertTrue(cantones.stream().allMatch(c -> c.id().startsWith("1") && c.id().length() == 3));
        assertTrue(ctp.visor().distritos("101").stream().allMatch(d -> d.id().startsWith("101") && d.id().length() == 5));
    }
}
