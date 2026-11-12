package com.example.trayecticoservidor.client;

import com.example.trayecticoservidor.model.TarifaAresep;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente encargado de descargar y procesar las tarifas publicadas por ARESEP.
 *
 * <p>Descarga el pliego tarifario de autobuses del servicio de datos abiertos de ARESEP y lo
 * convierte en una lista de {@link TarifaAresep}, una por fraccionamiento (tramo con tarifa propia).
 * Valida toda la respuesta antes de devolverla: si una sola fila es inválida, no se devuelve nada,
 * para que la importación conserve la carga anterior.</p>
 */
@Component
public class AresepClient {
    private static final Logger LOG = LoggerFactory.getLogger(AresepClient.class);

    private final URI url;
    private final Duration tiempoEspera;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Construye el cliente con la URL del servicio ARESEP y el tiempo máximo de espera.
     *
     * @param url URL del servicio de tarifas ARESEP.
     * @param segundosTiempoEspera Tiempo máximo de espera de la solicitud en segundos.
     */
    public AresepClient(@Value("${aresep.url}") URI url,
                        @Value("${aresep.timeout-seconds:120}") long segundosTiempoEspera) {
        this.url = url;
        this.tiempoEspera = Duration.ofSeconds(segundosTiempoEspera);
    }

    /**
     * Descarga las tarifas actuales desde el servicio de ARESEP y las procesa.
     *
     * @return Lista de tarifas obtenidas y validadas.
     * @throws IllegalStateException Si falla la descarga o la respuesta HTTP no es exitosa.
     */
    public List<TarifaAresep> obtenerTarifas() {
        // Se espera como máximo 15 s para conectar y "tiempoEspera" para recibir la respuesta completa.
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()) {
            HttpRequest solicitud = HttpRequest.newBuilder(url).timeout(tiempoEspera)
                    .header("Accept", "application/json").GET().build();
            HttpResponse<String> respuesta = http.send(solicitud, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() != 200) {
                throw new IllegalStateException("ARESEP devolvio HTTP " + respuesta.statusCode());
            }
            return analizarTarifas(respuesta.body());
        } catch (InterruptedException e) {
            // Se restaura la marca de interrupción para que quien llamó sepa que el hilo fue interrumpido.
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Importacion ARESEP interrumpida", e);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo descargar ARESEP; se conservan las tarifas anteriores", e);
        }
    }

    /**
     * Analiza y valida la respuesta JSON del servicio ARESEP.
     *
     * <p>ARESEP a veces publica el mismo fraccionamiento dos veces. Por ejemplo, en noviembre de 2026
     * la T7223 vino repetida y solo cambiaba el promedio de kilómetros (33.00 y 33.10). Si las filas
     * repetidas tienen la misma ruta, ramal, tarifas y fecha de vigencia, se conserva la primera y se
     * deja una advertencia en el log. Si alguno de esos datos cambia, no hay forma de saber cuál es
     * la tarifa correcta y se rechaza la respuesta completa.</p>
     *
     * @param json Respuesta JSON obtenida del servicio ARESEP.
     * @return Lista inmutable de tarifas procesadas, en el mismo orden del JSON y sin repetidos.
     * @throws IllegalArgumentException Si la respuesta es inválida, está vacía o repite un
     *                                  fraccionamiento con distinta tarifa o vigencia.
     */
    public List<TarifaAresep> analizarTarifas(String json) {
        try {
            JsonNode raiz = mapper.readTree(json);
            // La respuesta válida trae metadata.success = true, metadata.status = 200 y una lista "value" con filas.
            if (raiz == null || !raiz.path("metadata").path("success").asBoolean(false)
                    || raiz.path("metadata").path("status").asInt() != 200
                    || !raiz.path("value").isArray() || raiz.path("value").isEmpty()) {
                throw new IllegalArgumentException("Respuesta ARESEP fallida o sin tarifas");
            }
            // Una tarifa por código de fraccionamiento. LinkedHashMap conserva el orden en que vienen en el JSON.
            Map<String, TarifaAresep> tarifas = new LinkedHashMap<>();
            for (JsonNode fila : raiz.path("value")) {
                String codigo = null;
                try {
                    codigo = obligatorio(fila, "codigoFraccionamiento");
                    TarifaAresep tarifa = new TarifaAresep(obligatorio(fila, "codigoRuta"), obligatorio(fila, "nombreRuta"),
                            obligatorio(fila, "codigoRamal"), obligatorio(fila, "nombreRamal"), codigo,
                            obligatorio(fila, "nombreFraccionamiento"), decimal(fila, "promedioKmViaje", false),
                            decimal(fila, "tarifaRegular", true), decimal(fila, "tarifaAdultoMayor", true),
                            texto(fila, "resolucion"), fecha(fila, "fechaResolucion", false),
                            texto(fila, "gaceta"), texto(fila, "alcance"), fecha(fila, "fechaGaceta", false),
                            fecha(fila, "fechaVigencia", true), texto(fila, "expediente"), texto(fila, "operadores"),
                            texto(fila, "tipoServicio"), texto(fila, "provincia"));
                    // putIfAbsent devuelve la tarifa que ya estaba con ese código, o null si es la primera vez.
                    TarifaAresep anterior = tarifas.putIfAbsent(codigo, tarifa);
                    if (anterior != null) {
                        if (!mismaTarifa(anterior, tarifa)) {
                            throw new IllegalArgumentException(
                                    "Fraccionamiento ARESEP duplicado con tarifa o vigencia distinta: " + codigo);
                        }
                        LOG.warn("Fraccionamiento ARESEP {} repetido con la misma tarifa; se conserva la primera fila.",
                                codigo);
                    }
                } catch (IllegalArgumentException | ArithmeticException | java.time.DateTimeException e) {
                    // Cualquier fila inválida rechaza la respuesta completa; el mensaje dice cuál fila falló.
                    throw new IllegalArgumentException("Fila ARESEP con codigoFraccionamiento="
                            + (codigo == null ? "desconocido" : codigo) + ": " + e.getMessage(), e);
                }
            }
            return List.copyOf(tarifas.values());
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON de ARESEP invalido", e);
        }
    }

    /**
     * Indica si dos filas del mismo fraccionamiento describen la misma tarifa.
     *
     * <p>Compara solo los datos que usa la aplicación: ruta, ramal, montos y fecha de vigencia.
     * Los demás campos (kilómetros promedio, gaceta, expediente, operadores) pueden variar entre
     * filas repetidas sin cambiar lo que paga el usuario. Los cinco campos comparados son
     * obligatorios, así que nunca llegan en {@code null}.</p>
     *
     * @param a Primera fila leída del fraccionamiento.
     * @param b Fila repetida del mismo fraccionamiento.
     * @return {@code true} si ambas filas tienen la misma ruta, ramal, tarifas y vigencia.
     */
    private static boolean mismaTarifa(TarifaAresep a, TarifaAresep b) {
        // compareTo en los montos para comparar el valor numérico sin depender de la escala.
        return a.codigoRuta().equals(b.codigoRuta())
                && a.codigoRamal().equals(b.codigoRamal())
                && a.tarifaRegular().compareTo(b.tarifaRegular()) == 0
                && a.tarifaAdultoMayor().compareTo(b.tarifaAdultoMayor()) == 0
                && a.fechaVigencia().equals(b.fechaVigencia());
    }

    /**
     * Lee un campo de texto opcional, sin espacios al inicio ni al final.
     *
     * @return el texto, o {@code null} si el campo no viene o viene en null.
     */
    private static String texto(JsonNode fila, String campo) {
        JsonNode nodo = fila.get(campo);
        if (nodo == null || nodo.isNull()) return null;
        if (!nodo.isValueNode()) throw new IllegalArgumentException("Campo invalido: " + campo);
        return nodo.asText().strip();
    }

    /**
     * Lee un campo de texto que no puede faltar ni venir vacío.
     */
    private static String obligatorio(JsonNode fila, String campo) {
        String valor = texto(fila, campo);
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Falta campo ARESEP: " + campo);
        return valor;
    }

    /**
     * Lee un monto o una distancia con exactamente dos decimales.
     *
     * <p>{@code RoundingMode.UNNECESSARY} hace que un valor con más de dos decimales falle en lugar
     * de redondearse en silencio.</p>
     */
    private static BigDecimal decimal(JsonNode fila, String campo, boolean obligatorio) {
        String valor = obligatorio ? obligatorio(fila, campo) : texto(fila, campo);
        if (valor == null || valor.isBlank()) return null;
        BigDecimal monto = new BigDecimal(valor).setScale(2, java.math.RoundingMode.UNNECESSARY);
        if (monto.signum() < 0 || monto.precision() > 12) {
            throw new IllegalArgumentException("Monto o distancia ARESEP fuera de rango: " + campo);
        }
        return monto;
    }

    /**
     * Lee una fecha de ARESEP, que viene como fecha y hora ({@code 2026-10-28T00:00:00.000}),
     * y se queda solo con la fecha.
     */
    private static LocalDate fecha(JsonNode fila, String campo, boolean obligatorio) {
        String valor = obligatorio ? obligatorio(fila, campo) : texto(fila, campo);
        if (valor == null || valor.isBlank()) return null;
        return java.time.LocalDateTime.parse(valor).toLocalDate();
    }
}