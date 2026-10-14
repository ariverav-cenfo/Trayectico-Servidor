package ctpcliente.util;

import ctpcliente.model.Coordenada;
import ctpcliente.model.Envolvente;

/**
 * Distancias y areas de busqueda sobre coordenadas WGS84.
 */
public final class Distancia {

    public static final double RADIO_TIERRA_M = 6_371_008.8;
    private static final double METROS_POR_GRADO_LAT = 111_320.0;

    private Distancia() {
    }

    /**
     * @return distancia en metros por la formula del haversine
     */
    public static double metros(Coordenada a, Coordenada b) {
        double lat1 = Math.toRadians(a.latitud());
        double lat2 = Math.toRadians(b.latitud());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitud() - a.longitud());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RADIO_TIERRA_M * Math.asin(Math.min(1, Math.sqrt(h)));
    }

    /**
     * @return envolvente en grados que contiene el circulo de {@code radioMetros} alrededor de {@code centro}
     */
    public static Envolvente alrededor(Coordenada centro, double radioMetros) {
        if (radioMetros <= 0) {
            throw new IllegalArgumentException("radioMetros <= 0");
        }
        double dLat = radioMetros / METROS_POR_GRADO_LAT;
        double dLon = radioMetros / (METROS_POR_GRADO_LAT * Math.cos(Math.toRadians(centro.latitud())));
        return new Envolvente(centro.longitud() - dLon, centro.latitud() - dLat,
                centro.longitud() + dLon, centro.latitud() + dLat);
    }
}
