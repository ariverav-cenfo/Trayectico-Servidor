package ctpcliente.model;

/**
 * Atributo {@code tipo_parada}: posicion de la parada en el recorrido.
 */
public enum TipoParada {
    INICIAL("inicial"),
    INTERMEDIA("intermedia"),
    FINAL("final"),
    DESCONOCIDO;

    private final String[] claves;

    TipoParada(String... claves) {
        this.claves = claves.clone();
    }

    /**
     * @param valor codigo numerico o texto, sin distinguir mayusculas ni tildes
     * @return la constante correspondiente, o {@code DESCONOCIDO}
     */
    public static TipoParada desde(String valor) {
        return TablaCodigos.buscar(values(), DESCONOCIDO, valor, e -> e.claves);
    }
}
