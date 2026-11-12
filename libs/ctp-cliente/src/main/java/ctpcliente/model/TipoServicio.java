package ctpcliente.model;

/**
 * Atributo {@code tipo_servicio}. Significado inferido de las descripciones; el CTP no lo publica.
 */
public enum TipoServicio {
    DIRECTO("1"),
    REGULAR("2"),
    EXPRESO("3"),
    DESCONOCIDO;

    private final String[] claves;

    TipoServicio(String... claves) {
        this.claves = claves.clone();
    }

    /**
     * @param valor codigo numerico o texto, sin distinguir mayusculas ni tildes
     * @return la constante correspondiente, o {@code DESCONOCIDO}
     */
    public static TipoServicio desde(String valor) {
        return TablaCodigos.buscar(values(), DESCONOCIDO, valor, e -> e.claves);
    }
}
