package com.example.trayecticoservidor;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba para usar la BD de verdad con CalculosViajesTest, para troubleshooting con problema de conversion de coordenadas
 */

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-db-real")
public abstract class DBRealIntegrationTest {
    @LocalServerPort
    protected int port;

    @BeforeEach
    void setup(){
        RestAssured.port = port;
    }
}
