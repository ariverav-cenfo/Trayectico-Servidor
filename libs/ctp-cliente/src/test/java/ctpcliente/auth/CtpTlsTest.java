package ctpcliente.auth;

import org.junit.jupiter.api.Test;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.SSLContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CtpTlsTest {

    @Test
    void intermedioEmpaquetadoEsElDeGoDaddyG2() throws Exception {
        X509Certificate cert = CtpTls.intermedioGoDaddy();

        assertTrue(cert.getSubjectX500Principal().getName().contains("Go Daddy Secure Certificate Authority - G2"));
        assertEquals(CtpTls.HUELLA_INTERMEDIO, CtpTls.huellaSha256(cert));
        cert.checkValidity();
    }

    @Test
    void diasParaVencerEsPositivo() {
        assertTrue(CtpTls.diasParaVencer() > 0);
    }

    @Test
    void almacenIncluyeLasAnclasDelJdkMasElIntermedio() throws Exception {
        KeyStore conIntermedio = CtpTls.almacenConfianza(List.of(CtpTls.intermedioGoDaddy()));
        KeyStore soloJdk = CtpTls.almacenConfianza(List.of());

        assertEquals(soloJdk.size() + 1, conIntermedio.size());
        assertTrue(soloJdk.size() > 50);
        boolean presente = Collections.list(conIntermedio.aliases()).stream().anyMatch(a -> {
            try {
                return CtpTls.intermedioGoDaddy().equals(conIntermedio.getCertificate(a));
            } catch (Exception e) {
                return false;
            }
        });
        assertTrue(presente);
    }

    @Test
    void contextoSeConstruye() {
        SSLContext ctx = CtpTls.contexto();

        assertNotNull(ctx.getSocketFactory());
        assertEquals("TLS", ctx.getProtocol());
    }
}
