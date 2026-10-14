package ctpcliente.util;

import ctpcliente.model.Coordenada;
import ctpcliente.model.Envolvente;

import java.util.List;

/**
 * Conversion entre CRTM05 (EPSG:5367) y WGS84 (EPSG:4326).
 *
 * <p>CRTM05 es una proyeccion Transversal de Mercator sobre el elipsoide WGS84 con meridiano central
 * -84, factor de escala 0,9999 y falso este 500 000 m. Se usan las series de Snyder (USGS Professional
 * Paper 1395), con error submilimetrico dentro de Costa Rica.
 */
public final class Crtm05 {

    public static final int EPSG = 5367;

    private static final double A = 6_378_137.0;
    private static final double F = 1 / 298.257_223_563;
    private static final double E2 = F * (2 - F);
    private static final double EP2 = E2 / (1 - E2);
    private static final double K0 = 0.9999;
    private static final double LON0 = Math.toRadians(-84.0);
    private static final double FALSO_ESTE = 500_000.0;

    private static final double E4 = E2 * E2;
    private static final double E6 = E4 * E2;
    private static final double M1 = 1 - E2 / 4 - 3 * E4 / 64 - 5 * E6 / 256;
    private static final double M2 = 3 * E2 / 8 + 3 * E4 / 32 + 45 * E6 / 1024;
    private static final double M3 = 15 * E4 / 256 + 45 * E6 / 1024;
    private static final double M4 = 35 * E6 / 3072;
    private static final double E1 = (1 - Math.sqrt(1 - E2)) / (1 + Math.sqrt(1 - E2));

    private Crtm05() {
    }

    /**
     * @param crtm05 este y norte en metros
     * @return longitud y latitud en grados
     */
    public static Coordenada aWgs84(Coordenada crtm05) {
        double m = crtm05.y() / K0;
        double mu = m / (A * M1);
        double phi1 = mu
                + (3 * E1 / 2 - 27 * Math.pow(E1, 3) / 32) * Math.sin(2 * mu)
                + (21 * E1 * E1 / 16 - 55 * Math.pow(E1, 4) / 32) * Math.sin(4 * mu)
                + 151 * Math.pow(E1, 3) / 96 * Math.sin(6 * mu)
                + 1097 * Math.pow(E1, 4) / 512 * Math.sin(8 * mu);

        double sin1 = Math.sin(phi1);
        double cos1 = Math.cos(phi1);
        double tan1 = Math.tan(phi1);
        double c1 = EP2 * cos1 * cos1;
        double t1 = tan1 * tan1;
        double n1 = A / Math.sqrt(1 - E2 * sin1 * sin1);
        double r1 = A * (1 - E2) / Math.pow(1 - E2 * sin1 * sin1, 1.5);
        double d = (crtm05.x() - FALSO_ESTE) / (n1 * K0);

        double lat = phi1 - n1 * tan1 / r1 * (d * d / 2
                - (5 + 3 * t1 + 10 * c1 - 4 * c1 * c1 - 9 * EP2) * Math.pow(d, 4) / 24
                + (61 + 90 * t1 + 298 * c1 + 45 * t1 * t1 - 252 * EP2 - 3 * c1 * c1) * Math.pow(d, 6) / 720);
        double lon = LON0 + (d
                - (1 + 2 * t1 + c1) * Math.pow(d, 3) / 6
                + (5 - 2 * c1 + 28 * t1 - 3 * c1 * c1 + 8 * EP2 + 24 * t1 * t1) * Math.pow(d, 5) / 120) / cos1;

        return Coordenada.lonLat(Math.toDegrees(lon), Math.toDegrees(lat));
    }

    /**
     * @param wgs84 longitud y latitud en grados
     * @return este y norte en metros
     */
    public static Coordenada desdeWgs84(Coordenada wgs84) {
        double phi = Math.toRadians(wgs84.latitud());
        double lambda = Math.toRadians(wgs84.longitud());
        double sin = Math.sin(phi);
        double cos = Math.cos(phi);
        double tan = Math.tan(phi);

        double n = A / Math.sqrt(1 - E2 * sin * sin);
        double t = tan * tan;
        double c = EP2 * cos * cos;
        double a = (lambda - LON0) * cos;
        double m = A * (M1 * phi - M2 * Math.sin(2 * phi) + M3 * Math.sin(4 * phi) - M4 * Math.sin(6 * phi));

        double este = FALSO_ESTE + K0 * n * (a
                + (1 - t + c) * Math.pow(a, 3) / 6
                + (5 - 18 * t + t * t + 72 * c - 58 * EP2) * Math.pow(a, 5) / 120);
        double norte = K0 * (m + n * tan * (a * a / 2
                + (5 - t + 9 * c + 4 * c * c) * Math.pow(a, 4) / 24
                + (61 - 58 * t + t * t + 600 * c - 330 * EP2) * Math.pow(a, 6) / 720));

        return new Coordenada(este, norte);
    }

    /**
     * @param crtm05 envolvente en metros
     * @return envolvente en grados que contiene a las cuatro esquinas convertidas
     */
    public static Envolvente aWgs84(Envolvente crtm05) {
        return Envolvente.de(List.of(
                aWgs84(new Coordenada(crtm05.minX(), crtm05.minY())),
                aWgs84(new Coordenada(crtm05.minX(), crtm05.maxY())),
                aWgs84(new Coordenada(crtm05.maxX(), crtm05.minY())),
                aWgs84(new Coordenada(crtm05.maxX(), crtm05.maxY()))));
    }

    /**
     * @param wgs84 envolvente en grados
     * @return envolvente en metros que contiene a las cuatro esquinas convertidas
     */
    public static Envolvente desdeWgs84(Envolvente wgs84) {
        return Envolvente.de(List.of(
                desdeWgs84(Coordenada.lonLat(wgs84.minX(), wgs84.minY())),
                desdeWgs84(Coordenada.lonLat(wgs84.minX(), wgs84.maxY())),
                desdeWgs84(Coordenada.lonLat(wgs84.maxX(), wgs84.minY())),
                desdeWgs84(Coordenada.lonLat(wgs84.maxX(), wgs84.maxY()))));
    }
}
