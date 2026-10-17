package ctpcliente.integracion.flujos;

import ctpcliente.CtpCliente;
import ctpcliente.core.CapaWfs;
import ctpcliente.core.Caracterizacion;
import ctpcliente.core.ConsultaWfs;
import ctpcliente.core.Filtro;
import ctpcliente.integracion.PruebaIntegracion;
import ctpcliente.model.Division;
import ctpcliente.model.FilaConsulta;
import ctpcliente.util.Textos;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Escenario encadenado por distrito: A (division administrativa) -> B (paradas del distrito) ->
 * WFS (paradas fisicas dentro de la envolvente del distrito).
 */
@PruebaIntegracion
class FlujoUbicacionIT {

    @ParameterizedTest(name = "Distrito {2} {3}")
    @CsvFileSource(resources = "/integracion/distritos.csv", numLinesToSkip = 1)
    void distritoCompleto(String provincia, String canton, String distrito, String nombre, CtpCliente ctp,
                          TestReporter reporte) {
        Bitacora b = new Bitacora("Distrito " + distrito + " " + nombre);

        Division div = b.paso("A", "distrito en el catalogo", () -> ctp.visor().distritos(canton).stream()
                .filter(d -> d.id().equals(distrito)).findFirst().orElseThrow(), d -> d.nombre());
        assertTrue(Textos.equivalentes(nombre, div.nombre()));

        List<FilaConsulta> filas = b.paso("B", "paradas del distrito", () -> ctp.consultas().consultar(
                Caracterizacion.PARADAS_POR_UBICACION,
                Filtro.provincia(provincia), Filtro.canton(canton), Filtro.distrito(distrito)), l -> l.size() + " filas");
        assertFalse(filas.isEmpty());
        assertTrue(filas.stream().allMatch(f -> Textos.equivalentes(f.texto("distrito").orElse(""), div.nombre())),
                "todas las filas de B pertenecen al distrito de A");
        assertTrue(filas.stream().allMatch(f -> f.texto("identificador_parada").orElse("").startsWith("PBA-" + distrito)),
                "el codigo de cada parada contiene el codigo del distrito");
        b.verificacion("nombre y codigo de distrito coherentes", filas.size() + "/" + filas.size());

        Set<Long> idsB = filas.stream().map(f -> f.entero("id").orElseThrow()).collect(Collectors.toCollection(TreeSet::new));
        Set<Long> idsWfs = b.paso("WFS", "paradas fisicas en la envolvente del distrito", () -> ctp.wfs().obtener(
                ConsultaWfs.de(CapaWfs.PARADA_CONSULTABLE).propiedades("id")
                        .envolvente(div.envolvente().orElseThrow(), ConsultaWfs.CRTM05).construir()).elementos().stream()
                .map(e -> e.entero("id").orElseThrow()).collect(Collectors.toCollection(TreeSet::new)), s -> s.size() + " ids");
        Set<Long> faltantes = new TreeSet<>(idsB);
        faltantes.removeAll(idsWfs);
        assertEquals(Set.of(), faltantes, "las paradas de B deben estar dentro de la envolvente del distrito");
        b.verificacion("paradas de B dentro de la envolvente (WFS)", idsB.size() + " de " + idsWfs.size());

        b.publicar(reporte, distrito);
    }
}
