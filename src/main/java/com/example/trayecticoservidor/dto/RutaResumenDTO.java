package com.example.trayecticoservidor.dto;

/**
 *Propósito: Representar la información básica (resumen) de una ruta de transporte.
 *Funcionalidad:
 *Se utiliza de forma exclusiva como respuesta para el endpoint "GET /rutas".
 *Su objetivo es evitar sobrecargar la red enviando toda la jerarquía de ramales y
 *desplazamientos cuando el cliente (Angular) solo necesita pintar un menú o listado principal.
 *Al definirlo como un 'record', Java maneja de forma automática la inmutabilidad,
 *la creación de getters y la serialización directa a JSON.
 *@param codigo El identificador único de la ruta (ej. "0002").
 *@param nombre El nombre descriptivo de la ruta (ej. "San José - Sabana - Estadio").
 */
public record RutaResumenDTO(String codigo, String nombre){

}
