package ctpcliente;

import ctpcliente.auth.CtpCredenciales;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuracion inmutable del cliente.
 *
 * @param baseVisor        raiz de los servicios A y B
 * @param baseGeo          proxy de GeoServer (WFS y WMS)
 * @param credenciales     token del proxy de GeoServer
 * @param agenteUsuario    valor de la cabecera {@code User-Agent}
 * @param tiempoConexion   tiempo maximo para establecer la conexion
 * @param tiempoRespuesta  tiempo maximo por peticion
 * @param reintentos       reintentos ante errores de red, 429 o 5xx
 * @param esperaInicial    espera antes del primer reintento; se duplica en cada intento
 * @param intervaloMinimo  separacion minima entre peticiones
 * @param tamanoMaximo     bytes maximos de una respuesta leida en memoria
 */
public record CtpConfig(
        URI baseVisor,
        URI baseGeo,
        CtpCredenciales credenciales,
        String agenteUsuario,
        Duration tiempoConexion,
        Duration tiempoRespuesta,
        int reintentos,
        Duration esperaInicial,
        Duration intervaloMinimo,
        long tamanoMaximo) {

    public static final URI BASE_VISOR = URI.create("https://visortp.ctp.go.cr");
    public static final URI BASE_GEO = URI.create("https://mapas.ctp.go.cr/GeoP/geoprox2");

    public CtpConfig {
        Objects.requireNonNull(baseVisor, "baseVisor");
        Objects.requireNonNull(baseGeo, "baseGeo");
        Objects.requireNonNull(credenciales, "credenciales");
        Objects.requireNonNull(agenteUsuario, "agenteUsuario");
        Objects.requireNonNull(tiempoConexion, "tiempoConexion");
        Objects.requireNonNull(tiempoRespuesta, "tiempoRespuesta");
        Objects.requireNonNull(esperaInicial, "esperaInicial");
        Objects.requireNonNull(intervaloMinimo, "intervaloMinimo");
        if (reintentos < 0) {
            throw new IllegalArgumentException("reintentos < 0");
        }
        if (tamanoMaximo <= 0) {
            throw new IllegalArgumentException("tamanoMaximo <= 0");
        }
    }

    public static CtpConfig porDefecto() {
        return constructor().construir();
    }

    public static Constructor constructor() {
        return new Constructor();
    }

    public Constructor aConstructor() {
        return new Constructor()
                .baseVisor(baseVisor)
                .baseGeo(baseGeo)
                .credenciales(credenciales)
                .agenteUsuario(agenteUsuario)
                .tiempoConexion(tiempoConexion)
                .tiempoRespuesta(tiempoRespuesta)
                .reintentos(reintentos)
                .esperaInicial(esperaInicial)
                .intervaloMinimo(intervaloMinimo)
                .tamanoMaximo(tamanoMaximo);
    }

    /**
     * Constructor de {@link CtpConfig} con los valores por defecto ya cargados.
     */
    public static final class Constructor {
        private URI baseVisor = BASE_VISOR;
        private URI baseGeo = BASE_GEO;
        private CtpCredenciales credenciales = CtpCredenciales.desdeEntorno();
        private String agenteUsuario = "ctp-cliente/1.0 (Java " + Runtime.version().feature() + ")";
        private Duration tiempoConexion = Duration.ofSeconds(10);
        private Duration tiempoRespuesta = Duration.ofSeconds(90);
        private int reintentos = 3;
        private Duration esperaInicial = Duration.ofMillis(500);
        private Duration intervaloMinimo = Duration.ofMillis(100);
        private long tamanoMaximo = 64L * 1024 * 1024;

        private Constructor() {
        }

        public Constructor baseVisor(URI v) {
            this.baseVisor = v;
            return this;
        }

        public Constructor baseGeo(URI v) {
            this.baseGeo = v;
            return this;
        }

        public Constructor credenciales(CtpCredenciales v) {
            this.credenciales = v;
            return this;
        }

        public Constructor agenteUsuario(String v) {
            this.agenteUsuario = v;
            return this;
        }

        public Constructor tiempoConexion(Duration v) {
            this.tiempoConexion = v;
            return this;
        }

        public Constructor tiempoRespuesta(Duration v) {
            this.tiempoRespuesta = v;
            return this;
        }

        public Constructor reintentos(int v) {
            this.reintentos = v;
            return this;
        }

        public Constructor esperaInicial(Duration v) {
            this.esperaInicial = v;
            return this;
        }

        public Constructor intervaloMinimo(Duration v) {
            this.intervaloMinimo = v;
            return this;
        }

        public Constructor tamanoMaximo(long v) {
            this.tamanoMaximo = v;
            return this;
        }

        public CtpConfig construir() {
            return new CtpConfig(baseVisor, baseGeo, credenciales, agenteUsuario, tiempoConexion,
                    tiempoRespuesta, reintentos, esperaInicial, intervaloMinimo, tamanoMaximo);
        }
    }
}
