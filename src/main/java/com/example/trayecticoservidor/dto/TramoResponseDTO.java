package com.example.trayecticoservidor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Proposito: Representa un segmento del viaje ya sea en bus o a pie, los campos de bus van null cuando es a pie
 * @param tipo Tipo de viaje
 * @param distanciaMetros
 * @param geometria GeoJson con la info de las coordenadas
 * @param codigoRuta
 * @param nombreRuta
 * @param codigoRamal
 * @param nombreRamal
 * @param codigoRecorrido
 * @param paradaAbordaje
 * @param paradaDescenso
 * @param tarifa
 * @param aceptaPagoElectronico
 */
public record TramoResponseDTO(
        @JsonProperty("tipo") String tipo,
        @JsonProperty("distancia_metros") Integer distanciaMetros,
        @JsonProperty("geometria") GeoJsonLineStringDTO geometria,
        //Campos especifcos de bus
        @JsonProperty("codigo_ruta") String codigoRuta,
        @JsonProperty("nombre_ruta") String nombreRuta,
        @JsonProperty("codigo_ramal") String codigoRamal,
        @JsonProperty("nombre_ramal") String nombreRamal,
        @JsonProperty("codigo_recorrido") String codigoRecorrido,
        @JsonProperty("parada_abordaje") ParaResponseDTO paradaAbordaje,
        @JsonProperty("parada_descenso") ParaResponseDTO paradaDescenso,
        @JsonProperty("tarifa")BigDecimal tarifa,
        @JsonProperty("acepta_pago_electronico") Boolean aceptaPagoElectronico
        ) {
    //Constructor para los tramos a pie
    public static TramoResponseDTO segmentoCaminata(Integer distanciaMetros, GeoJsonLineStringDTO geometria){
        return new TramoResponseDTO(
                "caminata", distanciaMetros, geometria, null, null, null, null, null,
                null, null,null,null
                );
        //Los campos que no son relevantes para una caminata van null, caminar no requiere de pago electronico, etc.
    }

}
