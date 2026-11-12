package ctpcliente.model;

/**
 * Ramal: variante de una ruta con recorrido propio.
 *
 * @param id          id interno, requerido por los filtros {@code ramal}
 * @param codigo      codigo publico, p. ej. {@code 0002-A}
 * @param descripcion origen y destino
 */
public record Ramal(int id, String codigo, String descripcion) {
}
