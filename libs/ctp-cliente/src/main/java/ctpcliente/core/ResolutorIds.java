package ctpcliente.core;

import ctpcliente.error.CtpNoEncontradoException;
import ctpcliente.model.Desplazamiento;
import ctpcliente.model.Ramal;
import ctpcliente.model.Ruta;
import ctpcliente.model.RutaNacional;
import ctpcliente.util.CacheTemporal;
import ctpcliente.util.Identificadores;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Traduce codigos publicos a los ids internos que exigen los filtros. Los catalogos se guardan en cache
 * durante {@code vigencia}.
 */
public final class ResolutorIds {

    private final VisorService visor;
    private final CacheTemporal<String, Map<String, Ruta>> rutas;
    private final CacheTemporal<Integer, List<Ramal>> ramales;
    private final CacheTemporal<String, List<Desplazamiento>> desplazamientos;
    private final CacheTemporal<String, Map<Integer, RutaNacional>> carreteras;

    public ResolutorIds(VisorService visor, Duration vigencia, Clock reloj) {
        this.visor = visor;
        this.rutas = new CacheTemporal<>(vigencia, reloj);
        this.ramales = new CacheTemporal<>(vigencia, reloj);
        this.desplazamientos = new CacheTemporal<>(vigencia, reloj);
        this.carreteras = new CacheTemporal<>(vigencia, reloj);
    }

    /**
     * @param codigo codigo de ruta, p. ej. {@code 0002}
     */
    public Ruta ruta(String codigo) {
        Ruta r = rutas.obtener("rutas", k -> visor.rutas().stream()
                .collect(Collectors.toUnmodifiableMap(Ruta::codigo, Function.identity(), (a, b) -> a)))
                .get(codigo);
        if (r == null) {
            throw new CtpNoEncontradoException("Ruta no encontrada: " + codigo);
        }
        return r;
    }

    /**
     * @param codigo codigo de ramal, p. ej. {@code 0002-A}
     */
    public Ramal ramal(String codigo) {
        String codigoRuta = Identificadores.rutaDe(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Codigo de ramal invalido: " + codigo));
        int idRuta = ruta(codigoRuta).id();
        return ramales.obtener(idRuta, visor::ramales).stream()
                .filter(r -> r.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new CtpNoEncontradoException("Ramal no encontrado: " + codigo));
    }

    /**
     * @param codigo codigo de desplazamiento, p. ej. {@code 0002-A-1}
     */
    public Desplazamiento desplazamiento(String codigo) {
        String codigoRamal = Identificadores.ramalDe(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Codigo de desplazamiento invalido: " + codigo));
        int idRuta = ruta(codigoRamal.substring(0, 4)).id();
        int idRamal = ramal(codigoRamal).id();
        return desplazamientos.obtener(idRuta + "/" + idRamal, k -> visor.desplazamientos(idRuta, idRamal)).stream()
                .filter(d -> d.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new CtpNoEncontradoException("Desplazamiento no encontrado: " + codigo));
    }

    /**
     * @return la ruta nacional con el id interno que espera {@link Filtro#carreteraPorId}
     */
    public RutaNacional carretera(int numero) {
        RutaNacional r = carreteras.obtener("carreteras", k -> visor.rutasNacionales().stream()
                .collect(Collectors.toUnmodifiableMap(RutaNacional::numero, Function.identity(), (a, b) -> a)))
                .get(numero);
        if (r == null) {
            throw new CtpNoEncontradoException("Ruta nacional no encontrada: " + numero);
        }
        return r;
    }

    public void limpiar() {
        rutas.limpiar();
        ramales.limpiar();
        desplazamientos.limpiar();
        carreteras.limpiar();
    }
}
