package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dto.RutaDetalleDTO;
import com.example.trayecticoservidor.dto.RutaResumenDTO;

import java.util.List;
import java.util.Optional;

public interface CatalogoRutasService {

    List<RutaResumenDTO> obtenerRutas();

    Optional<RutaDetalleDTO> obtenerRutaPorCodigo(String codigo);


}
