package ctpcliente.integracion.flujos;

import ctpcliente.CtpCliente;
import ctpcliente.core.CapaWfs;
import ctpcliente.core.Caracterizacion;
import ctpcliente.core.ConsultaWfs;
import ctpcliente.core.Filtro;
import ctpcliente.error.CtpNoEncontradoException;
import ctpcliente.integracion.PruebaIntegracion;
import ctpcliente.model.Elemento;
import ctpcliente.model.RutaNacional;
import ctpcliente.util.Cql;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Escenario encadenado por ruta nacional: A (id de la carretera) -> B (rutas, recorridos) ->
 * WFS (tramos por CQL y por VIEWPARAMS) -> A (cada ruta encontrada existe en el catalogo).
 */
@PruebaIntegracion
class FlujoCarreteraIT {

    @ParameterizedTest(name = "Ruta Nacional {0} ({1})")
    @CsvFileSource(resources = "/integracion/carreteras.csv", numLinesToSkip = 1)
    void carreteraCompleta(int numero, String nombre, int minimoRutas, CtpCliente ctp, TestReporter reporte) {
        Bitacora b = new Bitacora("Ruta Nacional " + numero + " " + nombre);

        RutaNacional rn = b.paso("A", "id interno de la carretera", () -> ctp.ids().carretera(numero),
                r -> "id " + r.id());

        Set<String> rutasB = b.paso("B", "rutas por carretera (id interno)", () -> ctp.consultas()
                .consultar(Caracterizacion.RUTAS_POR_CARRETERA, Filtro.carreteraPorId(rn.id())).stream()
                .map(f -> f.texto("codigo_ruta").orElseThrow())
                .collect(Collectors.toCollection(TreeSet::new)), s -> s.size() + " rutas");
        assertTrue(rutasB.size() >= minimoRutas, () -> "rutas: " + rutasB.size());

        Set<String> recorridosB = b.paso("B", "recorridos por carretera (numero)", () -> ctp.consultas()
                .consultar(Caracterizacion.RECORRIDOS_POR_CARRETERA, Filtro.carreteraPorNumero(numero)).stream()
                .map(f -> f.texto("identificador_desplazamiento").orElseThrow())
                .collect(Collectors.toCollection(TreeSet::new)), s -> s.size() + " recorridos");

        List<Elemento> tramos = b.paso("WFS", "tramos por CQL rutanacional", () -> ctp.wfs().obtener(
                ConsultaWfs.de(CapaWfs.DESPLAZAMIENTO_TRAMO_CARRETERA)
                        .propiedades("codigo_ruta", "identificador_desplazamiento")
                        .filtro(Cql.igual("rutanacional", numero)).construir()).elementos(), l -> l.size() + " tramos");
        Set<String> rutasWfs = tramos.stream().map(e -> e.texto("codigo_ruta").orElseThrow())
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> recorridosWfs = tramos.stream().map(e -> e.texto("identificador_desplazamiento").orElseThrow())
                .collect(Collectors.toCollection(TreeSet::new));

        assertEquals(rutasB, rutasWfs, "B y WFS deben coincidir en las rutas");
        assertEquals(recorridosB, recorridosWfs, "B y WFS deben coincidir en los recorridos");
        b.verificacion("B y WFS coinciden en rutas y recorridos", rutasB.size() + " / " + recorridosB.size());

        long porVista = b.paso("WFS", "conteo por VIEWPARAMS ruar", () -> ctp.wfs().contar(
                ConsultaWfs.de(CapaWfs.DESPLAZAMIENTO_TRAMO_CARRETERA).vista("ruar", numero).construir()),
                n -> n + " elementos");
        assertEquals(recorridosB.size(), porVista);

        List<String> desconocidas = b.paso("A", "cada ruta existe en el catalogo", () -> rutasB.stream()
                .filter(c -> {
                    try {
                        ctp.ids().ruta(c);
                        return false;
                    } catch (CtpNoEncontradoException e) {
                        return true;
                    }
                }).toList(), l -> l.isEmpty() ? "todas" : "faltan " + l);
        assertEquals(List.of(), desconocidas);

        b.publicar(reporte, "RN" + numero);
    }
}
