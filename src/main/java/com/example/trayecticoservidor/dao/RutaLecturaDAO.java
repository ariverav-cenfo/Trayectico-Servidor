package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.dto.RecorridoResumenDTO;
import com.example.trayecticoservidor.dto.RamalResumenDTO;
import com.example.trayecticoservidor.dto.RutaDetalleDTO;
import com.example.trayecticoservidor.dto.RutaResumenDTO;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * DAO encargado exclusivamente de la lectura del catálogo de rutas.
 * Utiliza JdbcTemplate para ejecutar consultas SQL nativas sobre las tablas
 * del esquema V2 (rutas, ramales, recorridos).
 */
@Repository
public class RutaLecturaDAO {
    private final JdbcTemplate jdbc;

    public RutaLecturaDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Obtiene una lista resumida de todas las rutas disponibles en el sistema.
     * Propósito: Alimentar la vista principal de Angular con el código y nombre
     * de las rutas de manera rápida y ligera, sin cargar ramales ni desplazamientos.
     *
     * @return Lista de RutaResumenDTO ordenada alfabéticamente por el código del CTP.
     *
     * Actualizado para mostrar rutas que tenga recorridos activos(rutas activas)
     */
    public List<RutaResumenDTO> obtenerRutas() {
        String sql = """
            SELECT r.codigo_ctp,
                   COALESCE(r.nombre, (
                       SELECT b.descripcion
                       FROM ramales b
                       WHERE EXISTS(
                           SELECT 1 FROM recorridos d
                                    WHERE SPLIT_PART(d.codigo_ctp, '-', 1) = r.codigo_ctp
                                    AND d.activo = true
                       )
                       ORDER BY b.codigo_ctp
                       LIMIT 1
                   )) AS name
            FROM rutas r
            ORDER BY r.codigo_ctp
        """;

        return jdbc.query(sql,
                (rs, rowNum) -> new RutaResumenDTO(
                        rs.getString("codigo_ctp"),
                        rs.getString("name"))
        );
    }

    /**
     * Obtiene el detalle completo de una ruta específica, incluyendo sus ramales
     * y los desplazamientos (recorridos) asociados a cada ramal.
     * Propósito: Resolver el endpoint GET /rutas/{codigo} con una única ida a la base
     * de datos, optimizando el rendimiento.
     *
     * @param codigo El código CTP de la ruta (ej. "0002").
     * @return RutaDetalleDTO con la jerarquía completa, o null si la ruta no existe.
     */
    public RutaDetalleDTO obtenerRutaDetalle(String codigo) {

        String sql = """
                SELECT r.codigo_ctp as route_code, r.nombre as route_name,
                b.codigo_ctp as branch_code, b.descripcion as branch_name,
                d.codigo_ctp as desp_code, d.direccion as desp_direction, d.activo as desp_active
                FROM rutas r
                LEFT JOIN ramales b ON b.ruta_id = r.id
                LEFT JOIN recorridos d ON d.ramal_id = b.id
                WHERE r.codigo_ctp = ?
                ORDER BY b.codigo_ctp, d.codigo_ctp
                """;

        return jdbc.query(sql, rs -> {
            if (!rs.next()) return null;

            String routeName = rs.getString("route_name");

            if (routeName == null) {
                routeName = rs.getString("branch_name");
            }

            Map<String, RamalBuilder> ramalesMap = new LinkedHashMap<>();

            do {
                String branchCode = rs.getString("branch_code");

                if (branchCode != null) {
                    ramalesMap.putIfAbsent(branchCode, new RamalBuilder(branchCode, rs.getString("branch_name")));

                    String despCode = rs.getString("desp_code");

                    if (despCode != null) {
                        ramalesMap.get(branchCode).desplazamientos.add(new RecorridoResumenDTO(
                                despCode, rs.getString("desp_direction"), rs.getBoolean("desp_active")
                        ));
                    }
                }
            } while (rs.next());

            List<RamalResumenDTO> ramales = ramalesMap.values().stream()
                    // La tarifa la agrega CatalogoRutasServiceImpl; este DAO solo lee el catálogo del CTP.
                    .map(b -> new RamalResumenDTO(b.codigo, b.nombre, null, b.desplazamientos))
                    .toList();

            return new RutaDetalleDTO(codigo, routeName, ramales);
        }, codigo);
    }

    /**
     * Clase auxiliar interna de alcance privado.
     * Propósito: Actuar como un acumulador mutable temporal para los datos del ramal
     * mientras se procesan las filas del ResultSet. Al finalizar, se convierte
     * en el RamalResumenDTO que es inmutable.
     */
    private static class RamalBuilder{
        String codigo;
        String nombre;
        List<RecorridoResumenDTO> desplazamientos = new ArrayList<>();

        RamalBuilder(String codigo, String nombre) {
            this.codigo = codigo;
            this.nombre = nombre;
        }
    }
}