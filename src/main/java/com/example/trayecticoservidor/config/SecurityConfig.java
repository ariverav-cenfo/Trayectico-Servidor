package com.example.trayecticoservidor.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de seguridad y políticas CORS de la aplicación.
 * Define una arquitectura stateless con endpoints públicos de solo lectura
 * y valida el id_token de Google en los endpoints protegidos.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    /**
     * Configura la cadena de filtros de seguridad HTTP, deshabilitando CSRF,
     * estableciendo sesiones sin estado, restringiendo el acceso a los endpoints
     * y validando el token de Google como resource server.
     * Las rutas bajo /api/admin solo las usa el rol ADMIN.
     *
     * @param http instancia de {@link HttpSecurity} a configurar
     * @param rolJwtConverter asigna el rol según el correo del token
     * @param erroresSeguridad responde los 401 y 403 con un mensaje genérico
     * @return la cadena de filtros construida
     * @throws Exception si ocurre un error durante la configuración de seguridad
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RolJwtConverter rolJwtConverter,
                                                   ErroresSeguridadHandler erroresSeguridad) throws Exception {
        http
                // Aplica la configuración definida en el bean CorsConfigurationSource
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/admin/**").hasRole(RolJwtConverter.ROL_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/rutas/**", "/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/rutas/buscar").permitAll()
                        .anyRequest().authenticated()
                )
                // Peticiones sin token (401) o sin el rol requerido (403)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(erroresSeguridad)
                        .accessDeniedHandler(erroresSeguridad))
                // Valida el id_token de Google; los tokens inválidos o vencidos también dan 401 genérico
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolJwtConverter))
                        .authenticationEntryPoint(erroresSeguridad)
                        .accessDeniedHandler(erroresSeguridad));
        return http.build();
    }

    /**
     * Define la fuente de configuración CORS global para toda la aplicación,
     * restringiendo orígenes, métodos y cabeceras permitidas.
     *
     * @return instancia configurada de {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(allowedOrigins);
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS","PATCH"));
        corsConfiguration.setAllowedHeaders(List.of("Authorization", "Content-Type", "ngrok-skip-browser-warning"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}