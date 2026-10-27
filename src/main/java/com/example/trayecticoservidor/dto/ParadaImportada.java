package com.example.trayecticoservidor.dto;

public record ParadaImportada(String codigo, Long ctpId, int orden, String descripcion,
                              String provincia, String canton, String distrito,
                              double x, double y) {
}
