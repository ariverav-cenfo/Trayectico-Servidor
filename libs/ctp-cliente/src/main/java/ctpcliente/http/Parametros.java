package ctpcliente.http;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Lista ordenada de pares clave/valor para cadenas de consulta y formularios.
 */
public final class Parametros {

    private final List<Map.Entry<String, String>> pares = new ArrayList<>();

    public static Parametros nuevos() {
        return new Parametros();
    }

    public Parametros con(String clave, Object valor) {
        Objects.requireNonNull(clave, "clave");
        if (valor != null) {
            pares.add(Map.entry(clave, valor.toString()));
        }
        return this;
    }

    public Parametros con(Parametros otros) {
        pares.addAll(otros.pares);
        return this;
    }

    public List<Map.Entry<String, String>> pares() {
        return Collections.unmodifiableList(pares);
    }

    public String valor(String clave) {
        return pares.stream()
                .filter(p -> p.getKey().equals(clave))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * @return pares codificados en UTF-8 y unidos con {@code &}
     */
    public String codificar() {
        return pares.stream()
                .map(p -> codificar(p.getKey()) + "=" + codificar(p.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * Agrega los parametros a {@code base}, respetando una cadena de consulta existente.
     */
    public URI aplicarA(URI base) {
        if (pares.isEmpty()) {
            return base;
        }
        String texto = base.toString();
        String separador = texto.contains("?") ? "&" : "?";
        return URI.create(texto + separador + codificar());
    }

    private static String codificar(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @Override
    public String toString() {
        return codificar();
    }
}
