package com.example.trayecticoservidor.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Cliente encargado de consultar el servicio de geocodificación Photon.
 */
@Component
public class PhotonClient {

    private static final Logger LOG = LoggerFactory.getLogger(PhotonClient.class);

    private static final double CENTRO_LAT = 9.9281;
    private static final double CENTRO_LON = -84.0907;

    private static final double LAT_MIN = 9.75;
    private static final double LAT_MAX = 10.15;
    private static final double LON_MIN = -84.45;
    private static final double LON_MAX = -83.75;

    private static final int LIMITE_RESULTADOS = 5;

    private final URI url;
    private final Duration tiempoEspera;
    private final HttpClient http;
    private final ObjectMapper mapper;

    /**
     * Construye el cliente de Photon.
     *
     * @param url URL del servicio Photon.
     * @param segundosTiempoEspera Tiempo máximo de espera de la respuesta.
     * @param mapper Conversor JSON de Jackson.
     */
    public PhotonClient(
            @Value("${photon.url:https://photon.komoot.io/api/}") URI url,
            @Value("${photon.timeout-seconds:15}") long segundosTiempoEspera,
            ObjectMapper mapper) {

        this.url = url;
        this.tiempoEspera = Duration.ofSeconds(segundosTiempoEspera);
        this.mapper = mapper;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Consulta Photon utilizando el nombre de un lugar.
     *
     * @param nombre Nombre del lugar que se desea buscar.
     * @return Respuesta JSON de Photon.
     * @throws IllegalStateException Si ocurre un error de comunicación o la respuesta es inválida.
     */
    public JsonNode buscar(String nombre) {

        String consulta = URLEncoder.encode(nombre, StandardCharsets.UTF_8);

        String parametros = "?q=" + consulta
                + "&limit=" + LIMITE_RESULTADOS
                + "&lat=" + CENTRO_LAT
                + "&lon=" + CENTRO_LON
                + "&bbox=" + LON_MIN + "," + LAT_MIN + "," + LON_MAX + "," + LAT_MAX;

        URI uriConsulta = URI.create(url.toString() + parametros);

        HttpRequest solicitud = HttpRequest.newBuilder(uriConsulta)
                .timeout(tiempoEspera)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> respuesta = http.send(
                    solicitud,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (respuesta.statusCode() != 200) {
                throw new IllegalStateException(
                        "Photon devolvió HTTP " + respuesta.statusCode()
                );
            }

            JsonNode raiz = mapper.readTree(respuesta.body());

            if (raiz == null || !raiz.path("features").isArray()) {
                throw new IllegalStateException("Respuesta inválida de Photon");
            }

            return raiz;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta a Photon interrumpida", e);

        } catch (IOException e) {
            LOG.error("No se pudo consultar el servicio Photon", e);
            throw new IllegalStateException("No se pudo comunicar con Photon", e);
        }
    }
}
