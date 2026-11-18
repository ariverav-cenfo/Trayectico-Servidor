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
 * Define una arquitectura stateless con endpoints públicos de solo lectura.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    /**
     * Configura la cadena de filtros de seguridad HTTP, deshabilitando CSRF,
     * estableciendo sesiones sin estado y restringiendo el acceso a los endpoints.
     *
     * @param http instancia de {@link HttpSecurity} a configurar
     * @return la cadena de filtros construida
     * @throws Exception si ocurre un error durante la configuración de seguridad
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Aplica la configuración definida en el bean CorsConfigurationSource
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/rutas/**", "/avisos/**", "/viajes/**", "/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/rutas/buscar").permitAll()
                        .anyRequest().authenticated()
                );
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
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}