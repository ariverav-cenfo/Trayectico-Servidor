package com.example.trayecticoservidor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Jackson para la serialización y deserialización JSON.
 */
@Configuration
public class JacksonConfig {

    /**
     * Crea el mapper utilizado para procesar respuestas JSON.
     *
     * @return Instancia de ObjectMapper.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}