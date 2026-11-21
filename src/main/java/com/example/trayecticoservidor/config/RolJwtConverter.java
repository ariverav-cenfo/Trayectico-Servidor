package com.example.trayecticoservidor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Convierte el id_token de Google en una autenticación de Spring Security
 * y le asigna el rol según la lista de correos admin.
 */
@Component
public class RolJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_USUARIO = "USUARIO";

    private final Set<String> correosAdmin;

    /**
     * Crea el convertidor con la lista de correos que tienen rol admin.
     *
     * @param correosAdmin correos admin, tomados de la variable ADMIN_EMAILS
     */
    public RolJwtConverter(@Value("${app.seguridad.correos-admin}") List<String> correosAdmin) {
        this.correosAdmin = correosAdmin.stream()
                .map(correo -> correo.trim().toLowerCase(Locale.ROOT))
                .filter(correo -> !correo.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Asigna ROLE_ADMIN si el correo está en la lista y Google lo verificó;
     * cualquier otra cuenta recibe ROLE_USUARIO.
     *
     * @param jwt id_token de Google ya validado
     * @return autenticación con el correo como nombre y el rol asignado
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String correo = jwt.getClaimAsString("email");
        String rol = esAdmin(jwt, correo) ? ROL_ADMIN : ROL_USUARIO;
        String nombre = correo != null ? correo : jwt.getSubject();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + rol)), nombre);
    }

    /**
     * Revisa si el correo del token pertenece a un admin. Exige email_verified
     * para que nadie pueda hacerse pasar por un admin con un correo sin verificar.
     */
    private boolean esAdmin(Jwt jwt, String correo) {
        boolean correoVerificado = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
        return correo != null && correoVerificado && correosAdmin.contains(correo.toLowerCase(Locale.ROOT));
    }
}