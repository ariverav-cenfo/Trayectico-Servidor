package ctpcliente.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Cache en memoria con vencimiento por entrada. Es seguro para uso concurrente; cada clave se calcula a
 * lo sumo una vez por periodo de vigencia.
 */
public final class CacheTemporal<K, V> {

    private final ConcurrentMap<K, Entrada<V>> entradas = new ConcurrentHashMap<>();
    private final Duration vigencia;
    private final Clock reloj;

    public CacheTemporal(Duration vigencia, Clock reloj) {
        this.vigencia = Objects.requireNonNull(vigencia, "vigencia");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        if (vigencia.isNegative() || vigencia.isZero()) {
            throw new IllegalArgumentException("vigencia debe ser positiva");
        }
    }

    public V obtener(K clave, Function<? super K, ? extends V> calcular) {
        Instant ahora = reloj.instant();
        Entrada<V> e = entradas.compute(clave, (k, actual) -> {
            if (actual != null && actual.vence().isAfter(ahora)) {
                return actual;
            }
            return new Entrada<>(Objects.requireNonNull(calcular.apply(k), "valor calculado"), ahora.plus(vigencia));
        });
        return e.valor();
    }

    public void invalidar(K clave) {
        entradas.remove(clave);
    }

    public void limpiar() {
        entradas.clear();
    }

    public int tamano() {
        return entradas.size();
    }

    private record Entrada<V>(V valor, Instant vence) {
    }
}
