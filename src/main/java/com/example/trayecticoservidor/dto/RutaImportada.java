package com.example.trayecticoservidor.dto;


import java.util.List;

public record RutaImportada(long ctpId, String codigo, List<RamalImportado> ramales ) {

}

