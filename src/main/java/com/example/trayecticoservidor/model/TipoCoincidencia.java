package com.example.trayecticoservidor.model;

/**
 * Resultado del cruce entre el catalogo del CTP y el pliego tarifario de ARESEP.
 *
 * <p>Los tres primeros valores califican una ruta; los demas, un ramal.</p>
 */
public enum TipoCoincidencia {

    /** La ruta coincide con el codigo de ARESEP completado con ceros ({@code 02} y {@code 0002}). */
    EXACTA,

    /** La ruta solo coincide por el numero; ARESEP le agrega letras o sufijos ({@code 66 BS}). */
    SOLO_NUMERO,

    /** ARESEP no publica ninguna ruta con ese numero. */
    SIN_COINCIDENCIA,

    /** El nombre normalizado del ramal es igual al de un solo ramal de ARESEP. */
    NOMBRE_EXACTO,

    /** El nombre normalizado es igual al de varios ramales de ARESEP; hay que elegir a mano. */
    NOMBRE_AMBIGUO,

    /** El nombre se parece al de un ramal de ARESEP, pero no es igual; hay que confirmarlo a mano. */
    NOMBRE_PARECIDO,

    /** La ruta existe en ARESEP, pero ningun ramal se parece lo suficiente. */
    REVISAR,

    /** ARESEP no publica la ruta del ramal, asi que no hay tarifa. */
    SIN_RUTA_ARESEP
}
