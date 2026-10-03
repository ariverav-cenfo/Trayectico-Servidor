package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.HealthDAO;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private final HealthDAO healthDAO;

    public HealthService(HealthDAO healthDAO) {
        this.healthDAO = healthDAO;
    }

    public boolean isHealthy() {
        return healthDAO.isDatabaseConnected();
    }
}