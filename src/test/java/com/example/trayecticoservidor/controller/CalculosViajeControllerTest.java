package com.example.trayecticoservidor.controller;

import com.example.trayecticoservidor.AbstractIntegrationTest;
import com.example.trayecticoservidor.DBRealIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;


class CalculosViajeControllerTest extends AbstractIntegrationTest {
/*
    @LocalServerPort
    private int port;

    @BeforeEach
    void setup(){
        RestAssured.port = port;
    }

    @Test
    @DisplayName("POST /api/rutas/buscar - Debería calcular opciones de viaje válidas para las coordenadas de prueba")
    void testBuscarRecorridoExitoso() {

        String requestJson = """
            {
              "origen": {
              "lon": -84.08406273262446,
              "lat": 9.933756378756598
              },
              "destino": {
              "lon": -84.1062265310545,
              "lat": 9.93303510873382
              }
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(requestJson)
                .when()
                .post("/api/rutas/buscar")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                // Estructura principal
                .body("opciones", is(notNullValue()))
                .body("opciones", instanceOf(java.util.List.class))

                // Validaciones del contrato en snake_case
                .body("opciones[0].transbordos", greaterThanOrEqualTo(0))
                .body("opciones[0].distancia_caminando_metros", greaterThanOrEqualTo(0))
                .body("opciones[0].tramos", is(notNullValue()))
                .body("opciones[0].tramos[0].tipo", anyOf(is("caminata"), is("bus")))

                // Verificación del formato GeoJSON
                .body("opciones[0].tramos[0].geometria.type", is("LineString"))
                .body("opciones[0].tramos[0].geometria.coordinates", is(notNullValue()));
    }

    @Test
    @DisplayName("POST /api/rutas/buscar - Debería retornar 400 Bad Request cuando las coordenadas son inválidas")
    void testBuscarRecorridoCoordenadasInvalidas() {
        String requestJson = """
            {
              "origen": {
                "lon": -84.0832721128259,
                "lat": 120.0000
              },
              "destino": {
                "lon": -84.10931839036672,
                "lat": 9.936750585283297
              }
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(requestJson)
                .when()
                .post("/api/rutas/buscar")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("codigo_http", is(400))
                .body("codigo_error", is("COORDENADAS_INVALIDAS"))
                .body("mensaje", is(notNullValue()))
                .body("path", is("/api/rutas/buscar"));
    }

    @Test
    @DisplayName("POST /api/rutas/buscar - Validación de orden de coordenadas GeoJSON [longitud, latitud]")
    void testOrdenCoordenadasGeoJson() {
        String requestJson = """
            {
              "origen": {
                "lon": -84.08406359198368,
                "lat": 9.933761732019091
              },
              "destino": {
                "lon": -84.1062265310545,
                "lat": 9.93303510873382
              }
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(requestJson)
                .when()
                .post("/api/rutas/buscar")
                .then()
                .statusCode(200)
                // GeoJSON requiere que la primera coordenada sea Longitud (negativa en Costa Rica ~ -84.x)
                // y la segunda sea Latitud (positiva en Costa Rica ~ 9.x)
                .body("opciones[0].tramos[0].geometria.coordinates[0][0]", lessThan(0.0f))
                .body("opciones[0].tramos[0].geometria.coordinates[0][1]", greaterThan(0.0f));
    }
*/
}