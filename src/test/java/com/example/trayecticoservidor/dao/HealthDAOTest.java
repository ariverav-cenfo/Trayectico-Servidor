package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.AbstractIntegrationTest;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HealthDAOTest extends AbstractIntegrationTest {

    @Autowired
    private HealthDAO healthDAO;



    @Test
    void isDatabaseConnected_ReturnsTrueWhenDatabaseIsUP() {
        boolean result = healthDAO.isDatabaseConnected();

        assertTrue(result, "HealthDAO hablando con el testcontainer de postgre");
    }

    @Test
    void getPostGisVersion_ReturnsVersionWhenExtensionIsEnabled(){
        String version = healthDAO.getPostgisVersion();

        System.out.println("Version de postgis: "+version);
        assertNotNull(version);
        assertTrue(version.startsWith("3."),"Version inesperada de postgis:" + version);
    }

    @Test
    void getPgRoutingVersion_ReturnsVersionWhenExtensionIsEnable(){
        String version = healthDAO.getPgRoutingVersion();

        System.out.println("Version de PGrouting: "+ version);
        assertNotNull(version);
        assertTrue(version.startsWith("3."),"version inesperada de pgrouting: "+version);
    }

}