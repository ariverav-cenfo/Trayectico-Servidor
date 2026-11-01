package com.example.trayecticoservidor.dto;

import java.util.List;

/**
 *Propósito: Agrupar y organizar los recorridos bajo una variante específica de la ruta (ramal).
 *Funcionalidad:
 *Sirve como nodo intermedio en el endpoint detallado "GET /rutas/{codigo}".
 *Modela la realidad de negocio del CTP donde una ruta matriz puede tener varias desviaciones.
 *Contiene una lista de los recorridos que pertenecen única y exclusivamente a este ramal,
 *permitiendo que el frontend de Angular organice la información en el panel lateral.
 *@param codigo_ramal El identificador único del ramal.
 *@param nombre_ramal El nombre o descripción del ramal.
 *@param recorridos Colección de recorridos asociados a este ramal.
 */
public record RamalResumenDTO(
        String codigo_ramal,
        String nombre_ramal,
        List<RecorridoResumenDTO> recorridos
) {
}
