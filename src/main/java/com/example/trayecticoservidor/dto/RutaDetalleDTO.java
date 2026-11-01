package com.example.trayecticoservidor.dto;

import java.util.List;
import java.util.Optional;

/**
 *Propósito: Estructurar la respuesta completa y jerárquica de una ruta en específico.
 *Funcionalidad:
 *Es el objeto principal devuelto cuando el cliente consulta "GET /rutas/{codigo}".
 *Constituye el contrato JSON.
 *Integra los datos raíz de la ruta (código, nombre) junto con el árbol completo de
 *sus ramales y recorridos en una sola llamada al servidor.
 *@param codigo_ruta El identificador principal de la ruta consultada.
 *@param nombre_ruta El nombre completo de la ruta.
 *@param ramales Colección de ramales con sus respectivos detalles que pertenecen a la ruta.
 */

public record RutaDetalleDTO(
        String codigo_ruta,
        String nombre_ruta,
        List<RamalResumenDTO> ramales
) {
}
