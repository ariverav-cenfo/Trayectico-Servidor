package ctpcliente.core;

import java.util.Optional;
import java.util.Set;

/**
 * Capas del workspace {@code CTP_PUBLICO} que responden correctamente. Las capas {@code paradas} y
 * {@code rutas_transporte} aparecen en GetCapabilities pero fallan en el servidor y no se incluyen.
 */
public enum CapaWfs {
    DESPLAZAMIENTOS("desplazamientos", Campos.GEOM_CRTM05, Campos.ID),
    DESPLAZAMIENTOS_DE_RUTA("desplazamientos_de_ruta", Campos.GEOM, null, Campos.RUTA),
    DESPLAZAMIENTO_TRAMO_CARRETERA("desplazamiento_tramo_carretera", Campos.GEOM_CRTM05, null, "ruar"),
    DESPLAZAMIENTO_USA_PARADA("desplazamiento_usa_parada", Campos.GEOM_CRTM05, null, "parada"),
    DESPLAZAMIENTOS_PERSONALIZADA("personalizada_desplazamientos_de_ruta_ramal_desplazamiento", Campos.GEOM, null,
            Campos.RUTA, "ramal", "desplazamiento"),
    PARADAS_PERSONALIZADA("personalizada_desplazamientos_de_ruta_ramal_parada", Campos.GEOM_CRTM05, Campos.ID,
            Campos.RUTA, "ramal", "desplazamiento", "conparadas"),
    FRACCIONAMIENTOS_DE_RUTA("fraccionamientos_de_ruta", Campos.GEOM, null, Campos.RUTA),
    TRAMO_CARRETERA_NACIONAL("tramo_carretera_nacional_ruar", Campos.GEOM, null),
    PARADA_CONSULTABLE("parada_consultable", Campos.GEOM_CRTM05, Campos.ID),
    PARADAS_X_DESPLAZAMIENTO("paradas_x_desplazamiento", Campos.GEOM_CRTM05, Campos.ID),
    PARADAS_DESPLAZAMIENTO_RUTA_RAMAL("paradas_desplazamiento_ruta_ramal", Campos.GEOM_CRTM05, Campos.ID),
    PARADAS_DE_RUTA("paradas_de_ruta", Campos.GEOM, Campos.ID, Campos.RUTA),
    PARADAS_UBICACION("paradas_ubicacion", Campos.GEOM_CRTM05, Campos.ID, "provincia", "canton", "distrito", "sentido"),
    PARADAS_TERMINALES_UBICACION("paradas_terminales_ubicacion", Campos.GEOM_CRTM05, Campos.ID, "provincia", "canton"),
    PARADAS_INICIO_FRACCIONAMIENTO("paradas_inicial_de_fraccionamientos_de_ruta", Campos.GEOM, Campos.ID, Campos.RUTA),
    PARADAS_FIN_FRACCIONAMIENTO("paradas_final_de_fraccionamientos_de_ruta", Campos.GEOM, Campos.ID, Campos.RUTA);

    public static final String WORKSPACE = "CTP_PUBLICO";

    private final String nombre;
    private final String campoGeometria;
    private final String campoId;
    private final Set<String> parametrosVista;

    CapaWfs(String nombre, String campoGeometria, String campoId, String... parametrosVista) {
        this.nombre = nombre;
        this.campoGeometria = campoGeometria;
        this.campoId = campoId;
        this.parametrosVista = Set.of(parametrosVista);
    }

    public String nombre() {
        return nombre;
    }

    /**
     * @return nombre calificado, p. ej. {@code CTP_PUBLICO:desplazamientos}
     */
    public String nombreCalificado() {
        return WORKSPACE + ":" + nombre;
    }

    /**
     * @return campo de geometria, necesario para {@code BBOX} en CQL
     */
    public String campoGeometria() {
        return campoGeometria;
    }

    /**
     * @return campo con id numerico estable, util para {@code sortBy} al paginar
     */
    public Optional<String> campoId() {
        return Optional.ofNullable(campoId);
    }

    /**
     * @return claves aceptadas en {@code VIEWPARAMS}
     */
    public Set<String> parametrosVista() {
        return parametrosVista;
    }

    private static final class Campos {
        static final String GEOM = "geometria";
        static final String GEOM_CRTM05 = "geometria_crtm05";
        static final String ID = "id";
        static final String RUTA = "ruta";
    }
}
