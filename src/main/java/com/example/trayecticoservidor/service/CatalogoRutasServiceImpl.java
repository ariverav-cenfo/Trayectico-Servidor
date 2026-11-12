package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dto.RutaDetalleDTO;
import com.example.trayecticoservidor.dto.RutaResumenDTO;
import com.example.trayecticoservidor.dao.RutaLecturaDAO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de catálogo de rutas.
 * Se encarga de coordinar el acceso a los datos mediante el DAO correspondiente.
 */
@Service
public class CatalogoRutasServiceImpl implements CatalogoRutasService {
    private RutaLecturaDAO rutaLecturaDAO;

    public CatalogoRutasServiceImpl(RutaLecturaDAO rutaLecturaDAO) {
        this.rutaLecturaDAO = rutaLecturaDAO;
    }

    @Override
    public List<RutaResumenDTO> obtenerRutas() {
        return rutaLecturaDAO.obtenerRutas();
    }

    @Override
    public Optional<RutaDetalleDTO> obtenerRutaPorCodigo (String codigo){

        return Optional.ofNullable(rutaLecturaDAO.obtenerRutaDetalle(codigo));
    }

}