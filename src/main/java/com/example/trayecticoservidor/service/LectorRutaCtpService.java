package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dto.DesplazamientoImportado;
import com.example.trayecticoservidor.dto.ParadaImportada;
import com.example.trayecticoservidor.dto.RamalImportado;
import com.example.trayecticoservidor.dto.RutaImportada;
import com.example.trayecticoservidor.util.UbicacionParada;
import ctpcliente.CtpCliente;
import ctpcliente.core.CapaWfs;
import ctpcliente.core.ConsultaWfs;
import ctpcliente.model.Coordenada;
import ctpcliente.model.Desplazamiento;
import ctpcliente.model.ParadaEnRecorrido;
import ctpcliente.model.Ramal;
import ctpcliente.model.Ruta;
import ctpcliente.util.Cql;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// Trae una ruta del CTP (ramales, recorridos, paradas y trazados) y la convierte al formato de Trayectico. Solo lee: no guarda nada
@Service
public class LectorRutaCtpService {

    private final CtpCliente ctp;

    public LectorRutaCtpService(CtpCliente ctp) {
        this.ctp = ctp;
    }

    public RutaImportada leer(String codigoRuta) {
        // Nunca usar el id interno (56) a mano, el código "0002" se traduce al id
        Ruta ruta = ctp.ids().ruta(codigoRuta);

        List<RamalImportado> ramales = new ArrayList<>();
        for (Ramal ramal : ctp.visor().ramales(ruta.id())) {

            List<DesplazamientoImportado> desplazamientos = new ArrayList<>();
            for (Desplazamiento d : ctp.visor().desplazamientos(ruta.id(), ramal.id())) {
                DatosWfs w = datosWfs(d.codigo());
                // Solo recorridos activos según el CTP. Se revisa antes de pedir las paradas, para no hacerle al CTP solicitudes que no se van a usar
                if (!esActivo(w.estado())) {
                    continue;
                }
                List<ParadaImportada> paradas = ctp.consultas()
                        .paradasDeRecorrido(ruta.id(), ramal.id(), d.id())
                        .stream()
                        .map(this::aParada)
                        .toList();
                desplazamientos.add(new DesplazamientoImportado(
                        d.id(), d.codigo(), w.sentido(), w.longitudM(), w.estado(),
                        w.trazado(), paradas));
            }

            ramales.add(new RamalImportado(
                    ramal.id(), ramal.codigo(), ramal.descripcion(), desplazamientos));
        }
        return new RutaImportada(ruta.id(), ruta.codigo(), ramales);
    }

    private ParadaImportada aParada(ParadaEnRecorrido p) {
        // Una parada sin coordenadas no se puede poner en el mapa: mejor detenerse y saber cuál es
        Coordenada c = p.ubicacion().orElseThrow(() ->
                new IllegalStateException("La parada " + p.codigo() + " viene sin ubicación"));
        // La biblioteca pone 0 cuando el CTP no manda el id
        Long ctpId = p.id() == 0 ? null : p.id();
        UbicacionParada u = UbicacionParada.desdeCodigo(p.codigo());
        return new ParadaImportada(p.codigo(), ctpId, p.orden(), p.descripcionCorta(),
                u.provincia(), u.canton(), u.distrito(), c.x(), c.y());
    }

    // Si el WFS no trae el recorrido, no sabemos si está activo: tampoco se importa
    static boolean esActivo(String estado) {
        return estado != null && estado.strip().equalsIgnoreCase("activo");
    }

    // Lo que trae el WFS de un recorrido: atributos y trazado, en CRTM05
    record DatosWfs(String sentido, Integer longitudM, String estado,
                    List<Coordenada> trazado) {}

    private static final DatosWfs SIN_DATOS = new DatosWfs(null, null, null, List.of());

    // No es private para que la prueba pueda reemplazarla sin llamar al CTP
    DatosWfs datosWfs(String codigoDesplazamiento) {
        ConsultaWfs consulta = ConsultaWfs.de(CapaWfs.DESPLAZAMIENTOS)
                .filtro(Cql.igual("identificador_desplazamiento", codigoDesplazamiento))
                .propiedades("identificador_desplazamiento", "sentido", "longitud", "estado")
                .conGeometria()
                .srs(ConsultaWfs.CRTM05)
                .maximo(1)
                .construir();
        return ctp.wfs().obtener(consulta).elementos().stream()
                .findFirst()
                .map(e -> {
                    // Hay recorridos sin longitud; 0 se guarda como NULL.
                    long metros = e.entero("longitud").orElse(0L);
                    return new DatosWfs(
                            e.texto("sentido").orElse(null),
                            metros > 0 ? (int) metros : null,
                            e.texto("estado").orElse(null),
                            e.geometria().vertices());
                })
                .orElse(SIN_DATOS);
    }
}