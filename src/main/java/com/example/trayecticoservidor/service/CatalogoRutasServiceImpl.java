package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.RutaLecturaDAO;
import com.example.trayecticoservidor.dto.RamalResumenDTO;
import com.example.trayecticoservidor.dto.RutaDetalleDTO;
import com.example.trayecticoservidor.dto.RutaResumenDTO;
import com.example.trayecticoservidor.dto.TarifaRamalDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación del servicio de catálogo de rutas.
 * Se encarga de coordinar el acceso a los datos mediante el DAO correspondiente
 * y de agregar a cada ramal su tarifa de ARESEP (RF-TP-01).
 */
@Service
public class CatalogoRutasServiceImpl implements CatalogoRutasService {

    private final RutaLecturaDAO rutaLecturaDAO;
    private final TarifaService tarifaService;

    /**
     * Construye el servicio con sus dependencias.
     *
     * @param rutaLecturaDAO DAO de lectura del catálogo de rutas.
     * @param tarifaService servicio que consulta las tarifas de los ramales.
     */
    public CatalogoRutasServiceImpl(RutaLecturaDAO rutaLecturaDAO, TarifaService tarifaService) {
        this.rutaLecturaDAO = rutaLecturaDAO;
        this.tarifaService = tarifaService;
    }

    @Override
    public List<RutaResumenDTO> obtenerRutas() {
        return rutaLecturaDAO.obtenerRutas();
    }

    @Override
    public Optional<RutaDetalleDTO> obtenerRutaPorCodigo(String codigo) {
        return Optional.ofNullable(rutaLecturaDAO.obtenerRutaDetalle(codigo))
                .map(this::agregarTarifas);
    }

    /**
     * Agrega a cada ramal de la ruta su tarifa de ARESEP. Los ramales sin tarifa quedan con
     * {@code tarifa} en {@code null}, para que el cliente muestre que no está disponible.
     *
     * @param ruta detalle de la ruta leído del catálogo.
     * @return el mismo detalle, con la tarifa de cada ramal.
     */
    private RutaDetalleDTO agregarTarifas(RutaDetalleDTO ruta) {
        Map<String, TarifaRamalDTO> tarifas = tarifaService.obtenerTarifasDeRuta(ruta.codigoRuta());
        List<RamalResumenDTO> ramales = ruta.ramales().stream()
                .map(ramal -> ramal.conTarifa(tarifas.get(ramal.codigoRamal())))
                .toList();
        return new RutaDetalleDTO(ruta.codigoRuta(), ruta.nombreRuta(), ramales);
    }
}