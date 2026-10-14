package ctpcliente.model;

import java.util.Optional;

/**
 * Carretera de la red vial nacional.
 *
 * @param id         id que espera el filtro {@code ruar} de la consulta correspondiente
 * @param numero     numero de la ruta nacional
 * @param envolvente rectangulo envolvente, en CRTM05
 */
public record RutaNacional(String id, int numero, Optional<Envolvente> envolvente) {
}
