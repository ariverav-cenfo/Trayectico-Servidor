package com.example.trayecticoservidor.dao;

import com.example.trayecticoservidor.dto.DesplazamientoImportado;
import com.example.trayecticoservidor.dto.ParadaImportada;
import com.example.trayecticoservidor.dto.RamalImportado;
import com.example.trayecticoservidor.dto.RutaImportada;
import ctpcliente.model.Coordenada;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Necesita la base local levantada (docker compose) y con la V2 aplicada
// Se corre a mano. Usa códigos que empiezan con T- para no chocar con datos reales, y @Transactional revierte al terminar
@SpringBootTest
@Transactional
class RutaDAOIT {

    @Autowired RutaDAO rutaDAO;
    @Autowired JdbcTemplate jdbc;

    @Test
    void guardarDosVecesNoDuplica() {
        RutaImportada ruta = rutaDePrueba();
        rutaDAO.guardar(ruta);
        rutaDAO.guardar(ruta);

        assertEquals(1, contar("SELECT count(*) FROM route WHERE ctp_code = 'T-0002'"));
        assertEquals(2, contar("SELECT count(*) FROM stop WHERE ctp_code LIKE 'T-PBA-%'"));
        assertEquals(2, contar("""
                SELECT count(*) FROM stop_displacement sd
                JOIN displacement d ON d.id = sd.displacement_id
                WHERE d.ctp_code = 'T-0002-A-1'"""));
        assertEquals(5367, contar(
                "SELECT ST_SRID(geom) FROM displacement WHERE ctp_code = 'T-0002-A-1'"));
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private RutaImportada rutaDePrueba() {
        var p1 = new ParadaImportada("T-PBA-10104-001", null, 1, "PARADA 1",
                "1", "101", "10104", 491500.0, 1098200.0);
        var p2 = new ParadaImportada("T-PBA-10104-002", null, 2, "PARADA 2",
                "1", "101", "10104", 491600.0, 1098300.0);
        var d = new DesplazamientoImportado(999001, "T-0002-A-1", "1-2", 1500, "activo",
                List.of(new Coordenada(491500.0, 1098200.0), new Coordenada(491600.0, 1098300.0)),
                List.of(p1, p2));
        var ramal = new RamalImportado(999002, "T-0002-A", "RAMAL DE PRUEBA", List.of(d));
        return new RutaImportada(999003, "T-0002", List.of(ramal));
    }
}
