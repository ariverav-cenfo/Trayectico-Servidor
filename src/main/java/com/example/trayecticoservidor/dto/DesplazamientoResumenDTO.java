package com.example.trayecticoservidor.dto;
/**
 *Propósito: Contener los datos esenciales de un desplazamiento o recorrido individual.
 *Funcionalidad:
 *Es el nivel más bajo en la jerarquía del JSON expuesto en "GET /rutas/{codigo}".
 *Le indica al frontend si el recorrido está habilitado (activo) y hacia dónde se dirige (sentido).
 *Deliberadamente omite las paradas y el trazado geográfico (geometría) por ahora,
 *garantizando que la carga inicial de los detalles de la ruta sea rápida.
 *@param codigo El identificador del recorrido (ej. "DESP-001").
 *@param sentido La dirección del trayecto, típicamente "Ida" o "Vuelta".
 *@param activo Bandera booleana que indica si el desplazamiento está operativo.
 */
public record DesplazamientoResumenDTO (
        String codigo,
        String sentido,
        boolean activo
){
}
