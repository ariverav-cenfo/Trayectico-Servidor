package ctpcliente.model;

import java.util.List;
import java.util.OptionalLong;

/**
 * Coleccion de elementos devuelta por GetFeature o GetFeatureInfo.
 *
 * @param elementos elementos recibidos
 * @param total     total que cumple el filtro, si el servidor lo informa
 */
public record ColeccionElementos(List<Elemento> elementos, OptionalLong total) {

    public ColeccionElementos {
        elementos = List.copyOf(elementos);
    }

    public int tamano() {
        return elementos.size();
    }

    public boolean estaVacia() {
        return elementos.isEmpty();
    }
}
