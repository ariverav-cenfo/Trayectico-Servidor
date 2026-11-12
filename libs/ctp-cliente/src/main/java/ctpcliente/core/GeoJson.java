package ctpcliente.core;

import com.fasterxml.jackson.databind.JsonNode;
import ctpcliente.error.CtpException;
import ctpcliente.model.ColeccionElementos;
import ctpcliente.model.Coordenada;
import ctpcliente.model.Elemento;
import ctpcliente.model.Geometria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

final class GeoJson {

    private GeoJson() {
    }

    static ColeccionElementos coleccion(JsonNode raiz) {
        JsonNode features = raiz.path("features");
        if (!features.isArray()) {
            throw new CtpException("GeoJSON sin 'features'");
        }
        List<Elemento> elementos = new ArrayList<>(features.size());
        for (JsonNode f : features) {
            elementos.add(elemento(f));
        }
        return new ColeccionElementos(elementos, total(raiz));
    }

    static Elemento elemento(JsonNode f) {
        JsonNode geom = f.path("geometry");
        return new Elemento(f.path("id").asText(null), geom.isObject() ? geometria(geom) : null,
                mapa(f.path("properties")));
    }

    static Geometria geometria(JsonNode g) {
        String tipo = g.path("type").asText("");
        JsonNode c = g.path("coordinates");
        return switch (tipo) {
            case "Point" -> new Geometria.Punto(coordenada(c));
            case "MultiPoint" -> new Geometria.MultiPunto(coordenadas(c));
            case "LineString" -> new Geometria.Linea(coordenadas(c));
            case "MultiLineString" -> {
                List<Geometria.Linea> lineas = new ArrayList<>(c.size());
                c.forEach(l -> lineas.add(new Geometria.Linea(coordenadas(l))));
                yield new Geometria.MultiLinea(lineas);
            }
            case "Polygon" -> {
                List<List<Coordenada>> anillos = new ArrayList<>(c.size());
                c.forEach(a -> anillos.add(coordenadas(a)));
                yield new Geometria.Poligono(anillos);
            }
            default -> throw new CtpException("Tipo de geometria GeoJSON no soportado: " + tipo);
        };
    }

    private static OptionalLong total(JsonNode raiz) {
        for (String campo : new String[] {"totalFeatures", "numberMatched"}) {
            JsonNode n = raiz.path(campo);
            if (n.canConvertToLong()) {
                return OptionalLong.of(n.asLong());
            }
        }
        return OptionalLong.empty();
    }

    private static Coordenada coordenada(JsonNode par) {
        return new Coordenada(par.get(0).asDouble(), par.get(1).asDouble());
    }

    private static List<Coordenada> coordenadas(JsonNode arreglo) {
        List<Coordenada> lista = new ArrayList<>(arreglo.size());
        arreglo.forEach(p -> lista.add(coordenada(p)));
        return lista;
    }

    private static Object valor(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isIntegralNumber()) {
            return n.asLong();
        }
        if (n.isNumber()) {
            return n.asDouble();
        }
        if (n.isBoolean()) {
            return n.asBoolean();
        }
        if (n.isTextual()) {
            return n.asText();
        }
        return n.toString();
    }

    static Map<String, Object> mapa(JsonNode objeto) {
        Map<String, Object> m = new LinkedHashMap<>();
        objeto.properties().forEach(e -> m.put(e.getKey(), valor(e.getValue())));
        return m;
    }
}
