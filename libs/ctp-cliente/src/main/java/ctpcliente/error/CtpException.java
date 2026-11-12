package ctpcliente.error;

import java.io.Serial;

/**
 * Base de los errores del cliente.
 */
public class CtpException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CtpException(String mensaje) {
        super(mensaje);
    }

    public CtpException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
