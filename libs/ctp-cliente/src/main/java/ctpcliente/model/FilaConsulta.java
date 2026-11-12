package ctpcliente.model;

import ctpcliente.util.Wkt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Fila devuelta por {@code queryTablaDatos}. Los campos dependen de la caracterizacion.
 */
public record FilaConsulta(Map<String, Object> atributos) implements Valores {

    public static final String CAMPO_GEOMETRIA = "geometriatextual";

    public FilaConsulta {
        atributos = Collections.unmodifiableMap(new LinkedHashMap<>(atributos));
    }

    /**
     * @return geometria del campo {@value #CAMPO_GEOMETRIA}, en CRTM05
     */
    public Optional<Geometria> geometria() {
        return texto(CAMPO_GEOMETRIA).map(Wkt::leer);
    }
}
