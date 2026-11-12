package com.example.trayecticoservidor.dto;

import ctpcliente.model.Coordenada;

import java.util.List;

public record DesplazamientoImportado(long ctpId, String codigo, String sentido,
                                      Integer longitudM, String estadoCtp,
                                      List<Coordenada> trazado, List<ParadaImportada> paradas) {
}
