package ctpcliente.core;

import ctpcliente.auth.CtpCredenciales;
import ctpcliente.error.CtpException;
import ctpcliente.http.Parametros;
import ctpcliente.http.Transporte;
import ctpcliente.model.ColeccionElementos;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Servicio C: WMS 1.1.1 del GeoServer del CTP.
 */
public final class WmsService {

    private static final String VERSION = "1.1.1";
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G'};

    private final Transporte transporte;
    private final URI base;
    private final CtpCredenciales credenciales;

    public WmsService(Transporte transporte, URI baseGeo, CtpCredenciales credenciales) {
        this.transporte = transporte;
        this.base = baseGeo;
        this.credenciales = credenciales;
    }

    /**
     * @return URL de GetMap, apta para una etiqueta {@code img} o una capa de Leaflet
     */
    public URI urlMapa(ConsultaWms consulta) {
        return uri("GetMap", consulta.parametros());
    }

    /**
     * @return imagen en el formato pedido
     * @throws CtpException si el servidor responde con un reporte de error en lugar de una imagen
     */
    public byte[] mapa(ConsultaWms consulta) {
        URI uri = urlMapa(consulta);
        byte[] bytes = transporte.getBytes(uri);
        if (bytes.length == 0) {
            throw new CtpException("Imagen vacia desde " + uri);
        }
        if (bytes[0] == '<') {
            String xml = new String(bytes, StandardCharsets.UTF_8);
            throw Respuestas.errorXml(xml, uri);
        }
        return bytes;
    }

    public static boolean esPng(byte[] bytes) {
        if (bytes.length < FIRMA_PNG.length) {
            return false;
        }
        for (int i = 0; i < FIRMA_PNG.length; i++) {
            if (bytes[i] != FIRMA_PNG[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param x      columna del pixel, desde 0
     * @param y      fila del pixel, desde 0
     * @param maximo elementos maximos a devolver
     * @return elementos bajo el pixel, en EPSG:4326
     */
    public ColeccionElementos elementosEnPixel(ConsultaWms consulta, int x, int y, int maximo) {
        if (x < 0 || y < 0 || x >= consulta.ancho() || y >= consulta.alto()) {
            throw new IllegalArgumentException("Pixel fuera de la imagen: " + x + "," + y);
        }
        Parametros p = consulta.parametros()
                .con("query_layers", consulta.capasConsultables())
                .con("x", x)
                .con("y", y)
                .con("info_format", "application/json")
                .con("feature_count", maximo)
                .con("buffer", 10);
        URI uri = uri("GetFeatureInfo", p);
        return GeoJson.coleccion(Respuestas.json(transporte.get(uri), uri));
    }

    public URI urlLeyenda(CapaWfs capa) {
        return uri("GetLegendGraphic", Parametros.nuevos()
                .con("layer", capa.nombreCalificado())
                .con("format", "image/png")
                .con("width", 20)
                .con("height", 20));
    }

    private URI uri(String operacion, Parametros propios) {
        return Parametros.nuevos()
                .con("s", credenciales.tokenWfs())
                .con("service", "WMS")
                .con("version", VERSION)
                .con("request", operacion)
                .con(propios)
                .aplicarA(base);
    }
}
