package com.example.trayecticoservidor.controller;

import com.example.trayecticoservidor.dto.ViajeRequestDTO;
import com.example.trayecticoservidor.dto.ViajeResponseDTO;
import com.example.trayecticoservidor.service.CalculoViajeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rutas")
public class CalculosViajeController {
    private final CalculoViajeService calculoViajeService;

    public CalculosViajeController(CalculoViajeService calculoViajeService){
        this.calculoViajeService = calculoViajeService;
    }

    @PostMapping("/buscar")
    public ResponseEntity<ViajeResponseDTO> buscarRecorrido(@RequestBody ViajeRequestDTO request){
        ViajeResponseDTO response = calculoViajeService.calcularViaje(request);
        return ResponseEntity.ok(response);
    }

}
