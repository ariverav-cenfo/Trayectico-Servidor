package com.example.trayecticoservidor.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de los endpoints de {@link RutaController} contra PostgreSQL con Testcontainers.
 * Antes de cada prueba se cargan los datos de {@code datos-prueba-rutas.sql}: la ruta 0002 con
 * los ramales 0002-A (con tarifa de ARESEP) y 0002-B (sin tarifa).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional // Revierte los cambios en la BD después de cada prueba
@Sql(scripts = "/datos-prueba-rutas.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class RutaControllerTest {

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("pgrouting/pgrouting:16-3.5-3.8")
                    .asCompatibleSubstituteFor("postgres")
    );

    /**
     * Conecta Spring Boot a la base del contenedor de pruebas.
     *
     * @param registry registro de propiedades dinámicas de Spring.
     */
    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    /**
     * {@code GET /rutas} devuelve el listado con el nombre heredado del primer ramal.
     *
     * @throws Exception si falla la petición simulada.
     */
    @Test
    void debeRetornarListaDeRutas() throws Exception {
        mockMvc.perform(get("/api/rutas"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre_ruta").value("SAN JOSÉ - SABANA CEMENTERIO"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].codigo_ruta").value("0002"));
    }

    /**
     * {@code GET /rutas/0002} devuelve la ruta con sus ramales y recorridos.
     *
     * @throws Exception si falla la petición simulada.
     */
    @Test
    void debeRetornarDetalleDeRuta0002() throws Exception {
        mockMvc.perform(get("/api/rutas/0002"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo_ruta").value("0002"))
                .andExpect(jsonPath("$.nombre_ruta").value("SAN JOSÉ - SABANA CEMENTERIO"))
                .andExpect(jsonPath("$.ramales", hasSize(2)))
                .andExpect(jsonPath("$.ramales[0].nombre_ramal").value("SAN JOSÉ - SABANA CEMENTERIO"))
                .andExpect(jsonPath("$.ramales[0].recorridos", hasSize(2)));
    }

    /**
     * Cada ramal trae su tarifa en colones; el que no tiene tarifa trae {@code tarifa: null}
     * para que el cliente muestre que no está disponible (SCRUM-116).
     *
     * @throws Exception si falla la petición simulada.
     */
    @Test
    void debeIncluirLaTarifaDeCadaRamalONullSiNoTiene() throws Exception {
        mockMvc.perform(get("/rutas/0002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ramales[0].codigo_ramal").value("0002-A"))
                .andExpect(jsonPath("$.ramales[0].tarifa.monto_colones").value(240.0))
                .andExpect(jsonPath("$.ramales[0].tarifa.exacta").value(true))
                .andExpect(jsonPath("$.ramales[1].codigo_ramal").value("0002-B"))
                .andExpect(jsonPath("$.ramales[1].tarifa").value(nullValue()));
    }

    /**
     * {@code GET /rutas/9999} responde 404 porque la ruta no existe.
     *
     * @throws Exception si falla la petición simulada.
     */
    @Test
    void debeRetornar404SiRutaNoExiste() throws Exception {
        mockMvc.perform(get("/api/rutas/9999"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}