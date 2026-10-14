package ctpcliente.util;

import ctpcliente.model.Envolvente;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Construccion segura de filtros ECQL para {@code CQL_FILTER}. Los nombres de campo se validan y los
 * literales se escapan, de modo que un valor ingresado por un usuario no puede alterar el filtro.
 */
public final class Cql {

    private static final Pattern CAMPO = Pattern.compile("[A-Za-z_]\\w*");
    private static final Pattern SRS = Pattern.compile("EPSG:\\d{4,5}");

    private Cql() {
    }

    public static String campo(String nombre) {
        if (nombre == null || !CAMPO.matcher(nombre).matches()) {
            throw new IllegalArgumentException("Nombre de campo invalido: " + nombre);
        }
        return nombre;
    }

    public static String literal(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Literal nulo");
        }
        return "'" + valor.replace("'", "''") + "'";
    }

    public static String igual(String campo, String valor) {
        return campo(campo) + " = " + literal(valor);
    }

    public static String igual(String campo, long valor) {
        return campo(campo) + " = " + valor;
    }

    public static String distinto(String campo, String valor) {
        return campo(campo) + " <> " + literal(valor);
    }

    public static String mayorQue(String campo, double valor) {
        return campo(campo) + " > " + numero(valor);
    }

    public static String menorQue(String campo, double valor) {
        return campo(campo) + " < " + numero(valor);
    }

    /**
     * @param patron patron LIKE; {@code %} y {@code _} conservan su significado de comodin
     */
    public static String como(String campo, String patron) {
        return campo(campo) + " LIKE " + literal(patron);
    }

    public static String comoSinMayusculas(String campo, String patron) {
        return campo(campo) + " ILIKE " + literal(patron);
    }

    /**
     * El servidor no admite escapar comodines: {@code %} y {@code \} del texto se convierten en
     * {@code _}, que coincide con cualquier caracter. El resultado puede incluir coincidencias de mas, nunca
     * de menos.
     */
    public static String empiezaCon(String campo, String prefijo) {
        return como(campo, neutralizarComodines(prefijo) + "%");
    }

    /**
     * Busqueda sin distinguir mayusculas. Los comodines del texto se tratan como en {@link #empiezaCon}.
     */
    public static String contiene(String campo, String texto) {
        return comoSinMayusculas(campo, "%" + neutralizarComodines(texto) + "%");
    }

    public static String en(String campo, Collection<String> valores) {
        if (valores.isEmpty()) {
            throw new IllegalArgumentException("Lista vacia para IN");
        }
        return campo(campo) + " IN (" + valores.stream().map(Cql::literal).collect(Collectors.joining(",")) + ")";
    }

    public static String esNulo(String campo) {
        return campo(campo) + " IS NULL";
    }

    public static String bbox(String campoGeometria, Envolvente e, String srs) {
        if (srs == null || !SRS.matcher(srs).matches()) {
            throw new IllegalArgumentException("SRS invalido: " + srs);
        }
        return "BBOX(" + campo(campoGeometria) + "," + numero(e.minX()) + "," + numero(e.minY()) + ","
                + numero(e.maxX()) + "," + numero(e.maxY()) + ",'" + srs + "')";
    }

    public static String y(String... condiciones) {
        return unir(" AND ", condiciones);
    }

    public static String o(String... condiciones) {
        return unir(" OR ", condiciones);
    }

    public static String no(String condicion) {
        return "NOT (" + condicion + ")";
    }

    private static String unir(String operador, String... condiciones) {
        if (condiciones.length == 0) {
            throw new IllegalArgumentException("Sin condiciones");
        }
        if (condiciones.length == 1) {
            return condiciones[0];
        }
        return Arrays.stream(condiciones).map(c -> "(" + c + ")").collect(Collectors.joining(operador));
    }

    private static String neutralizarComodines(String s) {
        return s.replace('\\', '_').replace('%', '_');
    }

    private static String numero(double v) {
        if (!Double.isFinite(v)) {
            throw new IllegalArgumentException("Numero no finito: " + v);
        }
        return String.valueOf(v);
    }
}
