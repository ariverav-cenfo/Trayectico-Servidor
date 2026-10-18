package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class HealthDAOTest extends AbstractIntegrationTest {

    @Autowired
    private HealthDAO healthDAO;


    @Test
    void isDatabaseConnected_ReturnsTrueWhenDatabaseIsUP() {
        boolean result = healthDAO.isDatabaseConnected();

        assertTrue(result, "HealthDAO hablando con el testcontainer de postgre");
    }
}