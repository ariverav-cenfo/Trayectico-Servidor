package com.example.trayecticoservidor.dto;

public record RecorridoAdminDTO (
        long id,
        long ramal_id,
        String codigo_ctp,
        Integer id_ctp,
        String direccion,
        Integer longitud_m,
        String estado_ctp,
        boolean activo
){
}
