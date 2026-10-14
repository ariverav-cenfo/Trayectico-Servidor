package ctpcliente.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Elemento (feature) GeoJSON de GeoServer.
 *
 * @param id         identificador de GeoServer ({@code capa.fid-...}); no es estable entre peticiones
 * @param geometria  geometria o {@code null} si no se pidio
 * @param atributos  propiedades
 */
public record Elemento(String id, Geometria geometria, Map<String, Object> atributos) implements Valores {

    public Elemento {
        atributos = Collections.unmodifiableMap(new LinkedHashMap<>(atributos));
    }

    public Optional<Geometria> geometriaOpcional() {
        return Optional.ofNullable(geometria);
    }
}
