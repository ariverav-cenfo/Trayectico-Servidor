package ctpcliente.util;

import ctpcliente.model.Coordenada;
import ctpcliente.model.Geometria;

import java.util.ArrayList;
import java.util.List;

/**
 * Lector de WKT para los tipos que devuelve el CTP: POINT, MULTIPOINT, LINESTRING, MULTILINESTRING y
 * POLYGON.
 */
public final class Wkt {

    private Wkt() {
    }

    /**
     * @throws IllegalArgumentException si el texto no es WKT valido o el tipo no esta soportado
     */
    public static Geometria leer(String wkt) {
        if (wkt == null || wkt.isBlank()) {
            throw new IllegalArgumentException("WKT vacio");
        }
        String texto = wkt.strip();
        int abre = texto.indexOf('(');
        if (abre < 0) {
            throw new IllegalArgumentException("WKT sin coordenadas: " + resumen(texto));
        }
        String nombreTipo = texto.substring(0, abre).strip();
        Tipo tipo = Tipo.de(nombreTipo);
        Lector lector = new Lector(texto, abre);
        Nodo raiz = lector.leerNodo();
        lector.exigirFin();
        return construir(tipo, raiz);
    }

    private static Geometria construir(Tipo tipo, Nodo raiz) {
        return switch (tipo) {
            case POINT -> new Geometria.Punto(raiz.unaCoordenada());
            case MULTIPOINT -> new Geometria.MultiPunto(raiz.puntosSueltos());
            case LINESTRING -> new Geometria.Linea(raiz.coordenadas());
            case MULTILINESTRING -> new Geometria.MultiLinea(
                    raiz.hijos().stream().map(n -> new Geometria.Linea(n.coordenadas())).toList());
            case POLYGON -> new Geometria.Poligono(raiz.hijos().stream().map(Nodo::coordenadas).toList());
        };
    }

    private enum Tipo {
        POINT, MULTIPOINT, LINESTRING, MULTILINESTRING, POLYGON;

        static Tipo de(String nombre) {
            String mayusculas = mayusculasAscii(nombre);
            for (Tipo t : values()) {
                if (t.name().equals(mayusculas)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("Tipo WKT no soportado: " + nombre);
        }

        private static String mayusculasAscii(String nombre) {
            StringBuilder sb = new StringBuilder(nombre.length());
            for (int i = 0; i < nombre.length(); i++) {
                char c = nombre.charAt(i);
                boolean minuscula = c >= 'a' && c <= 'z';
                if (!minuscula && (c < 'A' || c > 'Z')) {
                    throw new IllegalArgumentException("Tipo WKT no soportado: " + nombre);
                }
                sb.append(minuscula ? (char) (c - 'a' + 'A') : c);
            }
            return sb.toString();
        }
    }

    private static String resumen(String s) {
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }

    private record Nodo(List<Nodo> hijos, List<Coordenada> coordenadas) {

        Coordenada unaCoordenada() {
            if (coordenadas.size() != 1) {
                throw new IllegalArgumentException("Se esperaba una coordenada");
            }
            return coordenadas.getFirst();
        }

        List<Coordenada> puntosSueltos() {
            if (!coordenadas.isEmpty()) {
                return coordenadas;
            }
            return hijos.stream().map(Nodo::unaCoordenada).toList();
        }
    }

    private static final class Lector {
        private final String s;
        private int i;

        Lector(String s, int inicio) {
            this.s = s;
            this.i = inicio;
        }

        Nodo leerNodo() {
            esperar('(');
            saltarEspacios();
            if (s.charAt(i) == '(') {
                List<Nodo> hijos = new ArrayList<>();
                do {
                    hijos.add(leerNodo());
                    saltarEspacios();
                } while (consumir(','));
                esperar(')');
                return new Nodo(hijos, List.of());
            }
            List<Coordenada> coords = new ArrayList<>();
            do {
                coords.add(leerCoordenada());
            } while (consumir(','));
            esperar(')');
            return new Nodo(List.of(), coords);
        }

        Coordenada leerCoordenada() {
            double x = leerNumero();
            double y = leerNumero();
            saltarEspacios();
            while (i < s.length() && s.charAt(i) != ',' && s.charAt(i) != ')') {
                leerNumero();
                saltarEspacios();
            }
            return new Coordenada(x, y);
        }

        double leerNumero() {
            saltarEspacios();
            int inicio = i;
            while (i < s.length() && "+-.0123456789eE".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            if (inicio == i) {
                throw new IllegalArgumentException("Numero esperado en la posicion " + i + ": " + resumen(s));
            }
            return Double.parseDouble(s.substring(inicio, i));
        }

        boolean consumir(char c) {
            saltarEspacios();
            if (i < s.length() && s.charAt(i) == c) {
                i++;
                return true;
            }
            return false;
        }

        void esperar(char c) {
            if (!consumir(c)) {
                throw new IllegalArgumentException("Se esperaba '" + c + "' en la posicion " + i + ": " + resumen(s));
            }
        }

        void saltarEspacios() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        void exigirFin() {
            saltarEspacios();
            if (i != s.length()) {
                throw new IllegalArgumentException("Texto sobrante en la posicion " + i + ": " + resumen(s));
            }
        }
    }
}
