package com.example.trayecticoservidor.dao;
import com.example.trayecticoservidor.dto.DesplazamientoResumenDTO;
import com.example.trayecticoservidor.dto.RamalResumenDTO;
import com.example.trayecticoservidor.dto.RutaDetalleDTO;
import com.example.trayecticoservidor.dto.RutaResumenDTO;
import ctpcliente.model.Ramal;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/*
 DAO encargado exclusivamente de la lectura del catálogo de rutas.
 Utiliza JdbcTemplate para ejecutar consultas SQL nativas sobre las tablas
 del esquema V2 (route, branch, displacement).
 */
@Repository
public class RutaLecturaDAO {
    private final JdbcTemplate jdbc;

    /*
     Obtiene una lista resumida de todas las rutas disponibles en el sistema.
     Propósito: Alimentar la vista principal de Angular con el código y nombre
     de las rutas de manera rápida y ligera, sin cargar ramales ni desplazamientos.
     @return Lista de RutaResumenDTO ordenada alfabéticamente por el código del CTP.
     */
    public RutaLecturaDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<RutaResumenDTO> obtenerRutas() {
        String sql = """
            SELECT r.ctp_code, 
                   COALESCE(r.name, (
                       SELECT b.description 
                       FROM branch b 
                       WHERE b.route_id = r.id 
                       ORDER BY b.ctp_code 
                       LIMIT 1
                   )) AS name
            FROM route r
            ORDER BY r.ctp_code
        """;

        return jdbc.query(sql,
                (rs, rowNum) -> new RutaResumenDTO(
                        rs.getString("ctp_code"),
                        rs.getString("name"))
        );
    }

    /*
     Obtiene el detalle completo de una ruta específica, incluyendo sus ramales
     y los desplazamientos (recorridos) asociados a cada ramal.
     Propósito: Resolver el endpoint GET /rutas/{codigo} con una única ida a la base
     de datos, optimizando el rendimiento.
     @param codigo El código CTP de la ruta (ej. "0002").
     @return RutaDetalleDTO con la jerarquía completa, o null si la ruta no existe.
     */
    public RutaDetalleDTO obtenerRutaDetalle(String codigo) {

        String sql = """
                SELECT r.ctp_code as route_code, r.name as route_name,
                b.ctp_code as branch_code, b.description as branch_name,
                d.ctp_code as desp_code, d.direction as desp_direction, d.active as desp_active
                FROM route r
                LEFT JOIN branch b ON b.route_id = r.id
                LEFT JOIN displacement d ON d.branch_id = b.id
                WHERE r.ctp_code = ?
                ORDER BY b.ctp_code, d.ctp_code
                """;
        // Usamos ResultSetExtractor (rs -> { ... }) porque una sola ruta generará múltiples filas en el ResultSet (una por cada desplazamiento).
        // Necesitamos procesar todas las filas juntas para agrupar los datos en una sola estructura jerárquica.
        return jdbc.query(sql, rs -> {
            // Si el ResultSet está vacío, significa que la ruta no existe (404 Not Found)
            if (!rs.next()) return null;

            // Leemos los datos base de la ruta (son iguales en todas las filas del ResultSet)
            String routeName = rs.getString("route_name");

            // Si la ruta no tiene nombre, heredamos el nombre del primer ramal (que viene en esta primera fila)
            if (routeName == null) {
                routeName = rs.getString("branch_name");
            }

            // Usamos un LinkedHashMap para agrupar los ramales por su código
            // manteniendo el orden en el que vienen de la base de datos (ORDER BY b.ctp_code)
            Map<String, RamalBuilder> ramalesMap = new LinkedHashMap<>();

            // Iteramos sobre todas las filas resultantes del JOIN
            do {
                String branchCode = rs.getString("branch_code");

                // Si la ruta tiene ramales, procesamos el ramal
                if (branchCode != null) {
                    // Si es la primera vez que vemos este ramal, lo añadimos al mapa
                    ramalesMap.putIfAbsent(branchCode, new RamalBuilder(branchCode, rs.getString("branch_name")));

                    String despCode = rs.getString("desp_code");

                    // Si el ramal tiene desplazamientos, los añadimos a la lista de ese ramal específico
                    if (despCode != null) {
                        ramalesMap.get(branchCode).desplazamientos.add(new DesplazamientoResumenDTO(
                                despCode, rs.getString("desp_direction"), rs.getBoolean("desp_active")
                        ));
                    }
                }
            } while (rs.next());

            // Convertimos el mapa de constructores temporales a la lista final de DTOs inmutables
            List<RamalResumenDTO> ramales = ramalesMap.values().stream()
                    .map(b -> new RamalResumenDTO(b.codigo, b.nombre, b.desplazamientos))
                    .toList();

            // Retornamos el DTO principal que agrupa todo
            return new RutaDetalleDTO(codigo, routeName, ramales);
        }, codigo);
    }

    /*
     Clase auxiliar interna de alcance privado.
     Propósito: Actuar como un acumulador mutable temporal para los datos del ramal
     mientras se procesan las filas del ResultSet. Al finalizar, se convierte
     en el RamalResumenDTO que es inmutable.
     */
    private static class RamalBuilder{
        String codigo;
        String nombre;
        List<DesplazamientoResumenDTO> desplazamientos = new ArrayList<>();

        RamalBuilder(String codigo, String nombre) {
            this.codigo = codigo;
            this.nombre = nombre;
        }
    }

}
