package ctpcliente.model;

/**
 * Atributo {@code estado} de una parada.
 */
public enum EstadoParada {
    ACTIVA("1", "activa"),
    INACTIVA("2", "inactiva"),
    DESCONOCIDO;

    private final String[] claves;

    EstadoParada(String... claves) {
        this.claves = claves.clone();
    }

    /**
     * @param valor codigo numerico o texto, sin distinguir mayusculas ni tildes
     * @return la constante correspondiente, o {@code DESCONOCIDO}
     */
    public static EstadoParada desde(String valor) {
        return TablaCodigos.buscar(values(), DESCONOCIDO, valor, e -> e.claves);
    }
}
