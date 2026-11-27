package com.example.trayecticoservidor.controller;

import com.example.trayecticoservidor.dto.InterpretacionViajeRequestDTO;
import com.example.trayecticoservidor.dto.InterpretacionViajeResponseDTO;
import com.example.trayecticoservidor.exception.GeocodificacionException;
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
     * @return Respuesta HTTP con el resultado de la interpretación.
     */
    @PostMapping("/interpretar")
    public ResponseEntity<?> interpretar(
            @RequestBody(required = false) InterpretacionViajeRequestDTO solicitud) {

        if (solicitud == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "La solicitud no puede estar vacía."));
        }

        try {
            InterpretacionViajeResponseDTO resultado;

            boolean haySeleccion =
                    solicitud.origenSeleccionado() != null
                            || solicitud.destinoSeleccionado() != null;

            if (haySeleccion) {
                if (solicitud.origenSeleccionado() == null
                        || solicitud.destinoSeleccionado() == null) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("mensaje",
                                    "Debe seleccionar tanto el origen como el destino."));
                }

                resultado = interpretacionViajeService.confirmarSeleccion(
                        solicitud.origenSeleccionado(),
                        solicitud.destinoSeleccionado()
                );

            } else {
                if (solicitud.mensaje() == null || solicitud.mensaje().isBlank()) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("mensaje",
                                    "Debe proporcionar un mensaje o seleccionar origen y destino."));
                }

                resultado = interpretacionViajeService.extraerOrigenDestino(
                        solicitud.mensaje()
                );
            }

            return ResponseEntity.ok(resultado);

        } catch (GroqException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "mensaje", e.getMessage()
                    ));

        } catch (GeocodificacionException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                            "mensaje", e.getMessage()
                    ));
        }
    }
}