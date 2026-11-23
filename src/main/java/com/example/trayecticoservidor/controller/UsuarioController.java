package com.example.trayecticoservidor.controller;

import com.example.trayecticoservidor.config.RolJwtConverter;
import com.example.trayecticoservidor.dto.UsuarioActualDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del usuario autenticado con Google.
 */
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    /**
     * Devuelve el correo y el rol del usuario que hace la petición.
     *
     * @param jwt id_token de Google ya validado
     * @param autenticacion autenticación con el rol asignado
     * @return correo y rol del usuario
     */
    @GetMapping("/yo")
    public UsuarioActualDTO obtenerUsuarioActual(@AuthenticationPrincipal Jwt jwt, Authentication autenticacion) {
        String rol = autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(autoridad -> autoridad.startsWith("ROLE_"))
                .map(autoridad -> autoridad.substring("ROLE_".length()))
                .findFirst()
                .orElse(RolJwtConverter.ROL_USUARIO);
        return new UsuarioActualDTO(jwt.getClaimAsString("email"), rol);
    }
}