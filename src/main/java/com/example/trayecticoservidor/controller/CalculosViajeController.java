package com.example.trayecticoservidor.controller;

import com.example.trayecticoservidor.config.WebConfig;
import com.example.trayecticoservidor.dto.ViajeRequestDTO;
import com.example.trayecticoservidor.dto.ViajeResponseDTO;
import com.example.trayecticoservidor.service.CalculoViajeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para el servicio de calculos de viajes, contiene el PostMapping necesario para iniciar el calculo de los viajes segun el request recibido del frontend
 */
@RestController
@RequestMapping("/api/rutas")

public class CalculosViajeController{
    private final CalculoViajeService calculoViajeService;
    private static final Logger log = LoggerFactory.getLogger(CalculosViajeController.class);
    public CalculosViajeController(CalculoViajeService calculoViajeService){
        this.calculoViajeService = calculoViajeService;
    }

    @PostMapping("/buscar")
    public ResponseEntity<ViajeResponseDTO> buscarRecorrido(@RequestBody ViajeRequestDTO request){
        ViajeResponseDTO response = calculoViajeService.calcularViaje(request);
        return ResponseEntity.ok(response);
    }

}
