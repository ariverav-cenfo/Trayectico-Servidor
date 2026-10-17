package ctpcliente.integracion;

import ctpcliente.CtpCliente;
import ctpcliente.auth.CtpTls;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@PruebaIntegracion
class TlsIT {

    private static final URI VISOR = URI.create("https://visortp.ctp.go.cr/Visor/service/ctp?action=getrutas");
    private static final URI GEO = URI.create(
            "https://mapas.ctp.go.cr/GeoP/geoprox2?s=3YNc808CTP_PUBLICO&service=WFS&version=1.1.0&request=GetCapabilities");

    private static int estado(SSLContext ctx, URI uri) throws IOException, InterruptedException {
        try (HttpClient http = HttpClient.newBuilder().sslContext(ctx).build()) {
            return http.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.discarding()).statusCode();
        }
    }

    @Test
    void elContextoPorDefectoDelJdkRechazaAlVisor() {
        assertThrows(SSLHandshakeException.class, () -> estado(SSLContext.getDefault(), VISOR));
    }

    @Test
    void ctpTlsAceptaAlVisor() throws Exception {
        assertEquals(200, estado(CtpTls.contexto(), VISOR));
    }

    @Test
    void elGeoServerFuncionaConAmbosContextos() throws Exception {
        assertEquals(200, estado(SSLContext.getDefault(), GEO));
        assertEquals(200, estado(CtpTls.contexto(), GEO));
    }

    @Test
    void elClienteUsaCtpTls(CtpCliente ctp) {
        assertFalse(ctp.visor().rutas().isEmpty());
    }
}
