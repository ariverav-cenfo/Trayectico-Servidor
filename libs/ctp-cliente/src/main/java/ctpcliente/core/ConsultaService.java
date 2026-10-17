package ctpcliente.core;

import com.fasterxml.jackson.databind.JsonNode;
import ctpcliente.error.CtpServicioException;
import ctpcliente.http.Parametros;
import ctpcliente.http.Transporte;
import ctpcliente.model.FilaConsulta;
import ctpcliente.model.ParadaEnRecorrido;

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio B: {@code POST /Visor/service/ctp} con {@code action=queryTablaDatos}.
 *
 * <p>Una respuesta {@code success:false} sin mensaje significa que no hay resultados y se devuelve una
 * lista vacia; con mensaje, se lanza {@link CtpServicioException}.
 */
public final class ConsultaService {

    private final Transporte transporte;
    private final URI servicio;

    public ConsultaService(Transporte transporte, URI baseVisor) {
        this.transporte = transporte;
        this.servicio = baseVisor.resolve("/Visor/service/ctp");
    }

    /**
     * @throws IllegalArgumentException si falta un filtro obligatorio, sobra uno no permitido o se repite
     */
    public List<FilaConsulta> consultar(Caracterizacion caracterizacion, Filtro... filtros) {
        validar(caracterizacion, List.of(filtros));
        JsonNode raiz = Respuestas.json(transporte.post(servicio, formulario(caracterizacion, filtros)), servicio);
        if (!raiz.path("success").asBoolean(false)) {
            String mensaje = raiz.path("message").asText("");
            if (!mensaje.isBlank()) {
                throw new CtpServicioException(mensaje);
            }
            return List.of();
        }
        List<FilaConsulta> filas = new ArrayList<>();
        raiz.path("data").forEach(n -> filas.add(new FilaConsulta(GeoJson.mapa(n))));
        return List.copyOf(filas);
    }

    /**
     * @return paradas de todos los recorridos de la ruta, ordenadas por ramal, sentido y orden
     */
    public List<ParadaEnRecorrido> paradasDeRuta(int idRuta) {
        return paradas(Filtro.ruta(idRuta));
    }

    public List<ParadaEnRecorrido> paradasDeRamal(int idRuta, int idRamal) {
        return paradas(Filtro.ruta(idRuta), Filtro.ramal(idRamal));
    }

    public List<ParadaEnRecorrido> paradasDeRecorrido(int idRuta, int idRamal, int idDesplazamiento) {
        return paradas(Filtro.ruta(idRuta), Filtro.ramal(idRamal), Filtro.desplazamiento(idDesplazamiento));
    }

    /**
     * @return codigos de las rutas que pasan por la parada
     */
    public List<String> rutasPorParada(String codigoParada) {
        return consultar(Caracterizacion.RUTAS_POR_PARADA, Filtro.parada(codigoParada)).stream()
                .map(f -> f.texto("codigo_ruta").orElse(""))
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    private List<ParadaEnRecorrido> paradas(Filtro... filtros) {
        return consultar(Caracterizacion.PARADAS_DE_RECORRIDO, filtros).stream()
                .map(ParadaEnRecorrido::de)
                .sorted(ParadaEnRecorrido.POR_RAMAL_SENTIDO_ORDEN)
                .toList();
    }

    static void validar(Caracterizacion c, List<Filtro> filtros) {
        Set<TipoFiltro> presentes = EnumSet.noneOf(TipoFiltro.class);
        for (Filtro f : filtros) {
            if (!c.permitidos().contains(f.tipo())) {
                throw new IllegalArgumentException(c + " no admite el filtro " + f.tipo() + "; admite " + c.permitidos());
            }
            if (!presentes.add(f.tipo())) {
                throw new IllegalArgumentException("Filtro repetido: " + f.tipo());
            }
        }
        if (!presentes.containsAll(c.obligatorios())) {
            throw new IllegalArgumentException(c + " requiere " + c.obligatorios() + "; recibio " + presentes);
        }
    }

    static Parametros formulario(Caracterizacion c, Filtro... filtros) {
        Parametros p = Parametros.nuevos().con("action", "queryTablaDatos").con("caracterizacion", c.clave());
        for (int i = 0; i < filtros.length; i++) {
            p.con("filtros[" + i + "][filtro]", filtros[i].tipo().nombreApi());
            p.con("filtros[" + i + "][valor]", filtros[i].valor());
        }
        return p;
    }
}
