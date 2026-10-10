package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.HealthDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

    @Mock
    private HealthDAO healthDAO;

    @InjectMocks
    private HealthService healthService;

    @Test
    void isHealthy_WhenDatabaseIsConnected_ReturnsTrue() {
        when(healthDAO.isDatabaseConnected()).thenReturn(true);

        boolean result = healthService.isHealthy();

        assertTrue(result);
    }

    @Test
    void isHealthy_WhenDatabaseIsNotConnected_ReturnsFalse() {
        when(healthDAO.isDatabaseConnected()).thenReturn(false);

        boolean result = healthService.isHealthy();

        assertFalse(result);
    }
}