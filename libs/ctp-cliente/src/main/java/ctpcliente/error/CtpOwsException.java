package ctpcliente.error;

import java.io.Serial;

/**
 * Reporte {@code ows:ExceptionReport} de GeoServer. El servidor lo envia con estado HTTP 200.
 */
public class CtpOwsException extends CtpException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String codigo;

    public CtpOwsException(String codigo, String texto) {
        super("GeoServer: " + texto + (codigo == null ? "" : " [" + codigo + "]"));
        this.codigo = codigo;
    }

    /**
     * @return el atributo {@code exceptionCode} del reporte, o {@code null}
     */
    public String codigo() {
        return codigo;
    }
}
