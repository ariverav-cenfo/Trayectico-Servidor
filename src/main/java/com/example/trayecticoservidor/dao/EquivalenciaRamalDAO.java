package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.model.EquivalenciaRamal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
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
     * Cantidad de equivalencias guardadas.
     *
     * @return número de filas de {@code equivalencias_ramales}.
     */
    public long contar() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM equivalencias_ramales", Long.class);
    }
}