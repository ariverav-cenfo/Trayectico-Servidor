package ctpcliente.core;

import ctpcliente.model.Sentido;
import ctpcliente.util.Identificadores;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Filtro de {@code queryTablaDatos}. Las fabricas validan el formato del valor.
 */
public record Filtro(TipoFiltro tipo, String valor) {

    private static final Pattern PROVINCIA = Pattern.compile("[1-7]");
    private static final Pattern CANTON = Pattern.compile("[1-7]\\d{2}");
    private static final Pattern DISTRITO = Pattern.compile("[1-7]\\d{4}");

    public Filtro {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(valor, "valor");
    }

    /**
     * @param idRuta id interno (p. ej. 56 para la ruta 0002)
     */
    public static Filtro ruta(int idRuta) {
        return new Filtro(TipoFiltro.RUTA, Integer.toString(positivo(idRuta)));
    }

    public static Filtro ramal(int idRamal) {
        return new Filtro(TipoFiltro.RAMAL, Integer.toString(positivo(idRamal)));
    }

    public static Filtro desplazamiento(int idDesplazamiento) {
        return new Filtro(TipoFiltro.DESPLAZAMIENTO, Integer.toString(positivo(idDesplazamiento)));
    }

    /**
     * @param codigo codigo publico, p. ej. {@code PBA-10104-054}
     */
    public static Filtro parada(String codigo) {
        if (!Identificadores.esParada(codigo)) {
            throw new IllegalArgumentException("Codigo de parada invalido: " + codigo);
        }
        return new Filtro(TipoFiltro.PARADA, codigo);
    }

    public static Filtro provincia(String codigo) {
        return new Filtro(TipoFiltro.PROVINCIA, validar(codigo, PROVINCIA, "provincia"));
    }

    public static Filtro canton(String codigo) {
        return new Filtro(TipoFiltro.CANTON, validar(codigo, CANTON, "canton"));
    }

    public static Filtro distrito(String codigo) {
        return new Filtro(TipoFiltro.DISTRITO, validar(codigo, DISTRITO, "distrito"));
    }

    public static Filtro sentido(Sentido sentido) {
        return switch (sentido) {
            case IDA -> new Filtro(TipoFiltro.SENTIDO, "1-2");
            case REGRESO -> new Filtro(TipoFiltro.SENTIDO, "2-1");
            default -> throw new IllegalArgumentException("Solo IDA o REGRESO: " + sentido);
        };
    }

    /**
     * @param id id interno de {@link VisorService#rutasNacionales()} (331 para la Ruta Nacional 1)
     */
    public static Filtro carreteraPorId(String id) {
        if (id == null || !id.chars().allMatch(Character::isDigit) || id.isEmpty()) {
            throw new IllegalArgumentException("Id de ruta nacional invalido: " + id);
        }
        return new Filtro(TipoFiltro.RUAR_ID, id);
    }

    /**
     * @param numero numero de la ruta nacional (1 para la Ruta Nacional 1)
     */
    public static Filtro carreteraPorNumero(int numero) {
        return new Filtro(TipoFiltro.RUAR_NUMERO, Integer.toString(positivo(numero)));
    }

    private static int positivo(int v) {
        if (v <= 0) {
            throw new IllegalArgumentException("Se esperaba un id positivo: " + v);
        }
        return v;
    }

    private static String validar(String v, Pattern p, String nombre) {
        if (v == null || !p.matcher(v).matches()) {
            throw new IllegalArgumentException("Codigo de " + nombre + " invalido: " + v);
        }
        return v;
    }
}
