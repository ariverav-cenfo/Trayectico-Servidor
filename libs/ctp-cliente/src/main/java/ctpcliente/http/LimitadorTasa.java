package ctpcliente.http;

import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Garantiza un intervalo minimo entre el inicio de dos peticiones consecutivas.
 */
public final class LimitadorTasa {

    private final long intervaloNanos;
    private final ReentrantLock candado = new ReentrantLock();
    private long proximoPermitido;

    public LimitadorTasa(Duration intervaloMinimo) {
        if (intervaloMinimo.isNegative()) {
            throw new IllegalArgumentException("intervaloMinimo negativo");
        }
        this.intervaloNanos = intervaloMinimo.toNanos();
        this.proximoPermitido = System.nanoTime();
    }

    /**
     * Bloquea hasta que se permita la siguiente peticion.
     *
     * @throws InterruptedException si el hilo se interrumpe mientras espera
     */
    public void esperarTurno() throws InterruptedException {
        if (intervaloNanos == 0) {
            return;
        }
        long espera;
        candado.lock();
        try {
            long ahora = System.nanoTime();
            espera = Math.max(0, proximoPermitido - ahora);
            proximoPermitido = Math.max(ahora, proximoPermitido) + intervaloNanos;
        } finally {
            candado.unlock();
        }
        if (espera > 0) {
            Thread.sleep(Duration.ofNanos(espera));
        }
    }
}
