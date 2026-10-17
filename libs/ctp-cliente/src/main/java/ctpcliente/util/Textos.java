package ctpcliente.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizacion de textos del CTP, que traen espacios sobrantes y mayusculas inconsistentes.
 */
public final class Textos {

    private static final Pattern ESPACIOS = Pattern.compile("\\s+");
    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");

    private Textos() {
    }

    /**
     * @return texto sin espacios al inicio o al final y con espacios internos simples; vacio si es nulo
     */
    public static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return ESPACIOS.matcher(s.strip()).replaceAll(" ");
    }

    /**
     * @return clave de comparacion: normalizada, en minusculas y sin tildes
     */
    public static String clave(String s) {
        String sinMarcas = MARCAS.matcher(Normalizer.normalize(normalizar(s), Normalizer.Form.NFD)).replaceAll("");
        return sinMarcas.toLowerCase(Locale.ROOT);
    }

    public static boolean equivalentes(String a, String b) {
        return clave(a).equals(clave(b));
    }
}
