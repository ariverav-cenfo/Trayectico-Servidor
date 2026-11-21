package com.example.trayecticoservidor.dto;

/**
 * Datos del usuario autenticado que devuelve "GET /usuarios/yo".
 *
 * @param correo correo de la cuenta de Google
 * @param rol rol asignado: ADMIN o USUARIO
 */
public record UsuarioActualDTO(String correo, String rol) {
}