package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.model.EquivalenciaRamal;
import com.example.trayecticoservidor.model.TarifaRamal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


/**
 * DAO de la tabla {@code equivalencias_ramales}: guarda qué ramal de ARESEP corresponde
 * a cada ramal del CTP (SCRUM-91).
 */
@Repository
public class EquivalenciaRamalDAO {

    private static final String UPSERT = """
        INSERT INTO equivalencias_ramales (id_ramal_ctp, codigo_ruta_aresep, codigo_ramal_aresep, tipo_coincidencia, puntaje)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (id_ramal_ctp) DO UPDATE SET
        codigo_ruta_aresep = EXCLUDED.codigo_ruta_aresep,
        codigo_ramal_aresep = EXCLUDED.codigo_ramal_aresep,
        tipo_coincidencia = EXCLUDED.tipo_coincidencia,
        puntaje = EXCLUDED.puntaje
        """;

    /**
     * Tarifa del viaje completo (la más alta) y cantidad de fraccionamientos de un ramal del CTP.
     * Como usa funciones de agregación, siempre devuelve una fila: si el ramal no tiene
     * equivalencia o no tiene tarifas, {@code fracciones} es 0 y la tarifa es NULL.
     */
    private static final String TARIFA_RAMAL = """
        SELECT MAX(t.tarifa_regular) AS tarifa_viaje_completo, COUNT(t.id) AS fracciones
        FROM equivalencias_ramales e
        JOIN tarifas_aresep t ON t.codigo_ramal = e.codigo_ramal_aresep
        WHERE e.id_ramal_ctp = ?
        """;

    /**
     * Tarifa del viaje completo (la más alta) y cantidad de fraccionamientos de cada ramal de una
     * ruta del CTP, en una sola consulta. Los ramales sin equivalencia o sin tarifas no aparecen.
     */
    private static final String TARIFAS_RUTA = """
        SELECT b.codigo_ctp AS codigo_ramal,
               MAX(t.tarifa_regular) AS tarifa_viaje_completo,
               COUNT(t.id) AS fracciones
        FROM rutas r
        JOIN ramales b ON b.ruta_id = r.id
        JOIN equivalencias_ramales e ON e.id_ramal_ctp = b.id_ctp
        JOIN tarifas_aresep t ON t.codigo_ramal = e.codigo_ramal_aresep
        WHERE r.codigo_ctp = ?
        GROUP BY b.codigo_ctp
        """;

    private final JdbcTemplate jdbc;

    /**
     * Construye el DAO con el acceso a la base de datos.
     *
     * @param jdbc JdbcTemplate utilizado para ejecutar las consultas SQL.
     */
    public EquivalenciaRamalDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Inserta o actualiza las equivalencias en una sola transacción. Si el ramal del CTP ya tiene
     * equivalencia, se actualizan los datos de ARESEP, pero {@code acepta_pago_electronico} se
     * conserva, porque se llena a mano.
     *
     * @param equivalencias equivalencias que se van a guardar; cada ramal del CTP debe existir en {@code ramales}.
     */
    @Transactional
    public void insertarOActualizarTodas(List<EquivalenciaRamal> equivalencias) {
        jdbc.batchUpdate(UPSERT, equivalencias, 250, (statement, e) -> {
            statement.setInt(1, e.idRamalCtp());
            statement.setString(2, e.codigoRutaAresep());
            statement.setString(3, e.codigoRamalAresep());
            statement.setString(4, e.tipo().name());
            statement.setDouble(5, e.puntaje());
        });
    }

    /**
     * Ids del CTP de los ramales que ya están importados.
     *
     * @return conjunto de {@code ramales.id_ctp}.
     */
    public Set<Integer> buscarIdsRamalesCtp() {
        return new HashSet<>(jdbc.queryForList("SELECT id_ctp FROM ramales", Integer.class));
    }

    /**
     * Códigos de los ramales de ARESEP que tienen al menos una tarifa importada.
     *
     * @return conjunto de {@code tarifas_aresep.codigo_ramal}.
     */
    public Set<String> buscarCodigosRamalAresepConTarifa() {
        return new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT codigo_ramal FROM tarifas_aresep", String.class));
    }

    /**
     * Busca la tarifa de un ramal del CTP a través de su equivalencia con ARESEP.
     *
     * @param idRamalCtp id del CTP del ramal ({@code ramales.id_ctp}).
     * @return la tarifa del viaje completo y la cantidad de fraccionamientos, o vacío si el ramal
     *         no tiene equivalencia o ARESEP no publica tarifas para él.
     */
    public Optional<TarifaRamal> buscarTarifaRamal(int idRamalCtp) {
        return jdbc.queryForObject(TARIFA_RAMAL, (rs, numeroFila) -> {
            // Sin fracciones no hay tarifa: la fila trae MAX en NULL.
            if (rs.getInt("fracciones") == 0) {
                return Optional.<TarifaRamal>empty();
            }
            return Optional.of(mapearTarifa(rs));
        }, idRamalCtp);
    }


    /**
     * Busca en una sola consulta las tarifas de todos los ramales de una ruta del CTP.
     *
     * @param codigoRuta código del CTP de la ruta ({@code rutas.codigo_ctp}), p. ej. {@code 0002}.
     * @return tarifa por código del CTP del ramal ({@code ramales.codigo_ctp}); los ramales sin
     *         equivalencia o sin tarifas de ARESEP no aparecen. Si la ruta no existe, el mapa está vacío.
     */
    public Map<String, TarifaRamal> buscarTarifasPorCodigoRuta(String codigoRuta) {
        Map<String, TarifaRamal> tarifas = new HashMap<>();
        jdbc.query(TARIFAS_RUTA, rs -> {
            tarifas.put(rs.getString("codigo_ramal"), mapearTarifa(rs));
        }, codigoRuta);
        return tarifas;
    }

    /**
     * Arma la tarifa de un ramal con la fila actual de una consulta que trae las columnas
     * {@code tarifa_viaje_completo} y {@code fracciones}.
     *
     * @param rs fila actual de la consulta.
     * @return la tarifa del ramal.
     * @throws SQLException si no se pueden leer las columnas.
     */
    private static TarifaRamal mapearTarifa(ResultSet rs) throws SQLException {
        return new TarifaRamal(rs.getBigDecimal("tarifa_viaje_completo"), rs.getInt("fracciones"));
    }

    /**
     * Cantidad de equivalencias guardadas.
     *
     * @return número de filas de {@code equivalencias_ramales}.
     */
    public long contar() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM equivalencias_ramales", Long.class);
    }
}