package ctpcliente.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validacion y descomposicion de los codigos publicos del CTP.
 */
public final class Identificadores {

    private static final Pattern RUTA = Pattern.compile("\\d{4}");
    private static final Pattern RAMAL = Pattern.compile("(\\d{4})-([A-Z]{1,3})");
    private static final Pattern DESPLAZAMIENTO = Pattern.compile("(\\d{4})-([A-Z]{1,3})-(\\d{1,2})");
    private static final Pattern PARADA = Pattern.compile("([A-Z]{3})-((\\d)(\\d{2})(\\d{2}))-(\\d{3,4})");
    private static final Pattern FRACCIONAMIENTO = Pattern.compile("(\\d{4})-FT-(\\d{1,3})");

    private Identificadores() {
    }

    /**
     * Codigo de parada descompuesto.
     *
     * @param distrito    codigo INEC de 5 digitos
     * @param canton      codigo INEC de 3 digitos
     * @param provincia   codigo INEC de 1 digito
     * @param consecutivo numero de la parada dentro del distrito
     */
    public record CodigoParada(String codigo, String prefijo, String provincia, String canton, String distrito,
                               int consecutivo) {
    }

    public static boolean esRuta(String s) {
        return s != null && RUTA.matcher(s).matches();
    }

    public static boolean esRamal(String s) {
        return s != null && RAMAL.matcher(s).matches();
    }

    public static boolean esDesplazamiento(String s) {
        return s != null && DESPLAZAMIENTO.matcher(s).matches();
    }

    public static boolean esParada(String s) {
        return s != null && PARADA.matcher(s).matches();
    }

    public static boolean esFraccionamiento(String s) {
        return s != null && FRACCIONAMIENTO.matcher(s).matches();
    }

    public static Optional<CodigoParada> parada(String s) {
        if (s == null) {
            return Optional.empty();
        }
        Matcher m = PARADA.matcher(s);
        if (!m.matches()) {
            return Optional.empty();
        }
        String distrito = m.group(2);
        return Optional.of(new CodigoParada(s, m.group(1), m.group(3), distrito.substring(0, 3), distrito,
                Integer.parseInt(m.group(6))));
    }

    /**
     * @return los cuatro digitos de ruta de un codigo de ramal, desplazamiento o fraccionamiento
     */
    public static Optional<String> rutaDe(String codigo) {
        if (codigo == null || codigo.length() < 4 || !RUTA.matcher(codigo.substring(0, 4)).matches()) {
            return Optional.empty();
        }
        return Optional.of(codigo.substring(0, 4));
    }

    /**
     * @return el codigo de ramal de un codigo de desplazamiento
     */
    public static Optional<String> ramalDe(String codigoDesplazamiento) {
        if (codigoDesplazamiento == null) {
            return Optional.empty();
        }
        Matcher m = DESPLAZAMIENTO.matcher(codigoDesplazamiento);
        return m.matches() ? Optional.of(m.group(1) + "-" + m.group(2)) : Optional.empty();
    }

    /**
     * Separa nombres del catalogo con la forma {@code "0002-A, SAN JOSE - SABANA"}.
     *
     * @return codigo y descripcion normalizados; la descripcion es vacia si no hay coma
     */
    public static String[] separarNombre(String nombre) {
        String n = Textos.normalizar(nombre);
        int coma = n.indexOf(',');
        if (coma < 0) {
            return new String[] {n, ""};
        }
        return new String[] {n.substring(0, coma).strip(), n.substring(coma + 1).strip()};
    }
}
