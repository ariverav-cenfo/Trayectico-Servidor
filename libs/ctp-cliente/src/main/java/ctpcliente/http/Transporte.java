package ctpcliente.http;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;

/**
 * Operaciones HTTP que usan los servicios.
 */
public interface Transporte extends AutoCloseable {

    /**
     * @return cuerpo de la respuesta a un GET, decodificado como UTF-8
     */
    String get(URI uri);

    /**
     * @return cuerpo de la respuesta a un POST {@code application/x-www-form-urlencoded}
     */
    String post(URI uri, Parametros formulario);

    /**
     * @return bytes de la respuesta a un GET
     */
    byte[] getBytes(URI uri);

    /**
     * Abre la respuesta a un GET como flujo. El llamador debe cerrarlo.
     */
    InputStream abrir(URI uri);

    /**
     * Guarda la respuesta a un GET en {@code destino}.
     *
     * @return {@code destino}
     */
    Path descargar(URI uri, Path destino);

    /**
     * Libera las conexiones. Por defecto no hace nada.
     */
    @Override
    default void close() {
    }
}
