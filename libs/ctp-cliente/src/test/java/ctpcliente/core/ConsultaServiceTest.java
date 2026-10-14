package ctpcliente.core;

import ctpcliente.CtpConfig;
import ctpcliente.error.CtpServicioException;
import ctpcliente.model.FilaConsulta;
import ctpcliente.model.ParadaEnRecorrido;
import ctpcliente.model.TipoParada;
import ctpcliente.soporte.TransporteSimulado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultaServiceTest {

    private TransporteSimulado transporte;
    private ConsultaService consultas;

    @BeforeEach
    void preparar() {
        transporte = new TransporteSimulado()
                .cuandoParametro("caracterizacion", "paradas_desplazamiento_ruta_ramal").fixture("consulta/paradas_56_244.json")
                .cuandoParametro("caracterizacion", "rutas_parada").fixture("consulta/rutas_parada.json");
        consultas = new ConsultaService(transporte, CtpConfig.BASE_VISOR);
    }

    @Test
    void enviaElFormularioConFiltrosIndexados() {
        consultas.paradasDeRamal(56, 244);

        TransporteSimulado.Peticion p = transporte.ultima();
        assertAll(
                () -> assertEquals("POST", p.metodo()),
                () -> assertEquals("queryTablaDatos", p.parametro("action")),
                () -> assertEquals("ruta", p.parametro("filtros[0][filtro]")),
                () -> assertEquals("56", p.parametro("filtros[0][valor]")),
                () -> assertEquals("ramal", p.parametro("filtros[1][filtro]")),
                () -> assertEquals("244", p.parametro("filtros[1][valor]")));
    }

    @Test
    void paradasQuedanOrdenadasPorSentidoYOrden() {
        List<ParadaEnRecorrido> paradas = consultas.paradasDeRamal(56, 244);

        assertTrue(paradas.size() > 10);
        ParadaEnRecorrido primera = paradas.getFirst();
        assertAll(
                () -> assertEquals(1, primera.orden()),
                () -> assertEquals(TipoParada.INICIAL, primera.tipo()),
                () -> assertEquals("0002-A", primera.codigoRamal()),
                () -> assertTrue(primera.ubicacion().isPresent()));
        for (int i = 1; i < paradas.size(); i++) {
            assertTrue(ParadaEnRecorrido.POR_RAMAL_SENTIDO_ORDEN.compare(paradas.get(i - 1), paradas.get(i)) <= 0);
        }
    }

    @Test
    void rutasPorParadaDevuelveCodigos() {
        assertEquals(List.of("0002"), consultas.rutasPorParada("PBA-10104-054"));
        assertEquals("PBA-10104-054", transporte.ultima().parametro("filtros[0][valor]"));
    }

    @Test
    void successFalseSinMensajeEsListaVacia() {
        transporte.cuandoParametro("caracterizacion", "fraccionamientos_de_una_ruta").fixture("consulta/sin_resultados.json");

        assertEquals(List.of(), consultas.consultar(Caracterizacion.PARADAS_DE_FRACCIONAMIENTOS, Filtro.ruta(56)));
    }

    @Test
    void successFalseConMensajeLanzaError() {
        transporte.cuandoParametro("caracterizacion", "paradas_ubicacion").fixture("consulta/falta_filtro.json");

        CtpServicioException e = assertThrows(CtpServicioException.class, () -> consultas.consultar(
                Caracterizacion.PARADAS_POR_UBICACION, Filtro.provincia("1"), Filtro.canton("101")));
        assertTrue(e.getMessage().contains("canton"));
    }

    @Test
    void faltaFiltroObligatorioFallaSinLlamarAlServidor() {
        assertThrows(IllegalArgumentException.class,
                () -> consultas.consultar(Caracterizacion.PARADAS_POR_UBICACION, Filtro.provincia("1")));
        assertTrue(transporte.peticiones().isEmpty());
    }

    @Test
    void filtroNoPermitidoFalla() {
        assertThrows(IllegalArgumentException.class,
                () -> consultas.consultar(Caracterizacion.RUTAS_POR_PARADA, Filtro.ruta(56)));
    }

    @Test
    void filtroRepetidoFalla() {
        assertThrows(IllegalArgumentException.class,
                () -> consultas.consultar(Caracterizacion.PARADAS_DE_RECORRIDO, Filtro.ruta(56), Filtro.ruta(57)));
    }

    @Test
    void carreteraPorIdNoSirveDondeSeEsperaNumero() {
        assertThrows(IllegalArgumentException.class,
                () -> consultas.consultar(Caracterizacion.RECORRIDOS_POR_CARRETERA, Filtro.carreteraPorId("331")));
        assertThrows(IllegalArgumentException.class,
                () -> consultas.consultar(Caracterizacion.RUTAS_POR_CARRETERA, Filtro.carreteraPorNumero(1)));
    }

    @ParameterizedTest
    @EnumSource(Caracterizacion.class)
    void todasLasCaracterizacionesTienenObligatoriosDentroDePermitidos(Caracterizacion c) {
        assertTrue(c.permitidos().containsAll(c.obligatorios()));
        assertTrue(c.clave().matches("[a-z_]+"));
    }

    @Test
    void filaConsultaLeeGeometria() {
        FilaConsulta f = consultas.consultar(Caracterizacion.RUTAS_POR_PARADA, Filtro.parada("PBA-10104-054")).getFirst();

        assertTrue(f.geometria().isPresent());
        assertEquals("0002", f.texto("codigo_ruta").orElseThrow());
    }
}
