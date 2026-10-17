package ctpcliente.model;

/**
 * Atributo {@code estado} de un recorrido o ramal.
 */
public enum EstadoRecorrido {
    ACTIVO("activo", "activa"),
    INACTIVO("inactivo", "inactiva"),
    NO_AUTORIZADO("no autorizado"),
    DESCONOCIDO;

    private final String[] claves;

    EstadoRecorrido(String... claves) {
        this.claves = claves.clone();
    }

    /**
     * @param valor codigo numerico o texto, sin distinguir mayusculas ni tildes
     * @return la constante correspondiente, o {@code DESCONOCIDO}
     */
    public static EstadoRecorrido desde(String valor) {
        return TablaCodigos.buscar(values(), DESCONOCIDO, valor, e -> e.claves);
    }
}
