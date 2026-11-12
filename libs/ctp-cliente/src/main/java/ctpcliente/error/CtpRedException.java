package ctpcliente.error;

import java.io.Serial;

/**
 * Error de conexion, TLS o tiempo de espera que persiste tras los reintentos.
 */
public class CtpRedException extends CtpException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CtpRedException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
