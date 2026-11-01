package com.example.trayecticoservidor.client;

import com.example.trayecticoservidor.model.TarifaAresep;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Component
public class AresepClient {
    private final URI url;
    private final Duration tiempoEspera;
    private final ObjectMapper mapper = new ObjectMapper();

    public AresepClient(@Value("${aresep.url}") URI url,
                        @Value("${aresep.timeout-seconds:120}") long segundosTiempoEspera) {
        this.url = url;
        this.tiempoEspera = Duration.ofSeconds(segundosTiempoEspera);
    }

    public List<TarifaAresep> obtenerTarifas() {
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()) {
            HttpRequest solicitud = HttpRequest.newBuilder(url).timeout(tiempoEspera)
                    .header("Accept", "application/json").GET().build();
            HttpResponse<String> respuesta = http.send(solicitud, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() != 200) {
                throw new IllegalStateException("ARESEP devolvio HTTP " + respuesta.statusCode());
            }
            return analizarTarifas(respuesta.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Importacion ARESEP interrumpida", e);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo descargar ARESEP; se conservan las tarifas anteriores", e);
        }
    }

    public List<TarifaAresep> analizarTarifas(String json) {
        try {
            JsonNode raiz = mapper.readTree(json);
            if (raiz == null || !raiz.path("metadata").path("success").asBoolean(false)
                    || raiz.path("metadata").path("status").asInt() != 200
                    || !raiz.path("value").isArray() || raiz.path("value").isEmpty()) {
                throw new IllegalArgumentException("Respuesta ARESEP fallida o sin tarifas");
            }
            List<TarifaAresep> tarifas = new ArrayList<>();
            HashSet<String> codigos = new HashSet<>();
            for (JsonNode fila : raiz.path("value")) {
                String codigo = null;
                try {
                    codigo = obligatorio(fila, "codigoFraccionamiento");
                    if (!codigos.add(codigo)) {
                        throw new IllegalArgumentException("Fraccionamiento ARESEP duplicado: " + codigo);
                    }
                    tarifas.add(new TarifaAresep(obligatorio(fila, "codigoRuta"), obligatorio(fila, "nombreRuta"),
                            obligatorio(fila, "codigoRamal"), obligatorio(fila, "nombreRamal"), codigo,
                            obligatorio(fila, "nombreFraccionamiento"), decimal(fila, "promedioKmViaje", false),
                            decimal(fila, "tarifaRegular", true), decimal(fila, "tarifaAdultoMayor", true),
                            texto(fila, "resolucion"), fecha(fila, "fechaResolucion", false),
                            texto(fila, "gaceta"), texto(fila, "alcance"), fecha(fila, "fechaGaceta", false),
                            fecha(fila, "fechaVigencia", true), texto(fila, "expediente"), texto(fila, "operadores"),
                            texto(fila, "tipoServicio"), texto(fila, "provincia")));
                } catch (IllegalArgumentException | ArithmeticException | java.time.DateTimeException e) {
                    throw new IllegalArgumentException("Fila ARESEP con codigoFraccionamiento="
                            + (codigo == null ? "desconocido" : codigo) + ": " + e.getMessage(), e);
                }
            }
            return List.copyOf(tarifas);
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON de ARESEP invalido", e);
        }
    }

    private static String texto(JsonNode fila, String campo) {
        JsonNode nodo = fila.get(campo);
        if (nodo == null || nodo.isNull()) return null;
        if (!nodo.isValueNode()) throw new IllegalArgumentException("Campo invalido: " + campo);
        return nodo.asText().strip();
    }

    private static String obligatorio(JsonNode fila, String campo) {
        String valor = texto(fila, campo);
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Falta campo ARESEP: " + campo);
        return valor;
    }

    private static BigDecimal decimal(JsonNode fila, String campo, boolean obligatorio) {
        String valor = obligatorio ? obligatorio(fila, campo) : texto(fila, campo);
        if (valor == null || valor.isBlank()) return null;
        BigDecimal monto = new BigDecimal(valor).setScale(2, java.math.RoundingMode.UNNECESSARY);
        if (monto.signum() < 0 || monto.precision() > 12) {
            throw new IllegalArgumentException("Monto o distancia ARESEP fuera de rango: " + campo);
        }
        return monto;
    }

    private static LocalDate fecha(JsonNode fila, String campo, boolean obligatorio) {
        String valor = obligatorio ? obligatorio(fila, campo) : texto(fila, campo);
        if (valor == null || valor.isBlank()) return null;
        return java.time.LocalDateTime.parse(valor).toLocalDate();
    }
}