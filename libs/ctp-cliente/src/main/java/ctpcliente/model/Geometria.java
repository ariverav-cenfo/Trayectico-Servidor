package ctpcliente.model;

import java.util.List;

/**
 * Geometria de un elemento. Las coordenadas estan en el sistema en que se pidieron.
 */
public sealed interface Geometria {

    /**
     * @return todos los vertices, en orden
     */
    List<Coordenada> vertices();

    default Envolvente envolvente() {
        return Envolvente.de(vertices());
    }

    /**
     * Un punto.
     */
    record Punto(Coordenada coordenada) implements Geometria {
        @Override
        public List<Coordenada> vertices() {
            return List.of(coordenada);
        }
    }

    /**
     * Una linea abierta.
     */
    record Linea(List<Coordenada> puntos) implements Geometria {
        public Linea {
            puntos = List.copyOf(puntos);
        }

        @Override
        public List<Coordenada> vertices() {
            return puntos;
        }
    }

    /**
     * Varias lineas; los recorridos con tramos discontinuos llegan asi.
     */
    record MultiLinea(List<Linea> lineas) implements Geometria {
        public MultiLinea {
            lineas = List.copyOf(lineas);
        }

        @Override
        public List<Coordenada> vertices() {
            return lineas.stream().flatMap(l -> l.puntos().stream()).toList();
        }
    }

    /**
     * @param anillos el primero es el exterior; los demas, huecos
     */
    record Poligono(List<List<Coordenada>> anillos) implements Geometria {
        public Poligono {
            anillos = List.copyOf(anillos.stream().map(List::copyOf).toList());
        }

        @Override
        public List<Coordenada> vertices() {
            return anillos.stream().flatMap(List::stream).toList();
        }
    }

    /**
     * Varios puntos.
     */
    record MultiPunto(List<Coordenada> puntos) implements Geometria {
        public MultiPunto {
            puntos = List.copyOf(puntos);
        }

        @Override
        public List<Coordenada> vertices() {
            return puntos;
        }
    }
}
