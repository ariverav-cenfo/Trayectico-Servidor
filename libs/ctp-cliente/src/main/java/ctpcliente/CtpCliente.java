package ctpcliente;

import ctpcliente.auth.CtpTls;
import ctpcliente.core.ConsultaService;
import ctpcliente.core.ResolutorIds;
import ctpcliente.core.VisorService;
import ctpcliente.core.WfsService;
import ctpcliente.core.WmsService;
import ctpcliente.http.HttpTransporte;
import ctpcliente.http.Transporte;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * Punto de entrada. Una instancia es segura para uso concurrente y debe reutilizarse; se cierra al
 * terminar la aplicacion.
 *
 * <pre>{@code
 * try (CtpCliente ctp = CtpCliente.crear()) {
 *     int idRuta = ctp.ids().ruta("0002").id();
 *     List<ParadaEnRecorrido> paradas = ctp.consultas().paradasDeRuta(idRuta);
 * }
 * }</pre>
 */
public final class CtpCliente implements AutoCloseable {

    public static final Duration VIGENCIA_CATALOGOS = Duration.ofHours(12);

    private final Transporte transporte;
    private final VisorService visor;
    private final ConsultaService consultas;
    private final WfsService wfs;
    private final WmsService wms;
    private final ResolutorIds ids;

    private CtpCliente(CtpConfig config, Transporte transporte, Clock reloj) {
        this.transporte = Objects.requireNonNull(transporte, "transporte");
        this.visor = new VisorService(transporte, config.baseVisor());
        this.consultas = new ConsultaService(transporte, config.baseVisor());
        this.wfs = new WfsService(transporte, config.baseGeo(), config.credenciales());
        this.wms = new WmsService(transporte, config.baseGeo(), config.credenciales());
        this.ids = new ResolutorIds(visor, VIGENCIA_CATALOGOS, reloj);
    }

    public static CtpCliente crear() {
        return crear(CtpConfig.porDefecto());
    }

    public static CtpCliente crear(CtpConfig config) {
        return new CtpCliente(config, new HttpTransporte(config, CtpTls.contexto()), Clock.systemUTC());
    }

    /**
     * Crea un cliente sobre un transporte propio, por ejemplo uno simulado en pruebas.
     */
    public static CtpCliente con(CtpConfig config, Transporte transporte, Clock reloj) {
        return new CtpCliente(config, transporte, reloj);
    }

    public VisorService visor() {
        return visor;
    }

    public ConsultaService consultas() {
        return consultas;
    }

    public WfsService wfs() {
        return wfs;
    }

    public WmsService wms() {
        return wms;
    }

    public ResolutorIds ids() {
        return ids;
    }

    @Override
    public void close() {
        transporte.close();
    }
}
