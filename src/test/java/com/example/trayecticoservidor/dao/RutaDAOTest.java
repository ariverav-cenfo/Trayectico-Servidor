package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.AbstractIntegrationTest;
import com.example.trayecticoservidor.dto.DesplazamientoImportado;
import com.example.trayecticoservidor.dto.ParadaImportada;
import com.example.trayecticoservidor.dto.RamalImportado;
import com.example.trayecticoservidor.dto.RutaImportada;
import ctpcliente.model.Coordenada;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guarda un grupo de dos rutas que comparten una parada, en el Postgres con PostGIS de Testcontainers.
 * A diferencia de {@code RutaDAOIT}, que usa la base local y se corre a mano, esta corre en el CI.
 * Usa códigos que empiezan con T- y {@code @Transactional} revierte al terminar.
 */
@Transactional
class RutaDAOTest extends AbstractIntegrationTest {

    // La misma consulta del README (sección 11), con un WHERE para quedarse con los datos de esta prueba
    private static final String PARADAS_COMPARTIDAS = """
            SELECT s.ctp_code AS parada,
                   s.description AS descripcion,
                   string_agg(DISTINCT r.ctp_code, ', ' ORDER BY r.ctp_code) AS rutas,
                   count(DISTINCT r.id) AS cantidad_rutas
            FROM stop s
            JOIN stop_displacement sd ON sd.stop_id = s.id
            JOIN displacement d ON d.id = sd.displacement_id
            JOIN branch b ON b.id = d.branch_id
            JOIN route r ON r.id = b.route_id
            WHERE s.ctp_code LIKE 'T-%'
            GROUP BY s.id, s.ctp_code, s.description
            HAVING count(DISTINCT r.id) >= 2
            ORDER BY cantidad_rutas DESC, s.ctp_code""";

    @Autowired
    private RutaDAO rutaDAO;

    @Autowired
    private JdbcTemplate jdbc;

    /** Reimportar el grupo completo deja exactamente las mismas filas. */
    @Test
    void reimportarUnGrupoDeRutasNoDuplica() {
        List<RutaImportada> grupo = List.of(rutaA(), rutaB());
        grupo.forEach(rutaDAO::guardar);
        Map<String, Integer> primeraVez = conteos();

        grupo.forEach(rutaDAO::guardar);

        assertEquals(primeraVez, conteos());
        assertEquals(Map.of("rutas", 2, "ramales", 2, "recorridos", 2, "paradas", 3, "paradasEnRecorridos", 4),
                primeraVez);
    }

    /** La parada compartida queda una sola vez en stop y la consulta del README la encuentra con las dos rutas. */
    @Test
    void laParadaCompartidaSeGuardaUnaVezYLaConsultaLaEncuentra() {
        rutaDAO.guardar(rutaA());
        rutaDAO.guardar(rutaB());

        List<Map<String, Object>> filas = jdbc.queryForList(PARADAS_COMPARTIDAS);

        assertEquals(1, filas.size());
        assertEquals("T-PBA-10103-002", filas.getFirst().get("parada"));
        assertEquals("T-0002, T-0090", filas.getFirst().get("rutas"));
        assertEquals(2L, ((Number) filas.getFirst().get("cantidad_rutas")).longValue());
    }

    private Map<String, Integer> conteos() {
        return Map.of(
                "rutas", contar("SELECT count(*) FROM route WHERE ctp_code LIKE 'T-%'"),
                "ramales", contar("SELECT count(*) FROM branch WHERE ctp_code LIKE 'T-%'"),
                "recorridos", contar("SELECT count(*) FROM displacement WHERE ctp_code LIKE 'T-%'"),
                "paradas", contar("SELECT count(*) FROM stop WHERE ctp_code LIKE 'T-%'"),
                "paradasEnRecorridos", contar("""
                        SELECT count(*) FROM stop_displacement sd
                        JOIN displacement d ON d.id = sd.displacement_id
                        WHERE d.ctp_code LIKE 'T-%'"""));
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    // Ruta A: paradas 001 y 002. Ruta B: paradas 002 y 003. Comparten la 002.
    private static RutaImportada rutaA() {
        return ruta(999101, "T-0002", parada("T-PBA-10104-001", 1, 491500.0, 1098200.0),
                parada("T-PBA-10103-002", 2, 491600.0, 1098300.0));
    }

    private static RutaImportada rutaB() {
        return ruta(999201, "T-0090", parada("T-PBA-10103-002", 1, 491600.0, 1098300.0),
                parada("T-PBA-10103-003", 2, 491700.0, 1098400.0));
    }

    private static RutaImportada ruta(long ctpId, String codigo, ParadaImportada p1, ParadaImportada p2) {
        var d = new DesplazamientoImportado(ctpId + 1, codigo + "-A-1", "1-2", 1500, "activo",
                List.of(new Coordenada(p1.x(), p1.y()), new Coordenada(p2.x(), p2.y())),
                List.of(p1, p2));
        var ramal = new RamalImportado(ctpId + 2, codigo + "-A", "RAMAL DE PRUEBA", List.of(d));
        return new RutaImportada(ctpId, codigo, List.of(ramal));
    }

    private static ParadaImportada parada(String codigo, int orden, double x, double y) {
        return new ParadaImportada(codigo, null, orden, "PARADA " + codigo, "1", "101", "10103", x, y);
    }
}
