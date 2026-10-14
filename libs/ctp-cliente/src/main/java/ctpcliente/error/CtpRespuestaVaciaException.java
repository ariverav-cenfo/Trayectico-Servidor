package ctpcliente.error;

import java.io.Serial;
import java.net.URI;

/**
 * Respuesta 200 sin cuerpo: token {@code s} ausente o accion desconocida.
 */
public class CtpRespuestaVaciaException extends CtpException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CtpRespuestaVaciaException(URI uri) {
        super("Respuesta vacia de " + uri);
    }
}
