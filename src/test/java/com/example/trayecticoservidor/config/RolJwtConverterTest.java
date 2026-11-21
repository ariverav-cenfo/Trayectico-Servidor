package com.example.trayecticoservidor.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RolJwtConverterTest {

    private final RolJwtConverter converter = new RolJwtConverter(List.of("Admin@Ejemplo.com"));

    @Test
    void correoAdminVerificado_recibeRolAdmin() {
        AbstractAuthenticationToken auth = converter.convert(token("admin@ejemplo.com", true));
        assertEquals(List.of("ROLE_ADMIN"), roles(auth));
        assertEquals("admin@ejemplo.com", auth.getName());
    }

    @Test
    void correoNuevo_recibeRolUsuario() {
        assertEquals(List.of("ROLE_USUARIO"), roles(converter.convert(token("nuevo@gmail.com", true))));
    }

    @Test
    void correoAdminSinVerificar_recibeRolUsuario() {
        assertEquals(List.of("ROLE_USUARIO"), roles(converter.convert(token("admin@ejemplo.com", false))));
    }

    private Jwt token(String correo, boolean verificado) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("123")
                .claim("email", correo)
                .claim("email_verified", verificado)
                .build();
    }

    private List<String> roles(AbstractAuthenticationToken auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }
}