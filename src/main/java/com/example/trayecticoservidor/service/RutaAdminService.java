package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.RutaAdminDAO;
import com.example.trayecticoservidor.dto.RutaAdminDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

@Slf4j
@Service
@RequiredArgsConstructor
public class RutaAdminService {
    private final RutaAdminDAO rutaAdminDAO;

    public List<RutaAdminDTO> listarRutasAdmin(){
        return rutaAdminDAO.obtenerTodasLasRutasAdmin();
    }

    public Optional<RutaAdminDTO>obtenerRutaAdmin(String codigo){
        return rutaAdminDAO.obtenerRutaAdminPorCodigo(codigo);
    }

    @Transactional
    public Optional<RutaAdminDTO> cambiarEstadoRuta(String codigo, boolean activo){
        Optional<RutaAdminDTO> rutaExistente = rutaAdminDAO.obtenerRutaAdminPorCodigo(codigo);
        if (rutaExistente.isEmpty()){
            return Optional.empty();
        }
        int actualizados = rutaAdminDAO.actualizarEstadoRecorridosPorRuta(codigo,activo);
        log.info("Ruta {}: se actualizaron {} recorridos a activo={}", codigo, actualizados, activo);
        return rutaAdminDAO.obtenerRutaAdminPorCodigo(codigo);
    }
}
