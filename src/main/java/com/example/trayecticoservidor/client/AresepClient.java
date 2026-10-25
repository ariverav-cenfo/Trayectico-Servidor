package com.example.trayecticoservidor.client;

import com.example.trayecticoservidor.model.AresepFare;
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
    private final Duration timeout;
    private final ObjectMapper mapper = new ObjectMapper();

    public AresepClient(@Value("${aresep.url}") URI url,
                       @Value("${aresep.timeout-seconds:120}") long timeoutSeconds) {
        this.url = url;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public List<AresepFare> fetchFares() {
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()) {
            HttpRequest request = HttpRequest.newBuilder(url).timeout(timeout)
                    .header("Accept", "application/json").GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("ARESEP devolvio HTTP " + response.statusCode());
            }
            return parseFares(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Importacion ARESEP interrumpida", e);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo descargar ARESEP; se conservan las tarifas anteriores", e);
        }
    }

    public List<AresepFare> parseFares(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            if (root == null || !root.path("metadata").path("success").asBoolean(false)
                    || root.path("metadata").path("status").asInt() != 200
                    || !root.path("value").isArray() || root.path("value").isEmpty()) {
                throw new IllegalArgumentException("Respuesta ARESEP fallida o sin tarifas");
            }
            List<AresepFare> fares = new ArrayList<>();
            HashSet<String> codes = new HashSet<>();
            for (JsonNode row : root.path("value")) {
                String code = required(row, "codigoFraccionamiento");
                if (!codes.add(code)) {
                    throw new IllegalArgumentException("Fraccionamiento ARESEP duplicado: " + code);
                }
                fares.add(new AresepFare(required(row, "codigoRuta"), required(row, "nombreRuta"),
                        required(row, "codigoRamal"), required(row, "nombreRamal"), code,
                        required(row, "nombreFraccionamiento"), decimal(row, "promedioKmViaje", false),
                        decimal(row, "tarifaRegular", true), decimal(row, "tarifaAdultoMayor", true),
                        text(row, "resolucion"), date(row, "fechaResolucion", false),
                        text(row, "gaceta"), text(row, "alcance"), date(row, "fechaGaceta", false),
                        date(row, "fechaVigencia", true), text(row, "expediente"), text(row, "operadores"),
                        text(row, "tipoServicio"), text(row, "provincia")));
            }
            return List.copyOf(fares);
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON de ARESEP invalido", e);
        }
    }

    private static String text(JsonNode row, String field) {
        JsonNode node = row.get(field);
        if (node == null || node.isNull()) return null;
        if (!node.isValueNode()) throw new IllegalArgumentException("Campo invalido: " + field);
        return node.asText().strip();
    }

    private static String required(JsonNode row, String field) {
        String value = text(row, field);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta campo ARESEP: " + field);
        return value;
    }

    private static BigDecimal decimal(JsonNode row, String field, boolean required) {
        String value = required ? required(row, field) : text(row, field);
        if (value == null || value.isBlank()) return null;
        BigDecimal amount = new BigDecimal(value).setScale(2, java.math.RoundingMode.UNNECESSARY);
        if (amount.signum() < 0 || amount.precision() > 12) {
            throw new IllegalArgumentException("Monto o distancia ARESEP fuera de rango: " + field);
        }
        return amount;
    }

    private static LocalDate date(JsonNode row, String field, boolean required) {
        String value = required ? required(row, field) : text(row, field);
        if (value == null || value.isBlank()) return null;
        return java.time.LocalDateTime.parse(value).toLocalDate();
    }
}
