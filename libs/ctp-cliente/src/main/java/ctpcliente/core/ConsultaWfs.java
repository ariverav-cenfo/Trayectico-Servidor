package ctpcliente.core;

import ctpcliente.http.Parametros;
import ctpcliente.model.Envolvente;
import ctpcliente.util.Cql;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Peticion GetFeature inmutable. Se construye con {@link #de(CapaWfs)}.
 */
public final class ConsultaWfs {

    public static final String WGS84 = "EPSG:4326";
    public static final String CRTM05 = "EPSG:5367";

    private static final Pattern VALOR_VISTA = Pattern.compile("[A-Za-z0-9._-]+");

    private final CapaWfs capa;
    private final String filtroCql;
    private final Envolvente envolvente;
    private final String srsEnvolvente;
    private final List<String> propiedades;
    private final Integer maximo;
    private final Integer inicio;
    private final String ordenCampo;
    private final boolean ordenDescendente;
    private final Map<String, String> vista;
    private final String srs;

    private ConsultaWfs(Constructor c) {
        this.capa = c.capa;
        this.filtroCql = c.filtroCql;
        this.envolvente = c.envolvente;
        this.srsEnvolvente = c.srsEnvolvente;
        this.propiedades = List.copyOf(c.propiedades);
        this.maximo = c.maximo;
        this.inicio = c.inicio;
        this.ordenCampo = c.ordenCampo;
        this.ordenDescendente = c.ordenDescendente;
        this.vista = Map.copyOf(c.vista);
        this.srs = c.srs;
    }

    public static Constructor de(CapaWfs capa) {
        return new Constructor(capa);
    }

    public CapaWfs capa() {
        return capa;
    }

    public Optional<String> ordenCampo() {
        return Optional.ofNullable(ordenCampo);
    }

    public Optional<Integer> maximo() {
        return Optional.ofNullable(maximo);
    }

    public Optional<Integer> inicio() {
        return Optional.ofNullable(inicio);
    }

    public String srs() {
        return srs;
    }

    /**
     * @return copia con otra pagina
     */
    public ConsultaWfs pagina(int nuevoInicio, int nuevoMaximo) {
        return aConstructor().inicio(nuevoInicio).maximo(nuevoMaximo).construir();
    }

    public Constructor aConstructor() {
        Constructor c = new Constructor(capa);
        c.filtroCql = filtroCql;
        c.envolvente = envolvente;
        c.srsEnvolvente = srsEnvolvente;
        c.propiedades.addAll(propiedades);
        c.maximo = maximo;
        c.inicio = inicio;
        c.ordenCampo = ordenCampo;
        c.ordenDescendente = ordenDescendente;
        c.vista.putAll(vista);
        c.srs = srs;
        return c;
    }

    /**
     * @return parametros propios de la consulta, sin {@code service}, {@code request} ni token
     */
    Parametros parametros() {
        Parametros p = Parametros.nuevos().con("typeName", capa.nombreCalificado()).con("srsName", srs);
        if (!propiedades.isEmpty()) {
            p.con("propertyName", String.join(",", propiedades));
        }
        if (filtroCql != null) {
            p.con("CQL_FILTER", filtroCql);
        }
        if (envolvente != null) {
            p.con("bbox", envolvente.comoTexto() + "," + srsEnvolvente);
        }
        if (maximo != null) {
            p.con("maxFeatures", maximo);
        }
        if (inicio != null) {
            p.con("startIndex", inicio);
        }
        if (ordenCampo != null) {
            p.con("sortBy", ordenCampo + (ordenDescendente ? " D" : " A"));
        }
        if (!vista.isEmpty()) {
            p.con("VIEWPARAMS", vista.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + ":" + e.getValue())
                    .collect(Collectors.joining(";", "", ";")));
        }
        return p;
    }

    /**
     * Constructor de {@link ConsultaWfs}.
     */
    public static final class Constructor {
        private final CapaWfs capa;
        private String filtroCql;
        private Envolvente envolvente;
        private String srsEnvolvente;
        private final List<String> propiedades = new ArrayList<>();
        private Integer maximo;
        private Integer inicio;
        private String ordenCampo;
        private boolean ordenDescendente;
        private final Map<String, String> vista = new LinkedHashMap<>();
        private String srs = WGS84;

        private Constructor(CapaWfs capa) {
            this.capa = Objects.requireNonNull(capa, "capa");
        }

        /**
         * @param cql filtro ECQL; para valores externos use {@link Cql}
         */
        public Constructor filtro(String cql) {
            Objects.requireNonNull(cql, "cql");
            if (cql.isBlank()) {
                throw new IllegalArgumentException("Filtro CQL vacio");
            }
            this.filtroCql = cql;
            return this;
        }

        public Constructor envolvente(Envolvente e, String srsDeLaEnvolvente) {
            this.envolvente = Objects.requireNonNull(e, "envolvente");
            this.srsEnvolvente = Objects.requireNonNull(srsDeLaEnvolvente, "srsDeLaEnvolvente");
            return this;
        }

        public Constructor propiedades(String... campos) {
            for (String c : campos) {
                propiedades.add(Cql.campo(c));
            }
            return this;
        }

        public Constructor conGeometria() {
            if (!propiedades.isEmpty() && !propiedades.contains(capa.campoGeometria())) {
                propiedades.add(capa.campoGeometria());
            }
            return this;
        }

        public Constructor maximo(int n) {
            if (n <= 0) {
                throw new IllegalArgumentException("maximo <= 0");
            }
            this.maximo = n;
            return this;
        }

        public Constructor inicio(int n) {
            if (n < 0) {
                throw new IllegalArgumentException("inicio < 0");
            }
            this.inicio = n;
            return this;
        }

        public Constructor ordenarPor(String campo, boolean descendente) {
            this.ordenCampo = Cql.campo(campo);
            this.ordenDescendente = descendente;
            return this;
        }

        /**
         * @throws IllegalArgumentException si la capa no admite la clave o el valor tiene caracteres no permitidos
         */
        public Constructor vista(String clave, Object valor) {
            if (!capa.parametrosVista().contains(clave)) {
                throw new IllegalArgumentException(capa + " no admite VIEWPARAMS '" + clave + "'; admite "
                        + capa.parametrosVista());
            }
            String v = String.valueOf(valor);
            if (!VALOR_VISTA.matcher(v).matches()) {
                throw new IllegalArgumentException("Valor de VIEWPARAMS invalido: " + v);
            }
            vista.put(clave, v);
            return this;
        }

        public Constructor srs(String srs) {
            this.srs = Objects.requireNonNull(srs, "srs");
            return this;
        }

        public ConsultaWfs construir() {
            return new ConsultaWfs(this);
        }
    }
}
