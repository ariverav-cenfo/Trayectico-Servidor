package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.model.TarifaAresep;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DAO encargado de insertar y actualizar las tarifas ARESEP en la base de datos.
 */
@Repository
public class TarifaAresepDAO {
    private final JdbcTemplate jdbc;

    private static final String UPSERT = """
        INSERT INTO tarifas_aresep (codigo_ruta, nombre_ruta, codigo_ramal, nombre_ramal, codigo_fraccion, nombre_fraccion, kilometros_promedio_viaje, tarifa_regular, tarifa_adulto_mayor, resolucion, fecha_resolucion, numero_gaceta, suplemento_gaceta, fecha_gaceta, fecha_vigencia, numero_expediente, operadores, tipo_servicio, provincia)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (codigo_fraccion) DO UPDATE SET
        codigo_ruta = EXCLUDED.codigo_ruta,
        nombre_ruta = EXCLUDED.nombre_ruta,
        codigo_ramal = EXCLUDED.codigo_ramal,
        nombre_ramal = EXCLUDED.nombre_ramal,
        nombre_fraccion = EXCLUDED.nombre_fraccion,
        kilometros_promedio_viaje = EXCLUDED.kilometros_promedio_viaje,
        tarifa_regular = EXCLUDED.tarifa_regular,
        tarifa_adulto_mayor = EXCLUDED.tarifa_adulto_mayor,
        resolucion = EXCLUDED.resolucion,
        fecha_resolucion = EXCLUDED.fecha_resolucion,
        numero_gaceta = EXCLUDED.numero_gaceta,
        suplemento_gaceta = EXCLUDED.suplemento_gaceta,
        fecha_gaceta = EXCLUDED.fecha_gaceta,
        fecha_vigencia = EXCLUDED.fecha_vigencia,
        numero_expediente = EXCLUDED.numero_expediente,
        operadores = EXCLUDED.operadores,
        tipo_servicio = EXCLUDED.tipo_servicio,
        provincia = EXCLUDED.provincia,
        fecha_importacion = CURRENT_TIMESTAMP
        WHERE EXCLUDED.fecha_vigencia >= tarifas_aresep.fecha_vigencia
        """;

    /**
     * Construye el DAO utilizando el acceso a la base de datos.
     *
     * @param jdbc JdbcTemplate utilizado para ejecutar las consultas SQL.
     */
    public TarifaAresepDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // A failed batch rolls back the entire import. Download and validation happen before this transaction.
    /**
     * Inserta o actualiza un conjunto de tarifas ARESEP en una única operación transaccional.
     *
     * @param tarifas Lista de tarifas que se van a insertar o actualizar.
     */
    @Transactional
    public void insertarOActualizarTodas(List<TarifaAresep> tarifas) {
        jdbc.batchUpdate(UPSERT, tarifas, 250, (statement, tarifa) -> {
            statement.setObject(1, tarifa.codigoRuta());
            statement.setObject(2, tarifa.nombreRuta());
            statement.setObject(3, tarifa.codigoRamal());
            statement.setObject(4, tarifa.nombreRamal());
            statement.setObject(5, tarifa.codigoFraccion());
            statement.setObject(6, tarifa.nombreFraccion());
            statement.setObject(7, tarifa.kilometrosPromedioViaje());
            statement.setObject(8, tarifa.tarifaRegular());
            statement.setObject(9, tarifa.tarifaAdultoMayor());
            statement.setObject(10, tarifa.resolucion());
            statement.setObject(11, tarifa.fechaResolucion());
            statement.setObject(12, tarifa.numeroGaceta());
            statement.setObject(13, tarifa.suplementoGaceta());
            statement.setObject(14, tarifa.fechaGaceta());
            statement.setObject(15, tarifa.fechaVigencia());
            statement.setObject(16, tarifa.numeroExpediente());
            statement.setObject(17, tarifa.operadores());
            statement.setObject(18, tarifa.tipoServicio());
            statement.setObject(19, tarifa.provincia());
        });
    }
}