package ctpcliente.integracion;

import ctpcliente.CtpCliente;
import ctpcliente.core.Caracterizacion;
import ctpcliente.core.Filtro;
import ctpcliente.model.FilaConsulta;
import ctpcliente.model.ParadaEnRecorrido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PruebaIntegracion
class ConsultaServiceIT {

    private static final int RUTA_0002 = 56;
    private static final int RAMAL_0002_A = 244;
    private static final int RUTA_0134 = 186;
    private static final String PARADA = "PBA-10104-054";

    static Stream<Arguments> todasLasCaracterizaciones() {
        return Stream.of(
                Arguments.of(Caracterizacion.PARADAS_DE_RECORRIDO,
                        new Filtro[] {Filtro.ruta(RUTA_0002), Filtro.ramal(RAMAL_0002_A)}),
                Arguments.of(Caracterizacion.RECORRIDOS_DE_RUTA, new Filtro[] {Filtro.ruta(RUTA_0134)}),
                Arguments.of(Caracterizacion.FRACCIONAMIENTOS_DE_RUTA, new Filtro[] {Filtro.ruta(RUTA_0134)}),
                Arguments.of(Caracterizacion.PARADAS_DE_FRACCIONAMIENTOS, new Filtro[] {Filtro.ruta(RUTA_0134)}),
                Arguments.of(Caracterizacion.RUTAS_POR_PARADA, new Filtro[] {Filtro.parada(PARADA)}),
                Arguments.of(Caracterizacion.RAMALES_POR_PARADA, new Filtro[] {Filtro.parada(PARADA)}),
                Arguments.of(Caracterizacion.RECORRIDOS_POR_PARADA, new Filtro[] {Filtro.parada(PARADA)}),
                Arguments.of(Caracterizacion.PARADAS_POR_UBICACION,
                        new Filtro[] {Filtro.provincia("1"), Filtro.canton("101"), Filtro.distrito("10101")}),
                Arguments.of(Caracterizacion.TERMINALES_POR_UBICACION,
                        new Filtro[] {Filtro.provincia("1"), Filtro.canton("101")}),
                Arguments.of(Caracterizacion.RUTAS_POR_CARRETERA, new Filtro[] {Filtro.carreteraPorId("331")}),
                Arguments.of(Caracterizacion.RAMALES_POR_CARRETERA, new Filtro[] {Filtro.carreteraPorId("331")}),
                Arguments.of(Caracterizacion.RECORRIDOS_POR_CARRETERA, new Filtro[] {Filtro.carreteraPorNumero(27)}));
    }

    @Test
    void elProveedorCubreTodasLasCaracterizaciones() {
        Set<Caracterizacion> cubiertas = todasLasCaracterizaciones()
                .map(a -> (Caracterizacion) a.get()[0])
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Caracterizacion.class)));

        assertEquals(EnumSet.allOf(Caracterizacion.class), cubiertas);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("todasLasCaracterizaciones")
    void cadaCaracterizacionDevuelveFilas(Caracterizacion c, Filtro[] filtros, CtpCliente ctp) {
        List<FilaConsulta> filas = ctp.consultas().consultar(c, filtros);

        assertFalse(filas.isEmpty(), c::name);
        assertFalse(filas.getFirst().atributos().isEmpty());
    }

    @Test
    void paradasDeUnRecorridoSonUnaSecuenciaDesdeUno(CtpCliente ctp) {
        int idDesp = ctp.ids().desplazamiento("0002-A-2").id();
        List<ParadaEnRecorrido> paradas = ctp.consultas().paradasDeRecorrido(RUTA_0002, RAMAL_0002_A, idDesp);

        List<Integer> ordenes = paradas.stream().map(ParadaEnRecorrido::orden).toList();
        assertEquals(IntStream.rangeClosed(1, paradas.size()).boxed().toList(), ordenes);
        assertTrue(paradas.stream().allMatch(p -> p.ubicacion().isPresent()));
    }

    @Test
    void faltaUnFiltroSeDetectaAntesDeLlamar(CtpCliente ctp) {
        assertThrows(IllegalArgumentException.class,
                () -> ctp.consultas().consultar(Caracterizacion.PARADAS_POR_UBICACION, Filtro.provincia("3")));
    }

    @Test
    void rutaSinFraccionamientosDevuelveListaVacia(CtpCliente ctp) {
        assertEquals(List.of(), ctp.consultas().consultar(Caracterizacion.FRACCIONAMIENTOS_DE_RUTA, Filtro.ruta(RUTA_0002)));
    }
}
