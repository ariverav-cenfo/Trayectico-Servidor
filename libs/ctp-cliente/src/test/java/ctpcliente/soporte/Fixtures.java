package ctpcliente.soporte;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public final class Fixtures {

    private Fixtures() {
    }

    public static byte[] bytes(String ruta) {
        try (InputStream in = Fixtures.class.getResourceAsStream("/fixtures/" + ruta)) {
            if (in == null) {
                throw new IllegalArgumentException("Fixture inexistente: " + ruta);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String texto(String ruta) {
        return new String(bytes(ruta), StandardCharsets.UTF_8);
    }
}
