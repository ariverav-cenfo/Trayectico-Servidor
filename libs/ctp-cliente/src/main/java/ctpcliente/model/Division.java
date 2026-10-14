package ctpcliente.model;

import java.util.Optional;

/**
 * Provincia, canton o distrito.
 *
 * @param id         codigo INEC: 1 digito (provincia), 3 (canton) o 5 (distrito)
 * @param nombre     nombre oficial
 * @param envolvente rectangulo envolvente, en CRTM05
 */
public record Division(String id, String nombre, Optional<Envolvente> envolvente) {
}
