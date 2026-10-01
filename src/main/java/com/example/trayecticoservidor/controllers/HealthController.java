package com.example.trayecticoservidor.controllers;

import com.example.trayecticoservidor.service.HealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        if (healthService.isHealthy()) {
            return ResponseEntity.ok("OK");
        }

        return ResponseEntity.internalServerError().body("Database connection failed");
    }
}