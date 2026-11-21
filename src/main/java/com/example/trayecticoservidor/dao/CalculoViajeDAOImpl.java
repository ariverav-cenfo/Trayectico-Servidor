package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.model.TramoRutaInterno;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 *Proposito: Puente a la DB para crear la informacion de los viajes, contiene los queries necesarios para recolectar la informacion del viaje, rutas, paradas, coordenadas, etc
 */
@Repository
public class CalculoViajeDAOImpl implements CalculoViajeDAO{
    private final JdbcTemplate jdbcTemplate;

    public CalculoViajeDAOImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<List<TramoRutaInterno>> buscarOpcionesViaje(
            BigDecimal origenLat, BigDecimal origenLon,
            BigDecimal destinoLat, BigDecimal destinoLon,
            double maxDistanciaCaminataMetros) {

        String sql = """
            WITH origen_geom AS (
                SELECT ST_Transform(ST_SetSRID(ST_MakePoint(?, ?), 4326), 5367) AS geom
            ),
            destino_geom AS (
                SELECT ST_Transform(ST_SetSRID(ST_MakePoint(?, ?), 4326), 5367) AS geom
            ),
            paradas_origen AS (
                SELECT p.id, p.codigo_ctp, p.descripcion, p.geometria,
                       ST_Distance(p.geometria, o.geom) AS dist_caminata_origen
                FROM paradas p, origen_geom o
                WHERE ST_DWithin(p.geometria, o.geom, ?)
            ),
            paradas_destino AS (
                SELECT p.id, p.codigo_ctp, p.descripcion, p.geometria,
                       ST_Distance(p.geometria, d.geom) AS dist_caminata_destino
                FROM paradas p, destino_geom d
                WHERE ST_DWithin(p.geometria, d.geom, ?)
            ),
            recorridos_directos AS (
                SELECT
                    po.codigo_ctp AS parada_origen_codigo,
                    po.descripcion AS parada_origen_nombre,
                    ST_Y(ST_Transform(po.geometria, 4326)) AS parada_origen_lat,
                    ST_X(ST_Transform(po.geometria, 4326)) AS parada_origen_lon,
                    pd.codigo_ctp AS parada_destino_codigo,
                    pd.descripcion AS parada_destino_nombre,
                    ST_Y(ST_Transform(pd.geometria, 4326)) AS parada_destino_lat,
                    ST_X(ST_Transform(pd.geometria, 4326)) AS parada_destino_lon,
                    r.codigo_ctp AS codigo_ruta,
                    COALESCE(r.nombre, (
                                 SELECT b.descripcion
                                 FROM ramales b
                                 WHERE b.ruta_id = r.id
                                 ORDER BY b.codigo_ctp
                                 LIMIT 1
                             )) AS nombre_ruta,
                    rm.codigo_ctp AS codigo_ramal,
                    rm.descripcion AS nombre_ramal,
                    d.codigo_ctp AS codigo_recorrido,
                    CAST(
                        ABS(
                        ST_LineLocatePoint(d.geometria, po.geometria) - ST_LineLocatePoint(d.geometria, pd.geometria)
                        )* ST_Length(d.geometria) AS INTEGER
                    ) AS distancia_bus,
                                        ST_AsGeoJSON(
                                                    ST_Transform(
                                                        ST_LineSubstring(
                                                            d.geometria,
                                                            LEAST(
                                                                ST_LineLocatePoint(d.geometria, po.geometria),
                                                                ST_LineLocatePoint(d.geometria, pd.geometria)
                                                            ),
                                                            GREATEST(
                                                                ST_LineLocatePoint(d.geometria, po.geometria),
                                                                ST_LineLocatePoint(d.geometria, pd.geometria)
                                                            )
                                                        ),
                                                        4326
                                                    )
                                                ) AS geometria_geojson,
                    po.dist_caminata_origen,
                    pd.dist_caminata_destino
                FROM paradas_origen po
                JOIN paradas_recorridos pd_orig ON po.id = pd_orig.parada_id
                JOIN recorridos d ON d.id = pd_orig.recorrido_id
                JOIN ramales rm ON rm.id = d.ramal_id
                JOIN rutas r ON r.id = rm.ruta_id
                JOIN paradas_recorridos pd_dest ON d.id = pd_dest.recorrido_id AND pd_dest.parada_id IN (SELECT id FROM paradas_destino)
                JOIN paradas_destino pd ON pd.id = pd_dest.parada_id
                WHERE d.activo = TRUE
                  AND (d.estado_ctp IS NULL OR LOWER(d.estado_ctp) = 'activo')
                  AND pd_orig.orden_parada < pd_dest.orden_parada
            )
            SELECT * FROM (
                SELECT DISTINCT ON (codigo_ruta,codigo_ramal) *
                from recorridos_directos
                ORDER BY codigo_ruta, codigo_ramal, (dist_caminata_origen + dist_caminata_destino)
            )sub
            ORDER BY (dist_caminata_origen + dist_caminata_destino)
            LIMIT 5;
        """;

        return jdbcTemplate.query(sql, ps -> {
            ps.setDouble(1, origenLon.doubleValue());
            ps.setDouble(2, origenLat.doubleValue());
            ps.setDouble(3, destinoLon.doubleValue());
            ps.setDouble(4, destinoLat.doubleValue());
            ps.setDouble(5, maxDistanciaCaminataMetros);
            ps.setDouble(6, maxDistanciaCaminataMetros);
        }, (rs, rowNum) -> {
            List<TramoRutaInterno> tramos = new ArrayList<>();
            tramos.add(new TramoRutaInterno(
                    "bus",
                    rs.getInt("distancia_bus"),
                    rs.getString("geometria_geojson"),
                    rs.getString("codigo_ruta"),
                    rs.getString("nombre_ruta"),
                    rs.getString("codigo_ramal"),
                    rs.getString("nombre_ramal"),
                    rs.getString("codigo_recorrido"),
                    rs.getString("parada_origen_codigo"),
                    rs.getString("parada_origen_nombre"),
                    rs.getBigDecimal("parada_origen_lat"),
                    rs.getBigDecimal("parada_origen_lon"),
                    rs.getString("parada_destino_codigo"),
                    rs.getString("parada_destino_nombre"),
                    rs.getBigDecimal("parada_destino_lat"),
                    rs.getBigDecimal("parada_destino_lon"),
                    null,
                    null
            ));
            return tramos;
        });
    }
}
