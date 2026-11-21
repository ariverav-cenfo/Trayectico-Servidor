package com.example.trayecticoservidor.config;

import com.example.trayecticoservidor.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityConfigTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void endpointsPublicosGet_sinLogin_retornan200() throws Exception {
        mockMvc.perform(get("/rutas"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    @Test
    void endpointProtegidoPost_sinLogin_retorna401() throws Exception {
        mockMvc.perform(post("/rutas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioActual_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/usuarios/yo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioActual_conTokenAdmin_retornaCorreoYRol() throws Exception {
        mockMvc.perform(get("/usuarios/yo")
                        .with(jwt()
                                .jwt(token -> token.claim("email", "admin@ejemplo.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("admin@ejemplo.com"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void corsPreflight_origenPermitido_retornaCabeceraAllowOrigin() throws Exception {
        mockMvc.perform(options("/rutas")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    void corsPreflight_headerNgrok_esPermitido() throws Exception {
        mockMvc.perform(options("/rutas")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "ngrok-skip-browser-warning"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "ngrok-skip-browser-warning"));
    }

    @Test
    void corsPreflight_origenNoPermitido_esRechazado() throws Exception {
        mockMvc.perform(options("/rutas")
                        .header(HttpHeaders.ORIGIN, "http://origen-malicioso.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}