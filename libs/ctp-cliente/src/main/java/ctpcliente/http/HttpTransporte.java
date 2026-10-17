package ctpcliente.http;

import ctpcliente.CtpConfig;
import ctpcliente.error.CtpException;
import ctpcliente.error.CtpHttpException;
import ctpcliente.error.CtpRedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import javax.net.ssl.SSLContext;

/**
 * {@link Transporte} sobre {@link HttpClient} con reintentos, espera exponencial, limite de tasa y
 * limite de tamano. Es seguro para uso concurrente.
 */
public final class HttpTransporte implements Transporte {

    private static final System.Logger LOG = System.getLogger(HttpTransporte.class.getName());
    private static final int EXTRACTO_ERROR = 300;
    private static final int ESTADO_ERROR = 400;

    private final CtpConfig config;
    private final HttpClient http;
    private final LimitadorTasa limitador;

    public HttpTransporte(CtpConfig config, SSLContext tls) {
        this.config = config;
        this.http = HttpClient.newBuilder()
                .sslContext(tls)
                .connectTimeout(config.tiempoConexion())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.limitador = new LimitadorTasa(config.intervaloMinimo());
    }

    @Override
    public String get(URI uri) {
        return new String(getBytes(uri), StandardCharsets.UTF_8);
    }

    @Override
    public String post(URI uri, Parametros formulario) {
        HttpRequest req = solicitud(uri)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(formulario.codificar(), StandardCharsets.UTF_8))
                .build();
        try (InputStream in = enviar(req)) {
            return new String(leerLimitado(in, uri), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CtpRedException("Error leyendo " + uri, e);
        }
    }

    @Override
    public byte[] getBytes(URI uri) {
        try (InputStream in = enviar(solicitud(uri).GET().build())) {
            return leerLimitado(in, uri);
        } catch (IOException e) {
            throw new CtpRedException("Error leyendo " + uri, e);
        }
    }

    @Override
    public InputStream abrir(URI uri) {
        return enviar(solicitud(uri).GET().build());
    }

    @Override
    public Path descargar(URI uri, Path destino) {
        Path temporal = null;
        Path carpeta = destino.toAbsolutePath().getParent();
        if (carpeta == null) {
            throw new IllegalArgumentException("El destino debe estar dentro de una carpeta: " + destino);
        }
        try (InputStream in = abrir(uri)) {
            Files.createDirectories(carpeta);
            temporal = Files.createTempFile(carpeta, ".descarga", ".tmp");
            Files.copy(in, temporal, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return destino;
        } catch (IOException e) {
            CtpRedException error = new CtpRedException("Error descargando " + uri + " en " + destino, e);
            borrar(temporal, error);
            throw error;
        }
    }

    @Override
    public void close() {
        http.close();
    }

    private HttpRequest.Builder solicitud(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(config.tiempoRespuesta())
                .header("User-Agent", config.agenteUsuario())
                .header("Accept", "application/json, application/xml;q=0.9, */*;q=0.8");
    }

    private InputStream enviar(HttpRequest req) {
        int intentos = config.reintentos() + 1;
        for (int intento = 1; ; intento++) {
            try {
                return enviarUnaVez(req);
            } catch (CtpHttpException e) {
                if (!e.esReintentable() || intento >= intentos) {
                    throw e;
                }
                registrarReintento(intento, e.getMessage());
            } catch (IOException e) {
                if (intento >= intentos) {
                    throw new CtpRedException("Fallo de red en " + req.uri() + " tras " + intento + " intentos", e);
                }
                registrarReintento(intento, e.toString());
            }
            dormir(espera(intento));
        }
    }

    private InputStream enviarUnaVez(HttpRequest req) throws IOException {
        try {
            limitador.esperarTurno();
            long inicio = System.nanoTime();
            HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            LOG.log(System.Logger.Level.DEBUG, () -> "%s %s -> %d (%d ms)".formatted(
                    req.method(), req.uri(), resp.statusCode(), (System.nanoTime() - inicio) / 1_000_000));
            if (resp.statusCode() >= ESTADO_ERROR) {
                throw new CtpHttpException(resp.statusCode(), req.uri(), extracto(resp.body()));
            }
            return resp.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CtpRedException("Peticion interrumpida: " + req.uri(), e);
        }
    }

    private void registrarReintento(int intento, String motivo) {
        LOG.log(System.Logger.Level.INFO, "Reintento {0}/{1}: {2}", intento, config.reintentos(), motivo);
    }

    Duration espera(int intento) {
        long base = config.esperaInicial().toMillis() << Math.min(intento - 1, 10);
        long jitter = base == 0 ? 0 : ThreadLocalRandom.current().nextLong(base / 2 + 1);
        return Duration.ofMillis(base + jitter);
    }

    private byte[] leerLimitado(InputStream in, URI uri) throws IOException {
        long maximo = config.tamanoMaximo();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        int n = in.read(buffer);
        while (n != -1) {
            total += n;
            if (total > maximo) {
                throw new CtpException("La respuesta de " + uri + " supera " + maximo
                        + " bytes; use WfsService.recorrer o descargar");
            }
            out.write(buffer, 0, n);
            n = in.read(buffer);
        }
        return out.toByteArray();
    }

    private static String extracto(InputStream in) {
        try (in) {
            String s = new String(in.readNBytes(EXTRACTO_ERROR), StandardCharsets.UTF_8);
            return s.replaceAll("\\s+", " ").strip();
        } catch (IOException e) {
            return "";
        }
    }

    private static void dormir(Duration d) {
        try {
            Thread.sleep(d);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CtpRedException("Espera interrumpida", e);
        }
    }

    private static void borrar(Path p, Exception principal) {
        if (p == null) {
            return;
        }
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            principal.addSuppressed(e);
        }
    }
}
