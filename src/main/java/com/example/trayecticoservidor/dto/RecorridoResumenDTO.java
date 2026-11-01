package com.example.trayecticoservidor.dto;
/**
 *Propósito: Contener los datos esenciales de un desplazamiento o recorrido individual.
 *Funcionalidad:
 *Es el nivel más bajo en la jerarquía del JSON expuesto en "GET /rutas/{codigo}".
 *Le indica al frontend si el recorrido está habilitado (activo) y hacia dónde se dirige (sentido).
 *Deliberadamente omite las paradas y el trazado geográfico (geometría) por ahora,
 *garantizando que la carga inicial de los detalles de la ruta sea rápida.
 *@param codigo_recorrido El identificador del recorrido.
 *@param sentido_recorrido La dirección del trayecto.
 *@param activo Bandera booleana que indica si el recorrido está operativo.
 */
public record RecorridoResumenDTO(
        String codigo_recorrido,
        String sentido_recorrido,
        boolean activo
){
}
