package ctpcliente.util;

import ctpcliente.model.Coordenada;
import ctpcliente.model.Envolvente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Crtm05Test {

    private static final double TOLERANCIA_GRADOS = 2e-6;
    private static final double TOLERANCIA_METROS = 0.01;

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "PBA-10104-054 (San Jose),   491641.116, 1098301.244, -84.07623178, 9.93269259",
            "Meridiano central en el ecuador, 500000.0, 0.0, -84.0, 0.0"
    })
    void coincideConGeoServer(String caso, double este, double norte, double lon, double lat) {
        Coordenada wgs = Crtm05.aWgs84(new Coordenada(este, norte));

        assertEquals(lon, wgs.longitud(), TOLERANCIA_GRADOS, caso);
        assertEquals(lat, wgs.latitud(), TOLERANCIA_GRADOS, caso);
    }

    @ParameterizedTest
    @CsvSource({"-85.6, 10.6", "-82.6, 9.6", "-84.0, 8.0", "-83.0, 11.2", "-84.0762, 9.9327"})
    void idaYVueltaEsExacta(double lon, double lat) {
        Coordenada metros = Crtm05.desdeWgs84(Coordenada.lonLat(lon, lat));
        Coordenada vuelta = Crtm05.aWgs84(metros);
        Coordenada metrosOtraVez = Crtm05.desdeWgs84(vuelta);

        assertEquals(lon, vuelta.longitud(), 1e-9);
        assertEquals(lat, vuelta.latitud(), 1e-9);
        assertEquals(metros.x(), metrosOtraVez.x(), TOLERANCIA_METROS);
        assertEquals(metros.y(), metrosOtraVez.y(), TOLERANCIA_METROS);
    }

    @Test
    void envolventeIdaYVueltaContieneLaOriginal() {
        Envolvente grados = new Envolvente(-84.15, 9.90, -84.03, 9.98);
        Envolvente metros = Crtm05.desdeWgs84(grados);
        Envolvente vuelta = Crtm05.aWgs84(metros);

        assertTrue(metros.minX() > 480_000 && metros.maxX() < 500_000);
        assertTrue(vuelta.minX() <= grados.minX() + 1e-9 && vuelta.maxY() >= grados.maxY() - 1e-9);
        assertEquals(grados.minY(), vuelta.minY(), 1e-3);
    }
}
