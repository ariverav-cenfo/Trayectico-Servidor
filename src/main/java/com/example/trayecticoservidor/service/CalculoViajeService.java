package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dto.ViajeRequestDTO;
import com.example.trayecticoservidor.dto.ViajeResponseDTO;

/**
 * Proposito: Define el metodo con el que se van a construir los reponses
 */
public interface CalculoViajeService {
    ViajeResponseDTO calcularViaje(ViajeRequestDTO request);
}
