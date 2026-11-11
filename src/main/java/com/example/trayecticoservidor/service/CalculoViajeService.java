package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dto.ViajeRequestDTO;
import com.example.trayecticoservidor.dto.ViajeResponseDTO;

public interface CalculoViajeService {
    ViajeResponseDTO calcularViaje(ViajeRequestDTO request);
}
