package ctpcliente.model;

import java.util.Comparator;
import java.util.Optional;

/**
 * Parada dentro de un recorrido, segun la consulta {@code paradas_desplazamiento_ruta_ramal}.
 *
 * @param id           id de la parada en las capas WFS
 * @param codigo       codigo publico, p. ej. {@code PBA-10104-054}
 * @param orden        posicion dentro del recorrido, desde 1
 * @param ubicacion    punto en CRTM05
 */
public record ParadaEnRecorrido(
        long id,
        String codigo,
        String descripcionCorta,
        String descripcion,
        int orden,
        TipoParada tipo,
        Sentido sentido,
        String codigoRamal,
        String codigoRuta,
        String provincia,
        String canton,
        String distrito,
        TipoCaseta caseta,
        Optional<Coordenada> ubicacion) {

    public static final Comparator<ParadaEnRecorrido> POR_RAMAL_SENTIDO_ORDEN =
            Comparator.comparing(ParadaEnRecorrido::codigoRamal)
                    .thenComparing(ParadaEnRecorrido::sentido)
                    .thenComparingInt(ParadaEnRecorrido::orden);

    public static ParadaEnRecorrido de(FilaConsulta f) {
        return new ParadaEnRecorrido(
                f.entero("id").orElse(0L),
                f.texto("identificador_parada").orElse(""),
                f.texto("descripcion_corta").orElse(""),
                f.texto("descripcion").orElse(""),
                f.entero("orden_parada").map(Long::intValue).orElse(0),
                TipoParada.desde(f.texto("tipo_parada").orElse(null)),
                Sentido.desde(f.texto("sentido_desplazamiento").orElse(null)),
                f.texto("identificador_ramal").orElse(""),
                f.texto("codigo_ruta").orElse(""),
                f.texto("provincia").orElse(""),
                f.texto("canton").orElse(""),
                f.texto("distrito").orElse(""),
                TipoCaseta.desde(f.texto("tipo_caseta").orElse(null)),
                ubicacion(f));
    }

    private static Optional<Coordenada> ubicacion(FilaConsulta f) {
        Optional<Double> x = f.decimal("coord_x");
        Optional<Double> y = f.decimal("coord_y");
        if (x.isPresent() && y.isPresent()) {
            return Optional.of(new Coordenada(x.get(), y.get()));
        }
        return f.geometria().map(g -> g.vertices().getFirst());
    }
}
