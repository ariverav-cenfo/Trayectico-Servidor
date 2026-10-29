package com.example.trayecticoservidor.dto;

import java.util.List;

/*
 Propósito: Agrupar y organizar los recorridos bajo una variante específica de la ruta (ramal).
 Funcionalidad:
 Sirve como nodo intermedio en el endpoint detallado "GET /rutas/{codigo}".
 Modela la realidad de negocio del CTP donde una ruta matriz puede tener varias desviaciones
 (ej. Ruta principal: Escazú; Ramal 1: Por pista; Ramal 2: Por los cerros).
 Contiene una lista de los desplazamientos que pertenecen única y exclusivamente a este ramal,
 permitiendo que el frontend de Angular organice la información en el panel lateral.
 @param codigo El identificador único del ramal (ej. "0002-A").
 @param nombre El nombre o descripción del ramal (ej. "Por Sabana Sur").
 @param desplazamientos Colección de desplazamientos (Ida/Vuelta) asociados a este ramal.
 */
public record RamalResumenDTO(
        String codigo,
        String nombre,
        List<DesplazamientoResumenDTO> desplazamientos
) {
}
