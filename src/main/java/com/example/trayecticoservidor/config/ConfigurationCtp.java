package com.example.trayecticoservidor.config;


import ctpcliente.CtpCliente;
import ctpcliente.CtpConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ConfigurationCtp {

    // Un solo cliente para toda la app: comparte conexiones, la pausa entre solicitudes y la caché de catálogos
    @Bean(destroyMethod = "close")
    public CtpCliente ctpCliente() {
        CtpConfig config = CtpConfig.constructor()
                .intervaloMinimo(Duration.ofMillis(250))
                .agenteUsuario("trayectico/1.0 (Kal Code, CENFOTEC)")
                .construir();
        return CtpCliente.crear(config);
    }

}
