package com.example.trayecticoservidor.config;

import com.example.trayecticoservidor.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityConfigTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Reemplaza el decodificador de Google: permite simular tokens rechazados sin ir a internet
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void endpointsPublicosGet_sinLogin_retornan200() throws Exception {
        mockMvc.perform(get("/api/rutas"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    @Test
    void endpointProtegidoPost_sinLogin_retorna401() throws Exception {
        mockMvc.perform(post("/api/rutas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioActual_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/usuarios/yo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo_error").value(ErroresSeguridadHandler.CODIGO_NO_AUTENTICADO));
    }

    @Test
    void usuarioActual_conTokenAdmin_retornaCorreoYRol() throws Exception {
        mockMvc.perform(get("/api/usuarios/yo")
                        .with(jwt()
                                .jwt(token -> token.claim("email", "admin@ejemplo.com"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("admin@ejemplo.com"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void corsPreflight_origenPermitido_retornaCabeceraAllowOrigin() throws Exception {
        mockMvc.perform(options("/api/rutas")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    void corsPreflight_headerNgrok_esPermitido() throws Exception {
        mockMvc.perform(options("/api/rutas")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "ngrok-skip-browser-warning"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "ngrok-skip-browser-warning"));
    }

    @Test
    void corsPreflight_origenNoPermitido_esRechazado() throws Exception {
        mockMvc.perform(options("/api/rutas")
                        .header(HttpHeaders.ORIGIN, "http://origen-malicioso.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void tokenVencido_retorna401Generico() throws Exception {
        tokenRechazado_retorna401Generico("Jwt expired at 2026-11-23T10:00:00Z");
    }

    @Test
    void tokenDeOtraAplicacion_retorna401Generico() throws Exception {
        tokenRechazado_retorna401Generico("The aud claim is not valid");
    }

    @Test
    void usuarioNoAdmin_enApiAdmin_retorna403Generico() throws Exception {
        mockMvc.perform(get("/api/admin/rutas")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USUARIO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo_error").value(ErroresSeguridadHandler.CODIGO_ACCESO_DENEGADO))
                .andExpect(jsonPath("$.mensaje").value(ErroresSeguridadHandler.MENSAJE_ACCESO_DENEGADO));
    }

    @Test
    void admin_enApiAdmin_pasaLaAutorizacion() throws Exception {
        // Todavía no hay controladores de admin: 404 significa que la seguridad lo dejó pasar
        mockMvc.perform(get("/api/admin/rutas")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    /**
     * Simula que Google rechaza el token y revisa que la respuesta no revele el motivo.
     *
     * @param motivo mensaje interno del rechazo, que no debe llegar al cliente
     */
    private void tokenRechazado_retorna401Generico(String motivo) throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new JwtValidationException(
                motivo, List.of(new OAuth2Error("invalid_token", motivo, null))));

        mockMvc.perform(get("/api/usuarios/yo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-rechazado"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.codigo_error").value(ErroresSeguridadHandler.CODIGO_NO_AUTENTICADO))
                .andExpect(content().string(not(containsString(motivo))));
    }
}