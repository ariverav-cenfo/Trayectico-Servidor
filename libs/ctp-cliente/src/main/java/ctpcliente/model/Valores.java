package ctpcliente.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Lectura tipada de un mapa de atributos cuyos numeros pueden llegar como numero o como texto.
 */
public sealed interface Valores permits Elemento, FilaConsulta {

    Map<String, Object> atributos();

    default Optional<String> texto(String campo) {
        Object v = atributos().get(campo);
        if (v == null) {
            return Optional.empty();
        }
        String s = v.toString().strip();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    default Optional<Long> entero(String campo) {
        Object v = atributos().get(campo);
        if (v instanceof Number n) {
            return Optional.of(n.longValue());
        }
        return texto(campo).flatMap(Valores::aEntero);
    }

    default Optional<Double> decimal(String campo) {
        Object v = atributos().get(campo);
        if (v instanceof Number n) {
            return Optional.of(n.doubleValue());
        }
        return texto(campo).flatMap(Valores::aDecimal);
    }

    private static Optional<Long> aEntero(String s) {
        try {
            return Optional.of(new BigDecimal(s).longValueExact());
        } catch (NumberFormatException | ArithmeticException e) {
            return Optional.empty();
        }
    }

    private static Optional<Double> aDecimal(String s) {
        try {
            return Optional.of(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
