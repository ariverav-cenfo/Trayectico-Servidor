package ctpcliente.core;

/**
 * Filtros de {@code queryTablaDatos}. {@link #RUAR_ID} y {@link #RUAR_NUMERO} se envian ambos como
 * {@code ruar}, pero cada caracterizacion espera solo uno de ellos.
 */
public enum TipoFiltro {
    RUTA("ruta"),
    RAMAL("ramal"),
    DESPLAZAMIENTO("desplazamiento"),
    PARADA("parada"),
    PROVINCIA("provincia"),
    CANTON("canton"),
    DISTRITO("distrito"),
    SENTIDO("sentido"),
    RUAR_ID("ruar"),
    RUAR_NUMERO("ruar");

    private final String nombreApi;

    TipoFiltro(String nombreApi) {
        this.nombreApi = nombreApi;
    }

    public String nombreApi() {
        return nombreApi;
    }
}
