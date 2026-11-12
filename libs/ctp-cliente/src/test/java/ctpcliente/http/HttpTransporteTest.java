package ctpcliente.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ctpcliente.CtpConfig;
import ctpcliente.error.CtpException;
import ctpcliente.error.CtpHttpException;
import ctpcliente.error.CtpRedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpTransporteTest {

    private record Recibida(String metodo, String ruta, String agente, String tipo, String cuerpo) {
    }

    private HttpServer servidor;
    private URI base;
    private final List<Recibida> recibidas = new CopyOnWriteArrayList<>();
    private final AtomicInteger fallosPendientes = new AtomicInteger();
    private HttpTransporte transporte;

    @BeforeEach
    void iniciar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/ok", ex -> responder(ex, 200, "hola"));
        servidor.createContext("/eco", ex -> responder(ex, 200, null));
        servidor.createContext("/no-existe", ex -> responder(ex, 404, "no esta"));
        servidor.createContext("/inestable", ex -> {
            if (fallosPendientes.getAndDecrement() > 0) {
                responder(ex, 503, "ocupado");
            } else {
                responder(ex, 200, "listo");
            }
        });
        servidor.createContext("/grande", ex -> responder(ex, 200, "x".repeat(2048)));
        servidor.start();
        base = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort());
        transporte = nuevoTransporte(2, 1024);
    }

    @AfterEach
    void detener() {
        transporte.close();
        servidor.stop(0);
    }

    private HttpTransporte nuevoTransporte(int reintentos, long tamanoMaximo) throws Exception {
        CtpConfig config = CtpConfig.constructor()
                .agenteUsuario("prueba/1.0")
                .reintentos(reintentos)
                .esperaInicial(Duration.ofMillis(1))
                .intervaloMinimo(Duration.ZERO)
                .tiempoRespuesta(Duration.ofSeconds(5))
                .tamanoMaximo(tamanoMaximo)
                .construir();
        return new HttpTransporte(config, SSLContext.getDefault());
    }

    private void responder(HttpExchange ex, int estado, String cuerpo) throws IOException {
        String recibido;
        try (InputStream in = ex.getRequestBody()) {
            recibido = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        recibidas.add(new Recibida(ex.getRequestMethod(), ex.getRequestURI().getPath(),
                ex.getRequestHeaders().getFirst("User-Agent"), ex.getRequestHeaders().getFirst("Content-Type"), recibido));
        byte[] b = (cuerpo == null ? recibido : cuerpo).getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(estado, b.length == 0 ? -1 : b.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(b);
        }
    }

    @Test
    void getEnviaElAgenteDeUsuario() {
        assertEquals("hola", transporte.get(base.resolve("/ok")));
        assertEquals("prueba/1.0", recibidas.getFirst().agente());
    }

    @Test
    void postEnviaFormularioUtf8() {
        String eco = transporte.post(base.resolve("/eco"), Parametros.nuevos().con("nombre", "San José").con("x", "a&b"));

        assertEquals("nombre=San%20Jos%C3%A9&x=a%26b", eco);
        assertEquals("POST", recibidas.getFirst().metodo());
        assertTrue(recibidas.getFirst().tipo().startsWith("application/x-www-form-urlencoded"));
    }

    @Test
    void reintentaAnte503() {
        fallosPendientes.set(2);

        assertEquals("listo", transporte.get(base.resolve("/inestable")));
        assertEquals(3, recibidas.size());
    }

    @Test
    void agotaLosReintentos() {
        fallosPendientes.set(10);

        CtpHttpException e = assertThrows(CtpHttpException.class, () -> transporte.get(base.resolve("/inestable")));
        assertEquals(503, e.estado());
        assertTrue(e.esReintentable());
        assertEquals(3, recibidas.size());
    }

    @Test
    void noReintentaAnte404() {
        CtpHttpException e = assertThrows(CtpHttpException.class, () -> transporte.get(base.resolve("/no-existe")));

        assertEquals(404, e.estado());
        assertFalse(e.esReintentable());
        assertTrue(e.getMessage().contains("no esta"));
        assertEquals(1, recibidas.size());
    }

    @Test
    void limitaElTamanoEnMemoria() {
        assertThrows(CtpException.class, () -> transporte.get(base.resolve("/grande")));
    }

    @Test
    void abrirNoAplicaElLimiteDeTamano() throws IOException {
        try (InputStream in = transporte.abrir(base.resolve("/grande"))) {
            assertEquals(2048, in.readAllBytes().length);
        }
    }

    @Test
    void descargarEscribeElArchivo(@TempDir Path dir) throws IOException {
        Path destino = transporte.descargar(base.resolve("/grande"), dir.resolve("sub/archivo.txt"));

        assertEquals(2048, Files.size(destino));
        try (var listado = Files.list(destino.getParent())) {
            assertEquals(1, listado.count());
        }
    }

    @Test
    void servidorInalcanzableEsErrorDeRed() throws Exception {
        URI cerrado = base.resolve("/ok");
        servidor.stop(0);
        try (HttpTransporte sinReintentos = nuevoTransporte(0, 1024)) {
            assertThrows(CtpRedException.class, () -> sinReintentos.get(cerrado));
        }
    }

    @Test
    void esperaCreceExponencialmente() throws Exception {
        try (HttpTransporte t = new HttpTransporte(CtpConfig.constructor().esperaInicial(Duration.ofMillis(100))
                .construir(), SSLContext.getDefault())) {
            Duration primera = t.espera(1);
            Duration tercera = t.espera(3);
            assertTrue(primera.toMillis() >= 100 && primera.toMillis() <= 150);
            assertTrue(tercera.toMillis() >= 400 && tercera.toMillis() <= 600);
        }
    }
}
