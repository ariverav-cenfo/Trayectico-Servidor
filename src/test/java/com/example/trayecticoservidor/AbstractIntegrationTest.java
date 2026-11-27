package com.example.trayecticoservidor;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {
//Esta clase es base para cualquier test que ocupe usar la BD
//Genera un contenedor reusable por cada ejecucion de test en lugar de abrir containers por cada clase de prueba
//El test tiene problemas para parsear pgrouting como un substituto de postgres, esto es necesario para definir la imagen explicitamente como un reemplazo de postgre
    private static final DockerImageName PGROUTING_IMAGE =
        DockerImageName.parse("pgrouting/pgrouting:16-3.5-3.8")
            .asCompatibleSubstituteFor("postgres");

    // Llamando a la imagen del container
    protected static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(PGROUTING_IMAGE)
            .withDatabaseName("TrayecticoDB_Test")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }
}