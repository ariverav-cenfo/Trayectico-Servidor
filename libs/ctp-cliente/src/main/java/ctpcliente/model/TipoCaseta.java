package ctpcliente.model;

/**
 * Atributo {@code tipo_caseta}: refugio de la parada.
 */
public enum TipoCaseta {
    NO_EXISTE("1", "no existe"),
    ARTESANAL("2", "artesanal"),
    PARABUS("3", "parabus"),
    DESCONOCIDO;

    private final String[] claves;

    TipoCaseta(String... claves) {
        this.claves = claves.clone();
    }

    /**
     * @param valor codigo numerico o texto, sin distinguir mayusculas ni tildes
     * @return la constante correspondiente, o {@code DESCONOCIDO}
     */
    public static TipoCaseta desde(String valor) {
        return TablaCodigos.buscar(values(), DESCONOCIDO, valor, e -> e.claves);
    }
}
