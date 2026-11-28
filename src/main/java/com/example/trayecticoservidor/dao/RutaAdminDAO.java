package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.dto.RecorridoAdminDTO;
import com.example.trayecticoservidor.dto.RutaAdminDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class RutaAdminDAO {

    private final JdbcTemplate jdbc;

    /**
     * Lista todas las rutas con todos sus campos internos y sus recorridos asociados
     * Usando codigo_ctp ajustado para excluir lo que esté despues del primer - en recorridos
     */

    public List<RutaAdminDTO> obtenerTodasLasRutasAdmin(){
        String sql = """
                SELECT r.id AS ruta_id,
                r.codigo_ctp AS ruta_codigo,
                r.id_ctp AS ruta_id_ctp,
                r.nombre AS ruta_nombre,
                d.id AS rec_id,
                d.ramal_id,
                d.codigo_ctp AS rec_codigo,
                d.id_ctp AS rec_id_ctp,
                d.direccion,
                d.longitud_m,
                d.estado_ctp,
                d.activo
                FROM rutas r
                LEFT JOIN recorridos d ON SPLIT_PART(d.codigo_ctp, '-', 1) = r.codigo_ctp
                ORDER BY r.codigo_ctp, d.codigo_ctp
                """;
        return jdbc.query(sql, rs -> {
            Map<Long, RutaAdminBuilder> rutasMap = new LinkedHashMap<>();

            while (rs.next()) {
                long rutaId = rs.getLong("ruta_id");
                RutaAdminBuilder builder = rutasMap.computeIfAbsent(rutaId, id -> {
                    try {
                        return new RutaAdminBuilder(
                                id,
                                rs.getString("ruta_codigo"),
                                rs.getObject("ruta_id_ctp", Integer.class),
                                rs.getString("ruta_nombre")
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                Long recId = rs.getObject("rec_id", Long.class);
                if (recId != null) {
                    boolean activo = rs.getBoolean("activo");
                    if (activo) {
                        builder.activo = true;
                    }
                    builder.recorridos.add(new RecorridoAdminDTO(
                            recId,
                            rs.getLong("ramal_id"),
                            rs.getString("rec_codigo"),
                            rs.getObject("rec_id_ctp", Integer.class),
                            rs.getString("direccion"),
                            rs.getObject("longitud_m", Integer.class),
                            rs.getString("estado_ctp"),
                            activo
                    ));
                }
            }

            return rutasMap.values().stream()
                    .map(b -> new RutaAdminDTO(b.id, b.codigo_ctp, b.id_ctp, b.nombre, b.activo, b.recorridos))
                    .toList();
        });
    }

   public Optional<RutaAdminDTO> obtenerRutaAdminPorCodigo(String codigo_ctp){
        String sql = """
                SELECT r.id AS ruta_id,
                r.codigo_ctp AS ruta_codigo,
                r.id_ctp AS ruta_id_ctp,
                r.nombre AS ruta_nombre,
                d.id AS rec_id, d.ramal_id, d.codigo_ctp AS rec_codigo,
                d.id_ctp AS rec_id_ctp, d.direccion, d.longitud_m, d.estado_ctp, d.activo
                FROM rutas r
                LEFT JOIN recorridos d ON SPLIT_PART(d.codigo_ctp, '-', 1) = r.codigo_ctp
                WHERE r.codigo_ctp = CAST(? AS varchar)
                ORDER BY d.codigo_ctp
                """;
        return jdbc.query(sql, rs ->{
            if (!rs.next()) return Optional.empty();
            RutaAdminBuilder builder = new RutaAdminBuilder(
                    rs.getLong("ruta_id"),
                    rs.getString("ruta_codigo"),
                    rs.getObject("ruta_id_ctp", Integer.class),
                    rs.getString("ruta_nombre")
            );
            do {
                Long recId = rs.getObject("rec_id", Long.class);
                if (recId != null){
                    boolean activo = rs.getBoolean("activo");
                    if (activo) {
                        builder.activo = true;
                    }
                    builder.recorridos.add(new RecorridoAdminDTO(
                            recId,
                            rs.getLong("ramal_id"),
                            rs.getString("rec_codigo"),
                            rs.getObject("rec_id_ctp", Integer.class),
                            rs.getString("direccion"),
                            rs.getObject("longitud_m", Integer.class),
                            rs.getString("estado_ctp"),
                            activo
                    ));
                }
            }while (rs.next());
            return Optional.of(new RutaAdminDTO(
                    builder.id, builder.codigo_ctp, builder.id_ctp, builder.nombre, builder.activo, builder.recorridos
            ));
        }, codigo_ctp);
    }
    /**
     * Habilita o deshabilita todos los recorridos donde codigo_ctp coincida
     * regresa el numero recorridos actualizados
     */
    @Transactional
    public int actualizarEstadoRecorridosPorRuta(String codigo_ruta, boolean activo){
        String sql = """
                UPDATE recorridos
                SET activo = ?
                WHERE SPLIT_PART(codigo_ctp,'-', 1) = ?
                """;
        return jdbc.update(sql, activo, codigo_ruta);
    }

    private static class RutaAdminBuilder{
        final long id;
        final String codigo_ctp;
        final Integer id_ctp;
        final String nombre;
        boolean activo = false;
        final List<RecorridoAdminDTO> recorridos = new ArrayList<>();

        RutaAdminBuilder(Long id, String codigo_ctp, Integer id_ctp, String nombre){
            this.id = id;
            this.codigo_ctp = codigo_ctp;
            this.id_ctp = id_ctp;
            this.nombre = nombre;
        }
    }
}
