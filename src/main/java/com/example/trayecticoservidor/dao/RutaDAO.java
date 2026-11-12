package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.dto.DesplazamientoImportado;
import com.example.trayecticoservidor.dto.ParadaImportada;
import com.example.trayecticoservidor.dto.RamalImportado;
import com.example.trayecticoservidor.dto.RutaImportada;
import com.example.trayecticoservidor.util.GeometriaWkt;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

// Guarda una ruta importada en las tablas de la V6:
// rutas, ramales, recorridos, paradas y paradas_recorridos.
//
// 1.Se puede correr varias veces: ON CONFLICT (codigo_ctp) DO UPDATE actualiza en lugar de duplicar.
//   Tiene que ser DO UPDATE: con DO NOTHING, RETURNING id no devuelve nada si la fila ya existe.
// 2.La columna activo no se toca: es el interruptor del administrador, no un dato del CTP.
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
                INSERT INTO rutas (codigo_ctp, id_ctp) VALUES (?, ?)
                ON CONFLICT (codigo_ctp) DO UPDATE SET id_ctp = EXCLUDED.id_ctp
                RETURNING id""",
                Long.class, ruta.codigo(), ruta.ctpId());

        for (RamalImportado ramal : ruta.ramales()) {
            long ramalId = jdbc.queryForObject("""
                    INSERT INTO ramales (ruta_id, codigo_ctp, id_ctp, descripcion)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (codigo_ctp) DO UPDATE SET ruta_id = EXCLUDED.ruta_id,
                        id_ctp = EXCLUDED.id_ctp, descripcion = EXCLUDED.descripcion
                    RETURNING id""",
                    Long.class, rutaId, ramal.codigo(), ramal.ctpId(), ramal.descripcion());

            for (DesplazamientoImportado d : ramal.desplazamientos()) {
                guardarDesplazamiento(ramalId, d);
            }
        }
    }

    private void guardarDesplazamiento(long ramalId, DesplazamientoImportado d) {
        long despId = jdbc.queryForObject("""
                INSERT INTO recorridos
                    (ramal_id, codigo_ctp, id_ctp, direccion, longitud_m, estado_ctp, geometria)
                VALUES (?, ?, ?, ?, ?, ?, ST_GeomFromText(?::text, 5367))
                ON CONFLICT (codigo_ctp) DO UPDATE SET ramal_id = EXCLUDED.ramal_id,
                    id_ctp = EXCLUDED.id_ctp, direccion = EXCLUDED.direccion,
                    longitud_m = EXCLUDED.longitud_m, estado_ctp = EXCLUDED.estado_ctp,
                    geometria = EXCLUDED.geometria
                RETURNING id""",
                Long.class, ramalId, d.codigo(), d.ctpId(), d.sentido(), d.longitudM(),
                d.estadoCtp(), GeometriaWkt.linea(d.trazado()));

        // Se reemplaza el orden completo: si el CTP quitó una parada, no queda colgando.
        jdbc.update("DELETE FROM paradas_recorridos WHERE recorrido_id = ?", despId);

        for (ParadaImportada p : d.paradas()) {
            // La misma parada sale en muchos recorridos: se inserta una vez y se reutiliza.
            long paradaId = jdbc.queryForObject("""
                    INSERT INTO paradas
                        (codigo_ctp, id_ctp, descripcion, provincia, canton, distrito, geometria)
                    VALUES (?, ?, ?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 5367))
                    ON CONFLICT (codigo_ctp) DO UPDATE SET
                        id_ctp = COALESCE(EXCLUDED.id_ctp, paradas.id_ctp),
                        descripcion = EXCLUDED.descripcion, geometria = EXCLUDED.geometria
                    RETURNING id""",
                    Long.class, p.codigo(), p.ctpId(), p.descripcion(), p.provincia(),
                    p.canton(), p.distrito(), p.x(), p.y());

            jdbc.update("""
                    INSERT INTO paradas_recorridos (recorrido_id, parada_id, orden_parada)
                    VALUES (?, ?, ?)""",
                    despId, paradaId, p.orden());
        }
    }
}