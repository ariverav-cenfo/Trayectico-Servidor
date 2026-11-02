package com.example.trayecticoservidor.model;

/**
 * Una fila del cruce: un ramal del CTP y el ramal de ARESEP que le corresponde, si se encontro.
 *
 * <p>Los campos de ARESEP son {@code null} cuando el tipo es {@link TipoCoincidencia#SIN_RUTA_ARESEP}.
 * En {@link TipoCoincidencia#REVISAR} traen el candidato mas parecido, solo como sugerencia.</p>
 *
 * @param codigoRutaCtp     codigo publico de la ruta en el CTP, p. ej. {@code 0002}
 * @param idRamalCtp        id interno del ramal en el CTP
 * @param codigoRamalCtp    codigo publico del ramal en el CTP, p. ej. {@code 0002-A}
 * @param descripcionCtp    origen y destino del ramal segun el CTP
 * @param codigoRutaAresep  codigo de la ruta en ARESEP, p. ej. {@code 02}
 * @param codigoRamalAresep codigo del ramal en ARESEP, p. ej. {@code R0012}
 * @param nombreRamalAresep nombre del ramal en ARESEP
 * @param tipo              tipo de coincidencia del ramal
 * @param puntaje           similitud de los nombres, de 0 a 1
 */
public record EquivalenciaRamal(
        String codigoRutaCtp,
        int idRamalCtp,
        String codigoRamalCtp,
        String descripcionCtp,
        String codigoRutaAresep,
        String codigoRamalAresep,
        String nombreRamalAresep,
        TipoCoincidencia tipo,
        double puntaje) {
}
