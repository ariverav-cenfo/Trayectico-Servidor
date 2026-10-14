package ctpcliente.util;

import ctpcliente.model.Coordenada;
import ctpcliente.model.Envolvente;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilidadesTest {

    @Test
    void textosNormalizaYComparaSinTildes() {
        assertEquals("SAN JOSÉ - SABANA", Textos.normalizar("  SAN   JOSÉ -  SABANA "));
        assertEquals("", Textos.normalizar(null));
        assertEquals("limon", Textos.clave(" Limón "));
        assertTrue(Textos.equivalentes("Parabús", "PARABUS"));
        assertFalse(Textos.equivalentes("Inicial", "Final"));
    }

    @Test
    void distanciaEntreDosParadasConocidas() {
        Coordenada a = Coordenada.lonLat(-84.07623178, 9.93269259);
        Coordenada b = Coordenada.lonLat(-84.07530086, 9.93264307);

        assertEquals(102, Distancia.metros(a, b), 2);
        assertEquals(0, Distancia.metros(a, a), 1e-9);
    }

    @Test
    void envolventeAlrededorContieneElCirculo() {
        Coordenada centro = Coordenada.lonLat(-84.0762, 9.9327);
        Envolvente e = Distancia.alrededor(centro, 300);

        assertTrue(e.contiene(centro));
        assertEquals(300, Distancia.metros(centro, Coordenada.lonLat(e.maxX(), centro.latitud())), 3);
        assertEquals(300, Distancia.metros(centro, Coordenada.lonLat(centro.longitud(), e.maxY())), 3);
        assertThrows(IllegalArgumentException.class, () -> Distancia.alrededor(centro, 0));
    }

    @Test
    void cacheCalculaUnaVezPorVigencia() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-03T00:00:00Z"), ZoneOffset.UTC);
        CacheTemporal<String, Integer> cache = new CacheTemporal<>(Duration.ofMinutes(5), reloj);
        AtomicInteger calculos = new AtomicInteger();

        assertEquals(1, cache.obtener("k", k -> calculos.incrementAndGet()));
        assertEquals(1, cache.obtener("k", k -> calculos.incrementAndGet()));
        cache.invalidar("k");
        assertEquals(2, cache.obtener("k", k -> calculos.incrementAndGet()));
        assertEquals(1, cache.tamano());
        cache.limpiar();
        assertEquals(0, cache.tamano());
    }

    @Test
    void cacheVenceYRecalcula() {
        Instant[] ahora = {Instant.parse("2026-10-03T00:00:00Z")};
        Clock reloj = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zona) {
                return this;
            }

            @Override
            public Instant instant() {
                return ahora[0];
            }
        };
        CacheTemporal<String, Integer> cache = new CacheTemporal<>(Duration.ofMinutes(5), reloj);
        AtomicInteger calculos = new AtomicInteger();

        cache.obtener("k", k -> calculos.incrementAndGet());
        ahora[0] = ahora[0].plus(Duration.ofMinutes(4));
        cache.obtener("k", k -> calculos.incrementAndGet());
        ahora[0] = ahora[0].plus(Duration.ofMinutes(2));
        cache.obtener("k", k -> calculos.incrementAndGet());

        assertEquals(2, calculos.get());
        assertThrows(IllegalArgumentException.class, () -> new CacheTemporal<>(Duration.ZERO, Clock.systemUTC()));
    }

    @Test
    void cacheEsSeguraConcurrentemente() throws InterruptedException {
        CacheTemporal<String, Integer> cache = new CacheTemporal<>(Duration.ofMinutes(5), Clock.systemUTC());
        AtomicInteger calculos = new AtomicInteger();
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 200; i++) {
                pool.submit(() -> cache.obtener("k", k -> calculos.incrementAndGet()));
            }
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(1, calculos.get());
    }

    @Test
    void envolventeValida() {
        assertThrows(IllegalArgumentException.class, () -> new Envolvente(1, 0, 0, 1));
        assertEquals(new Coordenada(1, 1), new Envolvente(0, 0, 2, 2).centro());
        assertThrows(IllegalArgumentException.class, () -> new Coordenada(Double.NaN, 0));
    }
}
