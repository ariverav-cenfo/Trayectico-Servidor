package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.dto.DesplazamientoImportado;
import com.example.trayecticoservidor.dto.ParadaImportada;
import com.example.trayecticoservidor.dto.RamalImportado;
import com.example.trayecticoservidor.dto.RutaImportada;
import com.example.trayecticoservidor.util.GeometriaWkt;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

// Guarda una ruta importada en las tablas de la V2:
// route, branch, displacement, stop y stop_displacement.
//
// 1.Se puede correr varias veces: ON CONFLICT (ctp_code) DO UPDATE actualiza en lugar de duplicar.
//   Tiene que ser DO UPDATE: con DO NOTHING, RETURNING id no devuelve nada si la fila ya existe.
// 2.La columna active no se toca: es el interruptor del administrador, no un dato del CTP.
// 3.El orden de paradas de cada recorrido se reemplaza completo.
@Repository
public class RutaDAO {

    private final JdbcTemplate jdbc;

    public RutaDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void guardar(RutaImportada ruta) {
        long rutaId = jdbc.queryForObject("""
                INSERT INTO route (ctp_code, ctp_id) VALUES (?, ?)
                ON CONFLICT (ctp_code) DO UPDATE SET ctp_id = EXCLUDED.ctp_id
                RETURNING id""",
                Long.class, ruta.codigo(), ruta.ctpId());

        for (RamalImportado ramal : ruta.ramales()) {
            long ramalId = jdbc.queryForObject("""
                    INSERT INTO branch (route_id, ctp_code, ctp_id, description)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (ctp_code) DO UPDATE SET route_id = EXCLUDED.route_id,
                        ctp_id = EXCLUDED.ctp_id, description = EXCLUDED.description
                    RETURNING id""",
                    Long.class, rutaId, ramal.codigo(), ramal.ctpId(), ramal.descripcion());

            for (DesplazamientoImportado d : ramal.desplazamientos()) {
                guardarDesplazamiento(ramalId, d);
            }
        }
    }

    private void guardarDesplazamiento(long ramalId, DesplazamientoImportado d) {
        long despId = jdbc.queryForObject("""
                INSERT INTO displacement
                    (branch_id, ctp_code, ctp_id, direction, length_m, ctp_status, geom)
                VALUES (?, ?, ?, ?, ?, ?, ST_GeomFromText(?::text, 5367))
                ON CONFLICT (ctp_code) DO UPDATE SET branch_id = EXCLUDED.branch_id,
                    ctp_id = EXCLUDED.ctp_id, direction = EXCLUDED.direction,
                    length_m = EXCLUDED.length_m, ctp_status = EXCLUDED.ctp_status,
                    geom = EXCLUDED.geom
                RETURNING id""",
                Long.class, ramalId, d.codigo(), d.ctpId(), d.sentido(), d.longitudM(),
                d.estadoCtp(), GeometriaWkt.linea(d.trazado()));

        // Se reemplaza el orden completo: si el CTP quitó una parada, no queda colgando.
        jdbc.update("DELETE FROM stop_displacement WHERE displacement_id = ?", despId);

        for (ParadaImportada p : d.paradas()) {
            // La misma parada sale en muchos recorridos: se inserta una vez y se reutiliza.
            long paradaId = jdbc.queryForObject("""
                    INSERT INTO stop
                        (ctp_code, ctp_id, description, province, canton, district, geom)
                    VALUES (?, ?, ?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 5367))
                    ON CONFLICT (ctp_code) DO UPDATE SET
                        ctp_id = COALESCE(EXCLUDED.ctp_id, stop.ctp_id),
                        description = EXCLUDED.description, geom = EXCLUDED.geom
                    RETURNING id""",
                    Long.class, p.codigo(), p.ctpId(), p.descripcion(), p.provincia(),
                    p.canton(), p.distrito(), p.x(), p.y());

            jdbc.update("""
                    INSERT INTO stop_displacement (displacement_id, stop_id, stop_order)
                    VALUES (?, ?, ?)""",
                    despId, paradaId, p.orden());
        }
    }
}
