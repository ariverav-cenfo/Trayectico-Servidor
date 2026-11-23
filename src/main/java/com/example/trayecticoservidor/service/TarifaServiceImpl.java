package com.example.trayecticoservidor.service;

import com.example.trayecticoservidor.dao.EquivalenciaRamalDAO;
import com.example.trayecticoservidor.dto.TarifaRamalDTO;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementación del servicio de tarifas. Lee las tarifas a través de las equivalencias
 * entre los ramales del CTP y los de ARESEP (SCRUM-91) y las convierte al formato de la API.
 */
@Service
public class TarifaServiceImpl implements TarifaService {

    private final EquivalenciaRamalDAO equivalenciaRamalDAO;

    /**
     * Construye el servicio con el DAO de equivalencias.
     *
     * @param equivalenciaRamalDAO DAO que consulta las tarifas de los ramales.
     */
    public TarifaServiceImpl(EquivalenciaRamalDAO equivalenciaRamalDAO) {
        this.equivalenciaRamalDAO = equivalenciaRamalDAO;
    }

    @Override
    public Map<String, TarifaRamalDTO> obtenerTarifasDeRuta(String codigoRuta) {
        Map<String, TarifaRamalDTO> tarifas = new HashMap<>();
        equivalenciaRamalDAO.buscarTarifasPorCodigoRuta(codigoRuta)
                .forEach((codigoRamal, tarifa) -> tarifas.put(codigoRamal, TarifaRamalDTO.desde(tarifa)));
        return tarifas;
    }
}