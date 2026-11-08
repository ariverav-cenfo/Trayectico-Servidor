package com.example.trayecticoservidor.client;

import com.example.trayecticoservidor.model.TarifaAresep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de {@link AresepClient#analizarTarifas(String)} sin red: cada prueba arma el JSON
 * que respondería ARESEP y revisa cómo se convierte o por qué se rechaza.
 */
class AresepClientTest {
    // El cliente no descarga nada en estas pruebas, así que la URL y el tiempo de espera no importan.
    private final AresepClient cliente = new AresepClient(URI.create("http://localhost"), 1);

    // Una fila válida del pliego de ARESEP. Las pruebas la copian y le cambian un valor para cada caso.
    private static final String FILA = """
        {"codigoRuta":"I-1","nombreRuta":" DESAMPARADOS-MORAVIA ",
         "codigoRamal":"R0002","nombreRamal":"DESAMPARADOS-MORAVIA",
         "codigoFraccionamiento":"T0001","nombreFraccionamiento":"DESAMPARADOS-MORAVIA",
         "tarifaRegular":"350.00","tarifaAdultoMayor":"0.00","promedioKmViaje":"13.15",
         "fechaVigencia":"2026-09-04T00:00:00.000","provincia":null}
        """;

    /** Arma una respuesta exitosa de ARESEP con las filas dadas, separadas por coma. */
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

    // ARESEP a veces publica la misma fila dos veces (T7223, noviembre de 2026): se usa una sola.
    @Test
    void unaFraccionRepetidaConLosMismosDatosSeImportaUnaVez() {
        List<TarifaAresep> tarifas = cliente.analizarTarifas(respuesta(FILA + "," + FILA));

        assertEquals(1, tarifas.size());
        assertEquals("T0001", tarifas.getFirst().codigoFraccion());
    }

    // Mismo código, distinta tarifa: no hay forma de saber cuál es la correcta, así que se rechaza todo.
    @Test
    void rechazaUnaFraccionRepetidaConDatosDistintos() {
        String otraTarifa = FILA.replace("350.00", "400.00");

        var error = assertThrows(IllegalArgumentException.class,
                () -> cliente.analizarTarifas(respuesta(FILA + "," + otraTarifa)));

        assertTrue(error.getMessage().contains("codigoFraccionamiento=T0001"));
        assertTrue(error.getMessage().contains("duplicado con datos distintos"));
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

    // La primera fila es válida y la segunda (T0002) no: el error tiene que decir cuál fila falló y por qué.
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