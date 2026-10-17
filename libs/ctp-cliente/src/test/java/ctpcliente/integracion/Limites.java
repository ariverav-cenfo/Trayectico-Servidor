package ctpcliente.integracion;

import ctpcliente.model.Coordenada;
import ctpcliente.model.Envolvente;

final class Limites {

    static final Envolvente COSTA_RICA_WGS84 = new Envolvente(-86.0, 8.0, -82.5, 11.3);

    private Limites() {
    }

    static boolean enCostaRica(Coordenada lonLat) {
        return COSTA_RICA_WGS84.contiene(lonLat);
    }
}
