package com.example.trayecticoservidor;

import com.example.trayecticoservidor.dao.RutaDAO;
import com.example.trayecticoservidor.dto.DesplazamientoImportado;
import com.example.trayecticoservidor.dto.ParadaImportada;
import com.example.trayecticoservidor.dto.RamalImportado;
import com.example.trayecticoservidor.dto.RutaImportada;
import ctpcliente.model.Coordenada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración para la construcción del grafo de recorridos.
 * Verifica la creación de aristas de viaje,
 * el cierre de recorridos circulares,
 * el cálculo de costos,
 * recorridos inactivos y
 * la reconstrucción del grafo sin duplicar registros.
 */
@Transactional
class ConstruirGrafoTest extends AbstractIntegrationTest {

    private static final String RECORRIDO_LINEAL = "T-GRAFO-L-A-1";
    private static final String RECORRIDO_CIRCULAR = "T-GRAFO-C-A-1";
    private static final String RECORRIDO_CIRCULAR_CIERRE = "T-GRAFO-C-CIERRE-A-1";
    private static final String RECORRIDO_INACTIVO = "T-GRAFO-I-A-1";

    @Autowired
    private RutaDAO rutaDAO;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararDatos() {
        crearRecorridoLineal();
        crearRecorridoCircular();
        crearRecorridoCircularConCierreEnUltimaParada();
        crearRecorridoInactivo();

        jdbc.update("""
            UPDATE recorridos
            SET activo = FALSE
            WHERE codigo_ctp = ?
            """, RECORRIDO_INACTIVO);
    }

    /**
     * Un recorrido lineal no debe tener una arista de viaje
     * desde su última parada hasta su primera parada.
     */
    @Test
    void recorridoLinealNoTieneAristaDeCierre() {
        construirGrafo();

        assertEquals(0, contarAristaDeCierre(RECORRIDO_LINEAL));
    }

    /**
     * Un recorrido circular debe tener una arista de viaje
     * desde su última parada hasta su primera parada.
     */
    @Test
    void recorridoCircularTieneAristaDeCierre() {
        construirGrafo();

        assertEquals(1, contarAristaDeCierre(RECORRIDO_CIRCULAR));

        double costo = obtenerCostoAristaDeCierre(RECORRIDO_CIRCULAR);

        assertTrue(Double.isFinite(costo));
        assertEquals(3.0, costo, 0.01);
    }

    /**
     * Ejecutar dos veces la construcción del grafo
     * debe producir exactamente los mismos conteos.
     */
    @Test
    void reconstruirGrafoNoDuplicaRegistros() {
        construirGrafo();
        Map<String, Integer> primeraEjecucion = conteosGrafo();

        construirGrafo();
        Map<String, Integer> segundaEjecucion = conteosGrafo();

        assertEquals(primeraEjecucion, segundaEjecucion);
    }

     /**
         * Verifica que el último tramo de un recorrido circular
         * se calcule correctamente cuando la última parada coincide
         * geográficamente con el punto inicial de la línea.
     */
    @Test
    void recorridoCircularCalculaCorrectamenteElUltimoTramo() {
        construirGrafo();

        double costoUltimoTramo = obtenerCostoArista(
                RECORRIDO_CIRCULAR_CIERRE, 4, 5
        );

        double costoCierre = obtenerCostoArista(
                RECORRIDO_CIRCULAR_CIERRE, 5, 1
        );

        assertEquals(1.5, costoUltimoTramo, 0.01);
        assertEquals(0.0, costoCierre, 0.01);
    }

    @Test
    void recorridoInactivoNoGeneraVerticesNiAristasDeViaje() {
        construirGrafo();

        Long recorridoId = obtenerRecorridoId(RECORRIDO_INACTIVO);

        Integer verticesEnBus = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM vertices
            WHERE recorrido_id = ?
              AND tipo = 'en_bus'
            """, Integer.class, recorridoId);

        Integer aristasViajar = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM aristas
            WHERE recorrido_id = ?
              AND tipo = 'viajar'
            """, Integer.class, recorridoId);

        assertEquals(0, verticesEnBus);
        assertEquals(0, aristasViajar);
    }

    private void construirGrafo() {
        jdbc.execute("SELECT construir_grafo()");
    }

    private int contarAristaDeCierre(String codigoRecorrido) {
        Long recorridoId = obtenerRecorridoId(codigoRecorrido);

        return jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM aristas a
                JOIN vertices vo ON vo.id = a.source
                JOIN vertices vd ON vd.id = a.target
                WHERE a.tipo = 'viajar'
                  AND a.recorrido_id = ?
                  AND vo.tipo = 'en_bus'
                  AND vd.tipo = 'en_bus'
                  AND vo.orden_parada = (
                      SELECT MAX(v.orden_parada)
                      FROM vertices v
                      WHERE v.recorrido_id = a.recorrido_id
                        AND v.tipo = 'en_bus'
                  )
                  AND vd.orden_parada = (
                      SELECT MIN(v.orden_parada)
                      FROM vertices v
                      WHERE v.recorrido_id = a.recorrido_id
                        AND v.tipo = 'en_bus'
                  )
                """, Integer.class, recorridoId);
    }

    private double obtenerCostoAristaDeCierre(String codigoRecorrido) {
        Long recorridoId = obtenerRecorridoId(codigoRecorrido);

        return jdbc.queryForObject("""
                SELECT a.cost
                FROM aristas a
                JOIN vertices vo ON vo.id = a.source
                JOIN vertices vd ON vd.id = a.target
                WHERE a.tipo = 'viajar'
                  AND a.recorrido_id = ?
                  AND vo.tipo = 'en_bus'
                  AND vd.tipo = 'en_bus'
                  AND vo.orden_parada = (
                      SELECT MAX(v.orden_parada)
                      FROM vertices v
                      WHERE v.recorrido_id = a.recorrido_id
                        AND v.tipo = 'en_bus'
                  )
                  AND vd.orden_parada = (
                      SELECT MIN(v.orden_parada)
                      FROM vertices v
                      WHERE v.recorrido_id = a.recorrido_id
                        AND v.tipo = 'en_bus'
                  )
                """, Double.class, recorridoId);
    }

    private double obtenerCostoArista(
            String codigoRecorrido,
            int ordenOrigen,
            int ordenDestino
    ) {
        Long recorridoId = obtenerRecorridoId(codigoRecorrido);

        return jdbc.queryForObject("""
            SELECT a.cost
            FROM aristas a
            JOIN vertices vo ON vo.id = a.source
            JOIN vertices vd ON vd.id = a.target
            WHERE a.tipo = 'viajar'
              AND a.recorrido_id = ?
              AND vo.tipo = 'en_bus'
              AND vd.tipo = 'en_bus'
              AND vo.orden_parada = ?
              AND vd.orden_parada = ?
            """, Double.class, recorridoId, ordenOrigen, ordenDestino);
    }

    private Long obtenerRecorridoId(String codigoRecorrido) {
        return jdbc.queryForObject("""
                SELECT id
                FROM recorridos
                WHERE codigo_ctp = ?
                """, Long.class, codigoRecorrido);
    }

    private Map<String, Integer> conteosGrafo() {
        return Map.of(
                "vertices", contar("SELECT COUNT(*) FROM vertices"),
                "aristas", contar("SELECT COUNT(*) FROM aristas")
        );
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private void crearRecorridoLineal() {
        ParadaImportada p1 = parada("T-GRAFO-L-001", 1, 491500.0, 1098200.0);
        ParadaImportada p2 = parada("T-GRAFO-L-002", 2, 492000.0, 1098200.0);
        ParadaImportada p3 = parada("T-GRAFO-L-003", 3, 492500.0, 1098200.0);

        rutaDAO.guardar(ruta(
                999301,
                "T-GRAFO-L",
                List.of(p1, p2, p3),
                List.of(
                        coordenada(p1),
                        coordenada(p2),
                        coordenada(p3)
                )
        ));
    }

    private void crearRecorridoCircular() {
        ParadaImportada p1 = parada("T-GRAFO-C-001", 1, 491500.0, 1098200.0);
        ParadaImportada p2 = parada("T-GRAFO-C-002", 2, 492000.0, 1098200.0);
        ParadaImportada p3 = parada("T-GRAFO-C-003", 3, 492500.0, 1098200.0);

        rutaDAO.guardar(ruta(
                999401,
                "T-GRAFO-C",
                List.of(p1, p2, p3),
                List.of(
                        coordenada(p1),
                        coordenada(p2),
                        coordenada(p3),
                        coordenada(p1)
                )
        ));
    }

    private static RutaImportada ruta(
            long ctpId,
            String codigo,
            List<ParadaImportada> paradas,
            List<Coordenada> coordenadas
    ) {
        var desplazamiento = new DesplazamientoImportado(
                ctpId + 1,
                codigo + "-A-1",
                "1-2",
                1500,
                "activo",
                coordenadas,
                paradas
        );

        var ramal = new RamalImportado(
                ctpId + 2,
                codigo + "-A",
                "RAMAL DE PRUEBA",
                List.of(desplazamiento)
        );

        return new RutaImportada(ctpId, codigo, List.of(ramal));
    }

    private static ParadaImportada parada(
            String codigo,
            int orden,
            double x,
            double y
    ) {
        return new ParadaImportada(
                codigo,
                (long) codigo.hashCode(),
                orden,
                "PARADA " + codigo,
                "1",
                "101",
                "10103",
                x,
                y
        );
    }

    private static Coordenada coordenada(ParadaImportada parada) {
        return new Coordenada(parada.x(), parada.y());
    }

    private void crearRecorridoCircularConCierreEnUltimaParada() {
        ParadaImportada p1 = parada("T-GRAFO-CC-001", 1, 491500.0, 1098200.0);
        ParadaImportada p2 = parada("T-GRAFO-CC-002", 2, 492000.0, 1098200.0);
        ParadaImportada p3 = parada("T-GRAFO-CC-003", 3, 492000.0, 1098700.0);
        ParadaImportada p4 = parada("T-GRAFO-CC-004", 4, 491500.0, 1098700.0);
        ParadaImportada p5 = parada("T-GRAFO-CC-005", 5, 491500.0, 1098200.0);

        rutaDAO.guardar(ruta(
                999501,
                "T-GRAFO-C-CIERRE",
                List.of(p1, p2, p3, p4, p5),
                List.of(
                        coordenada(p1),
                        coordenada(p2),
                        coordenada(p3),
                        coordenada(p4),
                        coordenada(p1)
                )
        ));
    }

    private void crearRecorridoInactivo() {
        var p1 = parada("I-P1", 1, 491500, 1098200);
        var p2 = parada("I-P2", 2, 492000, 1098200);
        var p3 = parada("I-P3", 3, 492500, 1098200);

        rutaDAO.guardar(ruta(
                9003,
                "T-GRAFO-I",
                List.of(p1, p2, p3),
                List.of(
                        coordenada(p1),
                        coordenada(p2),
                        coordenada(p3)
                )
        ));
    }
}