package ctpcliente.core;

import com.fasterxml.jackson.databind.JsonNode;
import ctpcliente.error.CtpServicioException;
import ctpcliente.http.Parametros;
import ctpcliente.http.Transporte;
import ctpcliente.model.Coordenada;
import ctpcliente.model.Desplazamiento;
import ctpcliente.model.Division;
import ctpcliente.model.Envolvente;
import ctpcliente.model.ParadaCatalogo;
import ctpcliente.model.Ramal;
import ctpcliente.model.Ruta;
import ctpcliente.model.RutaNacional;
import ctpcliente.util.Identificadores;
import ctpcliente.util.Textos;
import ctpcliente.util.Wkt;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Servicio A: catalogos del visor ({@code GET /Visor/service/ctp} y
 * {@code GET /Visor/service/selectoresseriesdatos}). Todas las geometrias estan en CRTM05.
 */
public final class VisorService {

    private static final String ACCION = "action";
    private static final String NOMBRE = "nombre";

    private final Transporte transporte;
    private final URI servicioCtp;
    private final URI servicioSelectores;

    public VisorService(Transporte transporte, URI baseVisor) {
        this.transporte = transporte;
        this.servicioCtp = baseVisor.resolve("/Visor/service/ctp");
        this.servicioSelectores = baseVisor.resolve("/Visor/service/selectoresseriesdatos");
    }

    public List<Ruta> rutas() {
        return leer(servicioCtp, Parametros.nuevos().con(ACCION, "getrutas"),
                n -> new Ruta(n.path("id").asInt(), Textos.normalizar(n.path(NOMBRE).asText())));
    }

    /**
     * @param idRuta id interno de la ruta, no su codigo publico
     */
    public List<Ramal> ramales(int idRuta) {
        return leer(servicioCtp, Parametros.nuevos().con(ACCION, "getramales").con("ruta", idRuta), n -> {
            String[] partes = Identificadores.separarNombre(n.path(NOMBRE).asText());
            return new Ramal(n.path("id").asInt(), partes[0], partes[1]);
        });
    }

    /**
     * @param idRuta  id interno de la ruta
     * @param idRamal id interno del ramal
     */
    public List<Desplazamiento> desplazamientos(int idRuta, int idRamal) {
        Parametros p = Parametros.nuevos().con(ACCION, "getdesplazamientos").con("ruta", idRuta).con("ramal", idRamal);
        return leer(servicioCtp, p, n -> {
            String[] partes = Identificadores.separarNombre(n.path(NOMBRE).asText());
            return new Desplazamiento(n.path("id").asInt(), partes[0], partes[1],
                    Textos.normalizar(n.path("descripcion_desplazamiento").asText()), envolvente(n));
        });
    }

    public List<ParadaCatalogo> paradas() {
        return leer(servicioCtp, Parametros.nuevos().con(ACCION, "getparadas"),
                n -> new ParadaCatalogo(n.path("id").asInt(), Textos.normalizar(n.path(NOMBRE).asText()), punto(n)));
    }

    /**
     * @return rutas nacionales con el id que esperan {@code rutas_carretera} y {@code ramales_carretera}
     */
    public List<RutaNacional> rutasNacionales() {
        return leer(servicioCtp, Parametros.nuevos().con(ACCION, "getredvialnacional"), VisorService::rutaNacional);
    }

    /**
     * @return rutas nacionales con id igual al numero, como espera {@code rutas_ramales_desplazamientos_carretera}
     */
    public List<RutaNacional> rutasNacionalesPorNumero() {
        return leer(servicioCtp, Parametros.nuevos().con(ACCION, "getredvialnacionalruar"), VisorService::rutaNacional);
    }

    public List<Division> provincias() {
        return leer(servicioSelectores, Parametros.nuevos().con(ACCION, "provincias"), VisorService::division);
    }

    /**
     * @param provincia codigo de provincia, 1 a 7
     */
    public List<Division> cantones(String provincia) {
        return leer(servicioSelectores, Parametros.nuevos().con(ACCION, "cantones").con("provincia", provincia),
                VisorService::division);
    }

    /**
     * @param canton codigo de canton de 3 digitos
     */
    public List<Division> distritos(String canton) {
        return leer(servicioSelectores, Parametros.nuevos().con(ACCION, "distritos").con("canton", canton),
                VisorService::division);
    }

    private <T> List<T> leer(URI servicio, Parametros parametros, Function<JsonNode, T> mapear) {
        URI uri = parametros.aplicarA(servicio);
        JsonNode raiz = Respuestas.json(transporte.get(uri), uri);
        if (!raiz.path("result").asBoolean(false)) {
            throw new CtpServicioException("El servicio respondio result=false: " + uri);
        }
        JsonNode datos = raiz.path("data");
        List<T> lista = new ArrayList<>(datos.size());
        datos.forEach(n -> lista.add(mapear.apply(n)));
        return List.copyOf(lista);
    }

    private static RutaNacional rutaNacional(JsonNode n) {
        return new RutaNacional(n.path("id").asText(), n.path(NOMBRE).asInt(), envolvente(n));
    }

    private static Division division(JsonNode n) {
        return new Division(n.path("id").asText(), Textos.normalizar(n.path(NOMBRE).asText()), envolvente(n));
    }

    private static Optional<Envolvente> envolvente(JsonNode n) {
        return wkt(n).map(w -> Wkt.leer(w).envolvente());
    }

    private static Optional<Coordenada> punto(JsonNode n) {
        return wkt(n).map(w -> Wkt.leer(w).vertices().getFirst());
    }

    private static Optional<String> wkt(JsonNode n) {
        String g = n.path("geometria").asText("");
        return g.isBlank() ? Optional.empty() : Optional.of(g);
    }
}
