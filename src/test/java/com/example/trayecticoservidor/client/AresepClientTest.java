package com.example.trayecticoservidor.client;

import com.example.trayecticoservidor.model.TarifaAresep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de {@link AresepClient#analizarTarifas(String)}. No usan red: el cliente apunta a
 * localhost y solo se prueba el análisis del JSON.
 */
class AresepClientTest {
    private final AresepClient cliente = new AresepClient(URI.create("http://localhost"), 1);

    /** Fila base válida. Cada prueba la modifica con replace para armar el caso que necesita. */
    private static final String FILA = """
        {"codigoRuta":"I-1","nombreRuta":" DESAMPARADOS-MORAVIA ",
         "codigoRamal":"R0002","nombreRamal":"DESAMPARADOS-MORAVIA",
         "codigoFraccionamiento":"T0001","nombreFraccionamiento":"DESAMPARADOS-MORAVIA",
         "tarifaRegular":"350.00","tarifaAdultoMayor":"0.00","promedioKmViaje":"13.15",
         "fechaVigencia":"2026-09-04T00:00:00.000","provincia":null}
        """;

    /** Envuelve las filas en una respuesta exitosa de ARESEP. */
    private String respuesta(String filas) {
        return "{\"metadata\":{\"success\":true,\"status\":200},\"value\":[" + filas + "]}";
    }

    @Test
    void analizaCodigosDecimalesFechasYValoresNulos() {
        var tarifa = cliente.analizarTarifas(respuesta(FILA)).getFirst();
        assertEquals("I-1", tarifa.codigoRuta());
        assertEquals("DESAMPARADOS-MORAVIA", tarifa.nombreRuta());
        assertEquals(new BigDecimal("350.00"), tarifa.tarifaRegular());
        assertEquals("2026-09-04", tarifa.fechaVigencia().toString());
        assertNull(tarifa.provincia());
    }

    /** Dos filas idénticas: se importa una sola. */
    @Test
    void unaFraccionRepetidaConLosMismosDatosSeImportaUnaVez() {
        List<TarifaAresep> tarifas = cliente.analizarTarifas(respuesta(FILA + "," + FILA));

        assertEquals(1, tarifas.size());
    }

    /**
     * Caso real T7223: misma tarifa y vigencia, solo cambia el promedio de kilómetros.
     * Se importa una vez y se conserva la primera fila.
     */
    @Test
    void unaFraccionRepetidaConOtrosKilometrosSeImportaUnaVez() {
        String repetida = FILA.replace("13.15", "13.25");

        List<TarifaAresep> tarifas = cliente.analizarTarifas(respuesta(FILA + "," + repetida));

        assertEquals(1, tarifas.size());
        assertEquals(new BigDecimal("13.15"), tarifas.getFirst().kilometrosPromedioViaje());
    }

    /** Mismo código con otra tarifa: no se sabe cuál es la correcta y se rechaza todo. */
    @Test
    void rechazaUnaFraccionRepetidaConTarifaDistinta() {
        String repetida = FILA.replace("350.00", "400.00");

        var error = assertThrows(IllegalArgumentException.class,
                () -> cliente.analizarTarifas(respuesta(FILA + "," + repetida)));

        assertTrue(error.getMessage().contains("codigoFraccionamiento=T0001"));
        assertTrue(error.getMessage().contains("duplicado con tarifa o vigencia distinta"));
    }

    /** Mismo código con otra fecha de vigencia: también se rechaza. */
    @Test
    void rechazaUnaFraccionRepetidaConVigenciaDistinta() {
        String repetida = FILA.replace("2026-09-04T00:00:00.000", "2026-10-01T00:00:00.000");

        var error = assertThrows(IllegalArgumentException.class,
                () -> cliente.analizarTarifas(respuesta(FILA + "," + repetida)));

        assertTrue(error.getMessage().contains("duplicado con tarifa o vigencia distinta"));
    }

    @Test
    void rechazaRespuestasVaciasFallidasYMalformadas() {
        for (String json : new String[]{respuesta(""), "{}", "null", "not-json",
                "{\"metadata\":{\"success\":false,\"status\":200},\"value\":[" + FILA + "]}"}) {
            assertThrows(IllegalArgumentException.class, () -> cliente.analizarTarifas(json));
        }
    }

    @Test
    void rechazaMontosNegativosAusentesYConPrecisionExcesiva() {
        for (String valor : new String[]{"-1.00", "350.001", ""}) {
            assertThrows(RuntimeException.class,
                    () -> cliente.analizarTarifas(respuesta(FILA.replace("350.00", valor))));
        }
    }

    @Test
    void erroresDeFilaIncluyenCodigoDeFraccionFallidoYCausaOriginal() {
        for (String filaInvalida : new String[]{
                FILA.replace("T0001", "T0002").replace("350.00", "-1.00"),
                FILA.replace("T0001", "T0002").replace("350.00", "350.001"),
                FILA.replace("T0001", "T0002").replace("350.00", "invalid"),
                FILA.replace("T0001", "T0002").replace("350.00", ""),
                FILA.replace("T0001", "T0002").replace("2026-09-04T00:00:00.000", "invalid"),
                FILA.replace("T0001", "T0002").replace("\"provincia\":null", "\"provincia\":{}")}) {
            var error = assertThrows(IllegalArgumentException.class,
                    () -> cliente.analizarTarifas(respuesta(FILA + "," + filaInvalida)));

            assertTrue(error.getMessage().contains("codigoFraccionamiento=T0002"));
            assertNotNull(error.getCause());
            assertTrue(error.getMessage().contains(error.getCause().getMessage()));
        }
    }

    @Test
    void codigoDeFraccionAusenteSeReportaComoDesconocido() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> cliente.analizarTarifas(respuesta(FILA.replace("T0001", ""))));

        assertTrue(error.getMessage().contains("codigoFraccionamiento=desconocido"));
        assertTrue(error.getMessage().contains("Falta campo ARESEP: codigoFraccionamiento"));
    }
}