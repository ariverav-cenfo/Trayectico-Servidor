package ctpcliente.model;

/**
 * Par de coordenadas. En WGS84 {@code x} es la longitud y {@code y} la latitud; en CRTM05 {@code x} es
 * el este y {@code y} el norte, en metros.
 */
public record Coordenada(double x, double y) {

    public Coordenada {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Coordenada no finita: " + x + ", " + y);
        }
    }

    public static Coordenada lonLat(double longitud, double latitud) {
        return new Coordenada(longitud, latitud);
    }

    public double longitud() {
        return x;
    }

    public double latitud() {
        return y;
    }
}
