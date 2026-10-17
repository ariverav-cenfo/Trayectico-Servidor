package ctpcliente.auth;

import java.util.Objects;

/**
 * Credenciales de acceso a los servicios del CTP.
 *
 * <p>El proxy de GeoServer exige el parametro {@code s}. El valor se toma de la variable de entorno
 * {@value #VARIABLE_ENTORNO} o, si no esta definida, de {@link #TOKEN_PUBLICO}.
 *
 * @param tokenWfs valor del parametro {@code s}
 */
public record CtpCredenciales(String tokenWfs) {

    public static final String TOKEN_PUBLICO = "3YNc808CTP_PUBLICO";

    public static final String VARIABLE_ENTORNO = "CTP_WFS_TOKEN";

    public CtpCredenciales {
        Objects.requireNonNull(tokenWfs, "tokenWfs");
        if (tokenWfs.isBlank()) {
            throw new IllegalArgumentException("tokenWfs vacio");
        }
    }

    public static CtpCredenciales desdeEntorno() {
        return desde(System.getenv(VARIABLE_ENTORNO));
    }

    public static CtpCredenciales publicas() {
        return new CtpCredenciales(TOKEN_PUBLICO);
    }

    static CtpCredenciales desde(String valor) {
        return new CtpCredenciales(valor == null || valor.isBlank() ? TOKEN_PUBLICO : valor.strip());
    }

    @Override
    public String toString() {
        String visible = tokenWfs.length() <= 4 ? "" : tokenWfs.substring(0, 4);
        return "CtpCredenciales[tokenWfs=" + visible + "****]";
    }
}
