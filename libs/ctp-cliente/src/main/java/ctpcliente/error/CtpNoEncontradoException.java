package ctpcliente.error;

import java.io.Serial;

/**
 * Un codigo publico no existe en el catalogo del CTP.
 */
public class CtpNoEncontradoException extends CtpException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CtpNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
