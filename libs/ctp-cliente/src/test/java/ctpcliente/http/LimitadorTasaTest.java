package ctpcliente.http;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitadorTasaTest {

    @Test
    void espaciaLasPeticiones() throws InterruptedException {
        LimitadorTasa limitador = new LimitadorTasa(Duration.ofMillis(40));
        long inicio = System.nanoTime();
        for (int i = 0; i < 4; i++) {
            limitador.esperarTurno();
        }
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        assertTrue(ms >= 110, () -> "transcurrieron " + ms + " ms");
    }

    @Test
    void intervaloCeroNoEspera() throws InterruptedException {
        LimitadorTasa limitador = new LimitadorTasa(Duration.ZERO);
        long inicio = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            limitador.esperarTurno();
        }

        assertTrue((System.nanoTime() - inicio) / 1_000_000 < 50);
    }

    @Test
    void intervaloNegativoEsInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new LimitadorTasa(Duration.ofMillis(-1)));
    }
}
