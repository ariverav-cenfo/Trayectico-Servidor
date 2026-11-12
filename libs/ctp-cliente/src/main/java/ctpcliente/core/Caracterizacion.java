package ctpcliente.core;

import java.util.EnumSet;
import java.util.Set;

/**
 * Consultas disponibles en {@code queryTablaDatos}, con sus filtros obligatorios y permitidos.
 */
public enum Caracterizacion {
    PARADAS_DE_RECORRIDO("paradas_desplazamiento_ruta_ramal",
            EnumSet.of(TipoFiltro.RUTA), EnumSet.of(TipoFiltro.RUTA, TipoFiltro.RAMAL, TipoFiltro.DESPLAZAMIENTO)),
    RECORRIDOS_DE_RUTA("ramales_desplazamientos_paradas_fraccionamientos_de_ruta_desplazamientos",
            EnumSet.of(TipoFiltro.RUTA), EnumSet.of(TipoFiltro.RUTA)),
    FRACCIONAMIENTOS_DE_RUTA("ramales_desplazamientos_paradas_fraccionamientos_de_ruta_fraccionamientos",
            EnumSet.of(TipoFiltro.RUTA), EnumSet.of(TipoFiltro.RUTA)),
    PARADAS_DE_FRACCIONAMIENTOS("fraccionamientos_de_una_ruta",
            EnumSet.of(TipoFiltro.RUTA), EnumSet.of(TipoFiltro.RUTA)),
    RUTAS_POR_PARADA("rutas_parada", EnumSet.of(TipoFiltro.PARADA), EnumSet.of(TipoFiltro.PARADA)),
    RAMALES_POR_PARADA("ramales_parada", EnumSet.of(TipoFiltro.PARADA), EnumSet.of(TipoFiltro.PARADA)),
    RECORRIDOS_POR_PARADA("desplazamientos_parada", EnumSet.of(TipoFiltro.PARADA), EnumSet.of(TipoFiltro.PARADA)),
    PARADAS_POR_UBICACION("paradas_ubicacion",
            EnumSet.of(TipoFiltro.PROVINCIA, TipoFiltro.CANTON),
            EnumSet.of(TipoFiltro.PROVINCIA, TipoFiltro.CANTON, TipoFiltro.DISTRITO, TipoFiltro.SENTIDO)),
    TERMINALES_POR_UBICACION("paradas_terminales_ubicacion",
            EnumSet.of(TipoFiltro.PROVINCIA, TipoFiltro.CANTON),
            EnumSet.of(TipoFiltro.PROVINCIA, TipoFiltro.CANTON, TipoFiltro.DISTRITO)),
    RUTAS_POR_CARRETERA("rutas_carretera", EnumSet.of(TipoFiltro.RUAR_ID), EnumSet.of(TipoFiltro.RUAR_ID)),
    RAMALES_POR_CARRETERA("ramales_carretera", EnumSet.of(TipoFiltro.RUAR_ID), EnumSet.of(TipoFiltro.RUAR_ID)),
    RECORRIDOS_POR_CARRETERA("rutas_ramales_desplazamientos_carretera",
            EnumSet.of(TipoFiltro.RUAR_NUMERO), EnumSet.of(TipoFiltro.RUAR_NUMERO));

    private final String clave;
    private final Set<TipoFiltro> obligatorios;
    private final Set<TipoFiltro> permitidos;

    Caracterizacion(String clave, Set<TipoFiltro> obligatorios, Set<TipoFiltro> permitidos) {
        this.clave = clave;
        this.obligatorios = Set.copyOf(obligatorios);
        this.permitidos = Set.copyOf(permitidos);
    }

    /**
     * @return valor del parametro {@code caracterizacion}
     */
    public String clave() {
        return clave;
    }

    public Set<TipoFiltro> obligatorios() {
        return obligatorios;
    }

    public Set<TipoFiltro> permitidos() {
        return permitidos;
    }
}
