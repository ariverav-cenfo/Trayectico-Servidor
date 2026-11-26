package com.example.trayecticoservidor.controller;

import com.example.trayecticoservidor.dto.InterpretacionViajeRequestDTO;
import com.example.trayecticoservidor.dto.InterpretacionViajeResponseDTO;
import com.example.trayecticoservidor.exception.GroqException;
import com.example.trayecticoservidor.service.InterpretacionViajeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador encargado de recibir solicitudes de interpretación de viajes.
 */
@RestController
@RequestMapping("/api/viajes")
public class InterpretacionViajeController {

    private final InterpretacionViajeService interpretacionViajeService;

    public InterpretacionViajeController(
            InterpretacionViajeService interpretacionViajeService) {
        this.interpretacionViajeService = interpretacionViajeService;
    }

    /**
     * Recibe el texto del usuario para interpretar una solicitud de viaje.
     *
     * @param solicitud Solicitud que contiene el mensaje del usuario.
     * @return Respuesta HTTP confirmando la recepción.
     */
    @PostMapping("/interpretar")
    public ResponseEntity<?> interpretar(
            @RequestBody InterpretacionViajeRequestDTO solicitud) {

        try {
            InterpretacionViajeResponseDTO resultado =
                    interpretacionViajeService.extraerOrigenDestino(
                            solicitud.mensaje()
                    );

            return ResponseEntity.ok(resultado);

        } catch (GroqException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "mensaje", e.getMessage()
                    ));
        }
    }
}