package com.example.trayecticoservidor.dto;

import java.util.List;

public record RutaAdminDTO (
        long id,
        String codigo_ctp,
        Integer id_ctp,
        String nombre,
        boolean activo,
        List<RecorridoAdminDTO> recorridos
){
}
