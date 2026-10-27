package com.example.trayecticoservidor.util;

// El código de parada trae el distrito: PBA-10104-054 -> provincia 1, cantón 101, distrito 10104
public record UbicacionParada(String provincia, String canton, String distrito) {

    private static final UbicacionParada VACIA = new UbicacionParada(null, null, null);

    public static UbicacionParada desdeCodigo(String codigoParada) {
        if (codigoParada == null){
            return VACIA;
        }
        //Strip(): el CTP trae espacios sobrantes en algunos códigos
        String[] partes = codigoParada.strip().split("-");
        if (partes.length < 2 || !partes[1].matches("\\d{5}")){
            return VACIA;
        }
        String d = partes[1];
        return new UbicacionParada(d.substring(0, 1), d.substring(0,3), d);
    }
}
