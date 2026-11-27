package com.example.trayecticoservidor.dto;

public record RecorridoAdminDTO (
        long id,
        long ramal_id,
        String codigo_ctp,
        Long id_ctp,
        String direccion,
        Double longitud_m,
        String estado_ctp,
        boolean activo
){
}
