package ctpcliente.error;

import java.io.Serial;
import java.net.URI;

/**
 * Respuesta con estado HTTP de error.
 */
public class CtpHttpException extends CtpException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int estado;
    private final URI uri;

    public CtpHttpException(int estado, URI uri, String extracto) {
        super("HTTP " + estado + " en " + uri + (extracto.isBlank() ? "" : ": " + extracto));
        this.estado = estado;
        this.uri = uri;
    }

    public int estado() {
        return estado;
    }

    public URI uri() {
        return uri;
    }

    public boolean esReintentable() {
        return estado >= 500 || estado == 429;
    }
}
