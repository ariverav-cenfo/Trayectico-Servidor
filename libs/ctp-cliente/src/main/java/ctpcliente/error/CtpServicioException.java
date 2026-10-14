package ctpcliente.error;

import java.io.Serial;

/**
 * Respuesta {@code {"success": false, "message": ...}} del servicio del visor.
 */
public class CtpServicioException extends CtpException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CtpServicioException(String mensaje) {
        super(mensaje);
    }
}
