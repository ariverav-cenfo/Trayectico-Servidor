package ctpcliente.core;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import ctpcliente.auth.CtpCredenciales;
import ctpcliente.error.CtpException;
import ctpcliente.error.CtpRespuestaVaciaException;
import ctpcliente.http.Parametros;
import ctpcliente.http.Transporte;
import ctpcliente.model.ColeccionElementos;
import ctpcliente.model.Elemento;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Servicio C: WFS 1.1.0 del GeoServer del CTP.
 */
public final class WfsService {

    private static final String VERSION = "1.1.0";
    private static final String GET_FEATURE = "GetFeature";
    private static final String FORMATO_SALIDA = "outputFormat";
    private static final Set<String> PARAMETROS_DE_PAGINA = Set.of("maxFeatures", "startIndex", "sortBy", "propertyName");
    private static final int MARCA = 64 * 1024;
    private static final Pattern NUMERO_ELEMENTOS = Pattern.compile("numberOfFeatures=\"(\\d+)\"");
    private static final Pattern NOMBRE_CAPA = Pattern.compile("<Name>" + CapaWfs.WORKSPACE + ":([^<]+)</Name>");
    private static final Pattern ETIQUETA_ELEMENTO = Pattern.compile("<xsd:element\\s([^>]*)>");
    private static final Pattern ATRIBUTO_NOMBRE = Pattern.compile("\\bname=\"([^\"]*)\"");
    private static final Pattern ATRIBUTO_TIPO = Pattern.compile("\\btype=\"([^\"]*)\"");

    private final Transporte transporte;
    private final URI base;
    private final CtpCredenciales credenciales;

    public WfsService(Transporte transporte, URI baseGeo, CtpCredenciales credenciales) {
        this.transporte = transporte;
        this.base = baseGeo;
        this.credenciales = credenciales;
    }

    /**
     * Carga la respuesta completa en memoria. Para capas grandes use {@link #recorrer} o {@link #paginar}.
     */
    public ColeccionElementos obtener(ConsultaWfs consulta) {
        URI uri = uri(GET_FEATURE, consulta.parametros().con(FORMATO_SALIDA, FormatoWfs.GEOJSON.valor()));
        return GeoJson.coleccion(Respuestas.json(transporte.get(uri), uri));
    }

    /**
     * @return elementos que cumplen el filtro, sin descargarlos; ignora maximo e inicio
     */
    public long contar(ConsultaWfs consulta) {
        Parametros p = Parametros.nuevos();
        consulta.parametros().pares().stream()
                .filter(e -> !PARAMETROS_DE_PAGINA.contains(e.getKey()))
                .forEach(e -> p.con(e.getKey(), e.getValue()));
        URI uri = uri(GET_FEATURE, p.con("resultType", "hits"));
        String cuerpo = transporte.get(uri);
        Respuestas.exigirSinExcepcionOws(cuerpo);
        Matcher m = NUMERO_ELEMENTOS.matcher(cuerpo);
        if (!m.find()) {
            throw new CtpException("Respuesta sin numberOfFeatures desde " + uri);
        }
        return Long.parseLong(m.group(1));
    }

    /**
     * Procesa los elementos uno a uno sin cargar la respuesta completa en memoria.
     *
     * @return cantidad de elementos procesados
     */
    public long recorrer(ConsultaWfs consulta, Consumer<Elemento> accion) {
        URI uri = uri(GET_FEATURE, consulta.parametros().con(FORMATO_SALIDA, FormatoWfs.GEOJSON.valor()));
        try (InputStream crudo = transporte.abrir(uri); BufferedInputStream in = new BufferedInputStream(crudo)) {
            in.mark(MARCA);
            int primero = primerCaracter(in);
            in.reset();
            if (primero == -1) {
                throw new CtpRespuestaVaciaException(uri);
            }
            if (primero == '<') {
                String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                throw Respuestas.errorXml(xml, uri);
            }
            return leerFlujo(in, accion);
        } catch (IOException e) {
            throw new CtpException("Error leyendo GeoJSON de " + uri, e);
        }
    }

    /**
     * Recorre la capa en paginas consecutivas. Requiere {@code ordenarPor} para que el orden sea estable.
     */
    public Stream<Elemento> paginar(ConsultaWfs consulta, int tamanoPagina) {
        if (consulta.ordenCampo().isEmpty()) {
            throw new IllegalArgumentException("paginar requiere ordenarPor(...)");
        }
        if (tamanoPagina <= 0) {
            throw new IllegalArgumentException("tamanoPagina <= 0");
        }
        Iterator<Elemento> it = new Iterator<>() {
            private int siguienteInicio = consulta.inicio().orElse(0);
            private Iterator<Elemento> pagina = List.<Elemento>of().iterator();
            private boolean agotado;

            @Override
            public boolean hasNext() {
                while (!pagina.hasNext() && !agotado) {
                    List<Elemento> lote = obtener(consulta.pagina(siguienteInicio, tamanoPagina)).elementos();
                    siguienteInicio += lote.size();
                    agotado = lote.size() < tamanoPagina;
                    pagina = lote.iterator();
                }
                return pagina.hasNext();
            }

            @Override
            public Elemento next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return pagina.next();
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED | Spliterator.NONNULL),
                false);
    }

    public Path descargar(ConsultaWfs consulta, FormatoWfs formato, Path destino) {
        return transporte.descargar(uri(GET_FEATURE, consulta.parametros().con(FORMATO_SALIDA, formato.valor())), destino);
    }

    /**
     * @return nombres de las capas publicadas, sin el prefijo del workspace
     */
    public List<String> capasPublicadas() {
        URI uri = uri("GetCapabilities", Parametros.nuevos());
        String xml = transporte.get(uri);
        Respuestas.exigirSinExcepcionOws(xml);
        List<String> capas = new ArrayList<>();
        Matcher m = NOMBRE_CAPA.matcher(xml);
        while (m.find()) {
            capas.add(m.group(1));
        }
        return List.copyOf(capas);
    }

    /**
     * @return campos de la capa y su tipo XSD, en el orden publicado
     */
    public Map<String, String> esquema(CapaWfs capa) {
        URI uri = uri("DescribeFeatureType", Parametros.nuevos().con("typeName", capa.nombreCalificado()));
        String xml = transporte.get(uri);
        Respuestas.exigirSinExcepcionOws(xml);
        Map<String, String> campos = new LinkedHashMap<>();
        Matcher etiqueta = ETIQUETA_ELEMENTO.matcher(xml);
        while (etiqueta.find()) {
            Matcher nombre = ATRIBUTO_NOMBRE.matcher(etiqueta.group(1));
            Matcher tipo = ATRIBUTO_TIPO.matcher(etiqueta.group(1));
            if (nombre.find() && tipo.find() && !nombre.group(1).equals(capa.nombre())) {
                campos.put(nombre.group(1), sinPrefijo(tipo.group(1)));
            }
        }
        return campos;
    }

    URI uri(String operacion, Parametros propios) {
        Parametros p = Parametros.nuevos()
                .con("s", credenciales.tokenWfs())
                .con("service", "WFS")
                .con("version", VERSION)
                .con("request", operacion)
                .con(propios);
        return p.aplicarA(base);
    }

    private static String sinPrefijo(String tipoXsd) {
        int dosPuntos = tipoXsd.indexOf(':');
        return dosPuntos < 0 ? tipoXsd : tipoXsd.substring(dosPuntos + 1);
    }

    private static int primerCaracter(InputStream in) throws IOException {
        int c;
        do {
            c = in.read();
        } while (c != -1 && Character.isWhitespace(c));
        return c;
    }

    private static long leerFlujo(InputStream in, Consumer<Elemento> accion) throws IOException {
        long n = 0;
        try (JsonParser p = Respuestas.MAPPER.createParser(in)) {
            if (p.nextToken() != JsonToken.START_OBJECT) {
                throw new CtpException("GeoJSON invalido: no inicia con un objeto");
            }
            while (p.nextToken() == JsonToken.FIELD_NAME) {
                String campo = p.currentName();
                JsonToken valor = p.nextToken();
                if ("features".equals(campo) && valor == JsonToken.START_ARRAY) {
                    while (p.nextToken() == JsonToken.START_OBJECT) {
                        JsonNode f = Respuestas.MAPPER.readTree(p);
                        accion.accept(GeoJson.elemento(f));
                        n++;
                    }
                } else {
                    p.skipChildren();
                }
            }
        }
        return n;
    }
}
