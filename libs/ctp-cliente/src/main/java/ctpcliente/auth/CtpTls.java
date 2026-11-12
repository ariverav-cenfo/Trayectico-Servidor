package ctpcliente.auth;

import ctpcliente.error.CtpException;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Contexto TLS para los servidores del CTP.
 *
 * <p>El contexto confia en las autoridades por defecto del JDK y, ademas, en el certificado intermedio
 * "Go Daddy Secure Certificate Authority - G2", que {@code visortp.ctp.go.cr} no envia en el handshake.
 * El certificado empaquetado se valida contra {@link #HUELLA_INTERMEDIO} antes de usarse.
 */
public final class CtpTls {

    public static final String RECURSO_INTERMEDIO = "/ctpcliente/auth/gdig2.crt.pem";

    public static final String HUELLA_INTERMEDIO =
            "973a41276ffd01e027a2aad49e34c37846d3e976ff6a620b6712e33832041aa6";

    private CtpTls() {
    }

    /**
     * @return contexto con las autoridades del JDK y el intermedio de GoDaddy
     */
    public static SSLContext contexto() {
        return contexto(List.of(intermedioGoDaddy()));
    }

    /**
     * @param adicionales certificados que se agregan como anclas de confianza
     * @return contexto con las autoridades del JDK y los certificados adicionales
     */
    public static SSLContext contexto(List<X509Certificate> adicionales) {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(almacenConfianza(adicionales));
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);
            return ctx;
        } catch (GeneralSecurityException e) {
            throw new CtpException("No se pudo construir el contexto TLS", e);
        }
    }

    /**
     * @return el intermedio empaquetado
     * @throws CtpException si el recurso falta o su huella no coincide
     */
    public static X509Certificate intermedioGoDaddy() {
        try (InputStream in = CtpTls.class.getResourceAsStream(RECURSO_INTERMEDIO)) {
            if (in == null) {
                throw new CtpException("Falta el recurso " + RECURSO_INTERMEDIO);
            }
            X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
            if (!HUELLA_INTERMEDIO.equals(huellaSha256(cert))) {
                throw new CtpException("La huella de " + RECURSO_INTERMEDIO + " no coincide");
            }
            return cert;
        } catch (IOException | GeneralSecurityException e) {
            throw new CtpException("No se pudo leer " + RECURSO_INTERMEDIO, e);
        }
    }

    /**
     * @return dias hasta el vencimiento del intermedio; negativo si ya vencio
     */
    public static long diasParaVencer() {
        Instant fin = intermedioGoDaddy().getNotAfter().toInstant();
        return Duration.between(Instant.now(), fin).toDays();
    }

    static String huellaSha256(X509Certificate cert) throws GeneralSecurityException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
    }

    static KeyStore almacenConfianza(List<X509Certificate> adicionales) throws GeneralSecurityException {
        TrustManagerFactory porDefecto = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        porDefecto.init((KeyStore) null);
        List<X509Certificate> anclas = new ArrayList<>();
        for (var tm : porDefecto.getTrustManagers()) {
            if (tm instanceof X509TrustManager x509) {
                anclas.addAll(List.of(x509.getAcceptedIssuers()));
            }
        }
        anclas.addAll(adicionales);

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try {
            ks.load(null, null);
        } catch (IOException e) {
            throw new GeneralSecurityException(e);
        }
        for (int i = 0; i < anclas.size(); i++) {
            ks.setCertificateEntry("ancla-" + i, anclas.get(i));
        }
        return ks;
    }
}
