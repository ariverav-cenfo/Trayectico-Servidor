package com.example.trayecticoservidor.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional // Revierte los cambios en la BD después de cada prueba
@Sql(scripts = "/datos-prueba-rutas.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class RutaControllerTest {

    private static final DockerImageName POSTGIS_NAME = DockerImageName.
            parse("pgrouting/pgrouting:16-3.5-3.8").
            asCompatibleSubstituteFor("postgres");

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("pgrouting/pgrouting:16-3.5-3.8")
                    .asCompatibleSubstituteFor("postgres")
    );

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void debeRetornarListaDeRutas() throws Exception {
        mockMvc.perform(get("/rutas"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre_ruta").value("SAN JOSÉ - SABANA CEMENTERIO"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].codigo_ruta").value("0002"));
    }

    @Test
    void debeRetornarDetalleDeRuta0002() throws Exception {
        mockMvc.perform(get("/rutas/0002"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo_ruta").value("0002"))
                // El nombre de la ruta debe coincidir con el primer ramal
                .andExpect(jsonPath("$.nombre_ruta").value("SAN JOSÉ - SABANA CEMENTERIO"))
                // Ahora insertamos 2 ramales en el SQL
                .andExpect(jsonPath("$.ramales", hasSize(2)))
                // El nombre del primer ramal
                .andExpect(jsonPath("$.ramales[0].nombre_ramal").value("SAN JOSÉ - SABANA CEMENTERIO"))
                // El primer ramal tiene 2 desplazamientos
                .andExpect(jsonPath("$.ramales[0].recorridos", hasSize(2)));
    }

    @Test
    void debeRetornar404SiRutaNoExiste() throws Exception {
        mockMvc.perform(get("/rutas/9999"))
                .andDo(print())
                .andExpect(status().isNotFound());
        }
}