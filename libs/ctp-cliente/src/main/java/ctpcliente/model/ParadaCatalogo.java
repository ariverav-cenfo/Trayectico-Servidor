package ctpcliente.model;

import java.util.Optional;

/**
 * Parada del catalogo {@code getparadas}.
 *
 * @param id        id del catalogo; no coincide con el id de las capas WFS
 * @param nombre    descripcion de la ubicacion
 * @param ubicacion punto en CRTM05
 */
public record ParadaCatalogo(int id, String nombre, Optional<Coordenada> ubicacion) {
}
