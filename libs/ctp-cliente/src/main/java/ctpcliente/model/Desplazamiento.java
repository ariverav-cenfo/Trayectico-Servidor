package ctpcliente.model;

import java.util.Optional;

/**
 * Desplazamiento: un ramal recorrido en un sentido.
 *
 * @param id          id interno, requerido por los filtros {@code desplazamiento}
 * @param codigo      codigo publico, p. ej. {@code 0002-A-1}
 * @param sentido     sentido declarado en el nombre, p. ej. {@code 1-2}
 * @param descripcion descripcion del recorrido
 * @param envolvente  rectangulo envolvente del recorrido, en CRTM05
 */
public record Desplazamiento(int id, String codigo, String sentido, String descripcion,
                             Optional<Envolvente> envolvente) {
}
