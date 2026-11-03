package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dto.RutaDetalleDTO;
import com.example.trayecticoservidor.dto.RutaResumenDTO;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz del servicio para la gestión y consulta del catálogo de rutas.
 */
public interface CatalogoRutasService {

    /**
     * Obtiene una lista resumida de todas las rutas del sistema.
     * @return Lista de RutaResumenDTO.
     */
    List<RutaResumenDTO> obtenerRutas();

    /**
     * Busca y obtiene el detalle completo de una ruta por su código.
     * @param codigo El código CTP de la ruta.
     * @return Un Optional que contiene el detalle de la ruta si existe.
     */
    Optional<RutaDetalleDTO> obtenerRutaPorCodigo(String codigo);

}