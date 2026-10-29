package com.example.trayecticoservidor.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional // Revierte los cambios en la BD después de cada prueba
@Sql(scripts = "/datos-prueba-rutas.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class RutaControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void debeRetornarListaDeRutas() throws Exception {
        mockMvc.perform(get("/rutas"))
                .andDo(print()) // Muestra la solicitud y respuesta en consola
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void debeRetornarDetalleDeRuta0002() throws Exception {
        mockMvc.perform(get("/rutas/0002"))
                .andDo(print()) // Muestra la solicitud y respuesta en consola
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("0002"));
    }

    @Test
    void debeRetornar404SiRutaNoExiste() throws Exception {
        mockMvc.perform(get("/rutas/9999"))
                .andDo(print()) // Muestra la solicitud y respuesta en consola
                .andExpect(status().isNotFound());
    }
}