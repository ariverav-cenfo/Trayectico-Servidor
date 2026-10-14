package ctpcliente.core;

import ctpcliente.http.Parametros;
import ctpcliente.model.Envolvente;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Peticion WMS 1.1.1 inmutable. Con {@code EPSG:4326} la envolvente va en orden longitud, latitud.
 */
public final class ConsultaWms {

    private static final Pattern NOMBRE_CAPA = Pattern.compile("[A-Za-z0-9_]+");
    private static final Pattern VALOR_VISTA = Pattern.compile("[A-Za-z0-9._-]+");

    private final List<String> capas;
    private final Envolvente envolvente;
    private final String srs;
    private final int ancho;
    private final int alto;
    private final String formato;
    private final boolean transparente;
    private final Map<String, String> vista;

    private ConsultaWms(Constructor c) {
        if (c.capas.isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos una capa");
        }
        this.capas = List.copyOf(c.capas);
        this.envolvente = Objects.requireNonNull(c.envolvente, "envolvente");
        this.srs = c.srs;
        this.ancho = c.ancho;
        this.alto = c.alto;
        this.formato = c.formato;
        this.transparente = c.transparente;
        this.vista = Map.copyOf(c.vista);
    }

    public static Constructor de(String... capas) {
        Constructor c = new Constructor();
        for (String capa : capas) {
            c.capa(capa);
        }
        return c;
    }

    public static Constructor de(CapaWfs... capas) {
        Constructor c = new Constructor();
        for (CapaWfs capa : capas) {
            c.capa(capa.nombre());
        }
        return c;
    }

    public int ancho() {
        return ancho;
    }

    public int alto() {
        return alto;
    }

    Parametros parametros() {
        String nombres = capas.stream().map(n -> CapaWfs.WORKSPACE + ":" + n).collect(Collectors.joining(","));
        Parametros p = Parametros.nuevos()
                .con("layers", nombres)
                .con("styles", "")
                .con("srs", srs)
                .con("bbox", envolvente.comoTexto())
                .con("width", ancho)
                .con("height", alto)
                .con("format", formato)
                .con("transparent", transparente);
        if (!vista.isEmpty()) {
            p.con("VIEWPARAMS", vista.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + ":" + e.getValue())
                    .collect(Collectors.joining(";", "", ";")));
        }
        return p;
    }

    String capasConsultables() {
        return capas.stream().map(n -> CapaWfs.WORKSPACE + ":" + n).collect(Collectors.joining(","));
    }

    /**
     * Constructor de {@link ConsultaWms}.
     */
    public static final class Constructor {
        private final List<String> capas = new ArrayList<>();
        private Envolvente envolvente;
        private String srs = ConsultaWfs.WGS84;
        private int ancho = 800;
        private int alto = 600;
        private String formato = "image/png";
        private boolean transparente = true;
        private final Map<String, String> vista = new LinkedHashMap<>();

        private Constructor() {
        }

        public Constructor capa(String nombre) {
            if (nombre == null || !NOMBRE_CAPA.matcher(nombre).matches()) {
                throw new IllegalArgumentException("Nombre de capa invalido: " + nombre);
            }
            capas.add(nombre);
            return this;
        }

        public Constructor envolvente(Envolvente e, String srsDeLaEnvolvente) {
            this.envolvente = Objects.requireNonNull(e, "envolvente");
            this.srs = Objects.requireNonNull(srsDeLaEnvolvente, "srsDeLaEnvolvente");
            return this;
        }

        public Constructor tamano(int anchoPx, int altoPx) {
            if (anchoPx <= 0 || altoPx <= 0 || anchoPx > 4096 || altoPx > 4096) {
                throw new IllegalArgumentException("Tamano fuera de rango (1-4096): " + anchoPx + "x" + altoPx);
            }
            this.ancho = anchoPx;
            this.alto = altoPx;
            return this;
        }

        public Constructor formato(String mime) {
            this.formato = Objects.requireNonNull(mime, "mime");
            return this;
        }

        public Constructor transparente(boolean v) {
            this.transparente = v;
            return this;
        }

        public Constructor vista(String clave, Object valor) {
            String v = String.valueOf(valor);
            if (!NOMBRE_CAPA.matcher(clave).matches() || !VALOR_VISTA.matcher(v).matches()) {
                throw new IllegalArgumentException("VIEWPARAMS invalido: " + clave + "=" + v);
            }
            vista.put(clave, v);
            return this;
        }

        public ConsultaWms construir() {
            return new ConsultaWms(this);
        }
    }
}
