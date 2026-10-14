package ctpcliente.model;

/**
 * Ruta de autobus del catalogo del CTP.
 *
 * @param id     id interno, requerido por los filtros {@code ruta}
 * @param codigo codigo publico, p. ej. {@code 0002}
 */
public record Ruta(int id, String codigo) {
}
