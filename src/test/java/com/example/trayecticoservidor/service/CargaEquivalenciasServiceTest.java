package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.AbstractIntegrationTest;
import com.example.trayecticoservidor.dao.EquivalenciaRamalDAO;
import com.example.trayecticoservidor.dao.TarifaAresepDAO;
import com.example.trayecticoservidor.model.TarifaAresep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas de {@link CargaEquivalenciasService} con el CSV de prueba
 * {@code equivalencias-prueba.csv} y PostgreSQL en Testcontainers.
 */
@Transactional
class CargaEquivalenciasServiceTest extends AbstractIntegrationTest {

    @Autowired
    private CargaEquivalenciasService servicio;

    @Autowired
    private EquivalenciaRamalDAO dao;

    @Autowired
    private TarifaAresepDAO tarifaDao;

    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path carpetaTemporal;

    @BeforeEach
    void prepararBase() {
        jdbc.update("INSERT INTO rutas (codigo_ctp, id_ctp, nombre) VALUES (?, ?, ?)",
                "TEST-9001", 900001, "Ruta de prueba");
        Long rutaId = jdbc.queryForObject(
                "SELECT id FROM rutas WHERE codigo_ctp = ?", Long.class, "TEST-9001");
        for (int idCtp : new int[] {900001, 900003, 900004, 900006}) {
            jdbc.update("INSERT INTO ramales (ruta_id, codigo_ctp, id_ctp, descripcion) VALUES (?, ?, ?, ?)",
                    rutaId, "TEST-" + idCtp, idCtp, "Ramal de prueba");
        }
        tarifaDao.insertarOActualizarTodas(List.of(
                tarifa("TEST-R9001", "TEST-T9001"),
                tarifa("TEST-R9006", "TEST-T9006")));
    }

    @Test
    void cargaSoloLosNombreExactoDeRamalesImportadosConTarifa() throws IOException {
        CargaEquivalenciasService.Resultado resultado = servicio.cargar(csvDePrueba());

        assertEquals(new CargaEquivalenciasService.Resultado(6, 2, 1, 1, 2), resultado);
        assertEquals(List.of(900001, 900006), jdbc.queryForList(
                "SELECT id_ramal_ctp FROM equivalencias_ramales ORDER BY id_ramal_ctp", Integer.class));
    }

    @Test
    void cargarDosVecesNoDuplica() throws IOException {
        servicio.cargar(csvDePrueba());
        long cantidad = dao.contar();

        servicio.cargar(csvDePrueba());

        assertEquals(cantidad, dao.contar());
    }

    @Test
    void separaCamposEntreComillasConComasYComillasDobles() {
        assertEquals(List.of("a", "b, c", "d \"e\"", ""),
                CargaEquivalenciasService.separarCampos("a,\"b, c\",\"d \"\"e\"\"\","));
    }

    @Test
    void rechazaUnArchivoSinElEncabezadoDelCruce() throws IOException {
        Path archivo = carpetaTemporal.resolve("sin-encabezado.csv");
        Files.writeString(archivo, "otra,cosa\n1,2\n");

        assertThrows(IllegalStateException.class, () -> servicio.cargar(archivo));
    }

    @Test
    void rechazaUnaLineaConCamposDeMas() throws IOException {
        Path archivo = carpetaTemporal.resolve("campos-de-mas.csv");
        Files.writeString(archivo, CargaEquivalenciasService.ENCABEZADO + "\n"
                + "TEST-9001,900001,TEST-9001-A,SAN JOSE, CENTRO,99,TEST-R9001,X,NOMBRE_EXACTO,1.00\n");

        assertThrows(IllegalStateException.class, () -> servicio.cargar(archivo));
    }

    private static Path csvDePrueba() throws IOException {
        return new ClassPathResource("equivalencias-prueba.csv").getFile().toPath();
    }

    private static TarifaAresep tarifa(String codigoRamal, String codigoFraccion) {
        LocalDate vigencia = LocalDate.of(2026, 9, 4);
        return new TarifaAresep("99", "RUTA DE PRUEBA", codigoRamal, "RAMAL DE PRUEBA",
                codigoFraccion, "RAMAL DE PRUEBA", new BigDecimal("5.00"),
                new BigDecimal("350.00"), new BigDecimal("0.00"), "RE-001", vigencia,
                "100", null, vigencia, vigencia, "ET-001", "Operador de prueba",
                "Regular", "San Jose");
    }
}