package com.example.trayecticoservidor.dto;

import java.util.List;

public record RamalImportado(long ctpId, String codigo, String descripcion,
                             List<DesplazamientoImportado> desplazamientos) {

}
