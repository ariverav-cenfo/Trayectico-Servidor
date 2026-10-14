package ctpcliente.model;

/**
 * Atributos {@code sentido} y {@code sentido_desplazamiento}.
 */
public enum Sentido {
    IDA("1-2"),
    REGRESO("2-1"),
    ANILLO("anillo"),
    ANILLO_HORARIO("anillo horario"),
    ANILLO_ANTIHORARIO("anillo antihorario"),
    DESCONOCIDO;

    private final String[] claves;

    Sentido(String... claves) {
        this.claves = claves.clone();
    }

    /**
     * @param valor codigo numerico o texto, sin distinguir mayusculas ni tildes
     * @return la constante correspondiente, o {@code DESCONOCIDO}
     */
    public static Sentido desde(String valor) {
        return TablaCodigos.buscar(values(), DESCONOCIDO, valor, e -> e.claves);
    }

    public boolean esAnillo() {
        return this == ANILLO || this == ANILLO_HORARIO || this == ANILLO_ANTIHORARIO;
    }
}
