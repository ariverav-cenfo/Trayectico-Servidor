package com.example.trayecticoservidor.controller;

import com.example.trayecticoservidor.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Sql(scripts = "/datos-prueba-adminRutas.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@WithMockUser(roles = "ADMIN")
class RutaAdminControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("GET /api/admin/rutas debe exponer todos los campos internos incluyendo los ocultos al cliente")
    void listarRutasAdmin_exponerCamposCompletos()throws Exception{
        mockMvc.perform(get("/api/admin/rutas"))
                .andExpect(jsonPath("$[?(@.codigo_ctp == '9901')].id", contains(9901)))
                .andExpect(jsonPath("$[?(@.codigo_ctp == '9901')].id_ctp", contains(9901)))
                .andExpect(jsonPath("$[?(@.codigo_ctp == '9901')].nombre", contains(nullValue())))
                .andExpect(jsonPath("$[?(@.codigo_ctp == '9901')].activo", contains(true)))
                .andExpect(jsonPath("$[?(@.codigo_ctp == '9901')].recorridos[*].codigo_ctp",
                        containsInAnyOrder("9901-A-1", "9901-A-2")))
                .andExpect(jsonPath("$[?(@.codigo_ctp == '9901')].recorridos[0].estado_ctp", contains("false")))
                .andExpect(jsonPath("$[?(@.codigo_ctp == '9901')].recorridos[0].longitud_m", contains(11400.0)));
    }

    @Test
    @DisplayName("PATCH /api/admin/rutas{codigo}/estado debe deshabilitar todos los recorridos asociados a la ruta sin afectar otras rutas")
    void deshabilitarRuta_actualizaTodosLosRecorridosDeLaRuta()throws Exception{
        mockMvc.perform(patch("/api/admin/rutas/9901/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "activo": false }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo_ctp").value("9901"))
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.recorridos[*].activo", everyItem(is(false))));

        // Verificar directamente en BD que ambos recorridos de 9901 quedaron en false
        Integer activos9901 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recorridos WHERE SPLIT_PART(codigo_ctp, '-', 1) = '9901' AND activo = true",
                Integer.class
        );
        assertThat(activos9901).isZero();

        // Verificar que la ruta 9902 no se vio afectada
        Boolean activo9902 = jdbcTemplate.queryForObject(
                "SELECT activo FROM recorridos WHERE codigo_ctp = '9902-A-1'",
                Boolean.class
        );
        assertThat(activo9902).isTrue();
    }

    @Test
    @DisplayName("PATCH /api/admin/rutas/{codigo}/estado debe volver habilitar una ruta deshabilitada")
    void habilitarRuta_reactivaTodosLosRecorridos()throws Exception{
        jdbcTemplate.update("UPDATE recorridos SET activo = false WHERE SPLIT_PART(codigo_ctp, '-', 1) = '9901'");

        mockMvc.perform(patch("/api/admin/rutas/9901/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "activo": true }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo_ctp").value("9901"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.recorridos[*].activo", everyItem(is(true))));

        Integer activos9901 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recorridos WHERE SPLIT_PART(codigo_ctp, '-', 1) = '9901' AND activo = true",
                Integer.class
        );
        assertThat(activos9901).isEqualTo(2);
    }

    @Test
    @DisplayName("PATCH /api/admin/rutas/{codigo}/estado debe retornar 404 si el codigo de ruta no existe")
    void cambiarEstadoRutaInexistente_Retorna404()throws Exception{
        mockMvc.perform(patch("/api/admin/rutas/0000/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"activo":false}
                        """))
                .andExpect(status().isNotFound());
    }
}