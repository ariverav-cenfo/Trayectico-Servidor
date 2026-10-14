package ctpcliente.model;

import ctpcliente.util.Textos;

import java.util.Arrays;
import java.util.function.Function;

final class TablaCodigos {

    private TablaCodigos() {
    }

    static <E extends Enum<E>> E buscar(E[] valores, E porDefecto, String valor, Function<E, String[]> claves) {
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        String buscado = Textos.clave(valor);
        return Arrays.stream(valores)
                .filter(e -> Arrays.stream(claves.apply(e)).anyMatch(c -> Textos.clave(c).equals(buscado)))
                .findFirst()
                .orElse(porDefecto);
    }
}
