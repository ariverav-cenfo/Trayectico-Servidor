package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.EquivalenciaRamalDAO;
import com.example.trayecticoservidor.model.EquivalenciaRamal;
import com.example.trayecticoservidor.model.TipoCoincidencia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Carga en {@code equivalencias_ramales} el CSV que genera el cruce de tarifas (SCRUM-36).
 *
 * <p>Solo carga las filas {@code NOMBRE_EXACTO} de ramales que ya están importados en {@code ramales}
 * y que tienen tarifa en {@code tarifas_aresep}. Las demás se cuentan por motivo y se reportan
 * en el log (SCRUM-91).</p>
 */
@Service
public class CargaEquivalenciasService {

    /** Encabezado que escribe {@code CruceTarifasService.aCsv}. */
    static final String ENCABEZADO = "codigo_ruta_ctp,id_ramal_ctp,codigo_ramal_ctp,descripcion_ctp,"
            + "codigo_ruta_aresep,codigo_ramal_aresep,nombre_ramal_aresep,tipo_coincidencia,puntaje";

    private static final int CAMPOS = 9;
    private static final Logger log = LoggerFactory.getLogger(CargaEquivalenciasService.class);

    private final EquivalenciaRamalDAO dao;

    /**
     * Construye el servicio con el DAO de equivalencias.
     *
     * @param dao DAO de la tabla {@code equivalencias_ramales}.
     */
    public CargaEquivalenciasService(EquivalenciaRamalDAO dao) {
        this.dao = dao;
    }

    /**
     * Lee el CSV del cruce, filtra las filas que se pueden cargar y las guarda.
     * Se puede correr varias veces: no duplica filas.
     *
     * @param archivo ruta del CSV, por ejemplo {@code data/equivalencias-ramales.csv}.
     * @return conteos de la carga.
     */
    public Resultado cargar(Path archivo) {
        List<EquivalenciaRamal> filas = leer(archivo);
        Set<Integer> ramalesImportados = dao.buscarIdsRamalesCtp();
        Set<String> ramalesConTarifa = dao.buscarCodigosRamalAresepConTarifa();

        List<EquivalenciaRamal> aCargar = new ArrayList<>();
        int porTipo = 0;
        int porRamalNoImportado = 0;
        int sinTarifa = 0;
        for (EquivalenciaRamal fila : filas) {
            if (fila.tipo() != TipoCoincidencia.NOMBRE_EXACTO) {
                porTipo++;
            } else if (!ramalesImportados.contains(fila.idRamalCtp())) {
                porRamalNoImportado++;
            } else if (!ramalesConTarifa.contains(fila.codigoRamalAresep())) {
                sinTarifa++;
            } else {
                aCargar.add(fila);
            }
        }
        dao.insertarOActualizarTodas(aCargar);

        Resultado resultado = new Resultado(filas.size(), porTipo, porRamalNoImportado, sinTarifa, aCargar.size());
        log.info("Carga de equivalencias terminada: {} filas leidas, {} cargadas. Descartadas: {} por tipo "
                        + "de coincidencia, {} por ramal no importado, {} sin tarifa en ARESEP.",
                resultado.leidas(), resultado.cargadas(), resultado.descartadasPorTipo(),
                resultado.descartadasPorRamalNoImportado(), resultado.descartadasSinTarifa());
        return resultado;
    }

    /**
     * Lee el CSV completo en UTF-8 y convierte cada fila en una equivalencia.
     *
     * @param archivo ruta del CSV.
     * @return filas del archivo, sin el encabezado.
     */
    List<EquivalenciaRamal> leer(Path archivo) {
        List<String> lineas;
        try {
            lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + archivo, e);
        }
        if (lineas.isEmpty() || !lineas.getFirst().replace("\uFEFF", "").strip().equals(ENCABEZADO)) {
            throw new IllegalStateException("El archivo " + archivo + " no tiene el encabezado del cruce de tarifas");
        }
        List<EquivalenciaRamal> filas = new ArrayList<>();
        for (int i = 1; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            if (!linea.isBlank()) {
                filas.add(aEquivalencia(separarCampos(linea), i + 1));
            }
        }
        return filas;
    }

    /**
     * Separa una línea del CSV en campos. Entiende las comillas que agrega
     * {@code CruceTarifasService}: un campo con coma va entre comillas, y una comilla
     * dentro del campo se escribe doble.
     *
     * @param linea línea del CSV, sin el salto de línea.
     * @return campos de la línea, sin las comillas.
     */
    static List<String> separarCampos(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (entreComillas) {
                if (c == '"' && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else if (c == '"') {
                    entreComillas = false;
                } else {
                    actual.append(c);
                }
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == ',') {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos;
    }

    private static EquivalenciaRamal aEquivalencia(List<String> c, int numeroLinea) {
        if (c.size() != CAMPOS) {
            throw new IllegalStateException("La linea " + numeroLinea + " tiene " + c.size()
                    + " campos y se esperaban " + CAMPOS);
        }
        return new EquivalenciaRamal(c.get(0), Integer.parseInt(c.get(1)), c.get(2), c.get(3),
                vacioANulo(c.get(4)), vacioANulo(c.get(5)), vacioANulo(c.get(6)),
                TipoCoincidencia.valueOf(c.get(7)), Double.parseDouble(c.get(8)));
    }

    /** El cruce escribe los valores nulos como campos vacíos. */
    private static String vacioANulo(String valor) {
        return valor.isEmpty() ? null : valor;
    }

    /**
     * Conteos de una carga de equivalencias.
     *
     * @param leidas                         filas del CSV, sin el encabezado.
     * @param descartadasPorTipo             filas que no son {@code NOMBRE_EXACTO}.
     * @param descartadasPorRamalNoImportado filas {@code NOMBRE_EXACTO} cuyo ramal no está en {@code ramales}.
     * @param descartadasSinTarifa           filas cuyo ramal de ARESEP no tiene tarifa importada.
     * @param cargadas                       filas guardadas en {@code equivalencias_ramales}.
     */
    public record Resultado(int leidas, int descartadasPorTipo, int descartadasPorRamalNoImportado,
                            int descartadasSinTarifa, int cargadas) {
    }
}