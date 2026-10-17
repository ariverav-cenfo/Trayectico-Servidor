package ctpcliente.soporte;

import ctpcliente.http.Parametros;
import ctpcliente.http.Transporte;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class TransporteSimulado implements Transporte {

    public record Peticion(String metodo, URI uri, Parametros formulario) {

        public String parametro(String nombre) {
            if (formulario != null && formulario.valor(nombre) != null) {
                return formulario.valor(nombre);
            }
            return consulta().get(nombre);
        }

        public Map<String, String> consulta() {
            Map<String, String> m = new LinkedHashMap<>();
            String q = uri.getRawQuery();
            if (q == null) {
                return m;
            }
            for (String par : q.split("&")) {
                int i = par.indexOf('=');
                String k = URLDecoder.decode(i < 0 ? par : par.substring(0, i), StandardCharsets.UTF_8);
                String v = i < 0 ? "" : URLDecoder.decode(par.substring(i + 1), StandardCharsets.UTF_8);
                m.put(k, v);
            }
            return m;
        }
    }

    private record Regla(Predicate<Peticion> condicion, Supplier<byte[]> respuesta) {
    }

    private final List<Regla> reglas = new ArrayList<>();
    private final List<Peticion> peticiones = new CopyOnWriteArrayList<>();

    public Respuesta cuando(Predicate<Peticion> condicion) {
        return new Respuesta(condicion);
    }

    public Respuesta cuandoParametro(String nombre, String valor) {
        return cuando(p -> valor.equals(p.parametro(nombre)));
    }

    public List<Peticion> peticiones() {
        return List.copyOf(peticiones);
    }

    public Peticion ultima() {
        if (peticiones.isEmpty()) {
            throw new AssertionError("No hubo peticiones");
        }
        return peticiones.getLast();
    }

    public final class Respuesta {
        private final Predicate<Peticion> condicion;

        private Respuesta(Predicate<Peticion> condicion) {
            this.condicion = condicion;
        }

        public TransporteSimulado fixture(String ruta) {
            return bytes(() -> Fixtures.bytes(ruta));
        }

        public TransporteSimulado texto(String cuerpo) {
            return bytes(() -> cuerpo.getBytes(StandardCharsets.UTF_8));
        }

        public TransporteSimulado bytes(Supplier<byte[]> cuerpo) {
            reglas.add(new Regla(condicion, cuerpo));
            return TransporteSimulado.this;
        }
    }

    @Override
    public String get(URI uri) {
        return new String(responder(new Peticion("GET", uri, null)), StandardCharsets.UTF_8);
    }

    @Override
    public String post(URI uri, Parametros formulario) {
        return new String(responder(new Peticion("POST", uri, formulario)), StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getBytes(URI uri) {
        return responder(new Peticion("GET", uri, null));
    }

    @Override
    public InputStream abrir(URI uri) {
        return new ByteArrayInputStream(responder(new Peticion("GET", uri, null)));
    }

    @Override
    public Path descargar(URI uri, Path destino) {
        try {
            Files.write(destino, responder(new Peticion("GET", uri, null)));
            return destino;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] responder(Peticion p) {
        peticiones.add(p);
        for (Regla r : reglas.reversed()) {
            if (r.condicion().test(p)) {
                return r.respuesta().get();
            }
        }
        throw new AssertionError("Peticion no prevista: " + p.metodo() + " " + p.uri()
                + (p.formulario() == null ? "" : " " + p.formulario()));
    }
}
