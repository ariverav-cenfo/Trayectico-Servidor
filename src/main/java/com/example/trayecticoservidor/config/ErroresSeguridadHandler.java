package com.example.trayecticoservidor.config;

import com.example.trayecticoservidor.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Responde los errores de seguridad (401 y 403) con el formato de error de la API
 * y un mensaje genérico. No revela si el token venció, es falso o es de otra aplicación,
 * ni por qué se negó el acceso.
 */
@Component
public class ErroresSeguridadHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    public static final String CODIGO_NO_AUTENTICADO = "NO_AUTENTICADO";
    public static final String CODIGO_ACCESO_DENEGADO = "ACCESO_DENEGADO";
    public static final String MENSAJE_NO_AUTENTICADO = "La sesión no es válida. Inicie sesión de nuevo.";
    public static final String MENSAJE_ACCESO_DENEGADO = "No tiene permiso para realizar esta acción.";

    private final ObjectMapper objectMapper;

    /**
     * Crea el manejador con el ObjectMapper de la aplicación.
     *
     * @param objectMapper convierte la respuesta de error a JSON
     */
    public ErroresSeguridadHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Responde 401 cuando no hay token o el token no es válido.
     * La cabecera WWW-Authenticate va sin error_description para no dar detalles.
     *
     * @param request petición rechazada
     * @param response respuesta donde se escribe el error
     * @param excepcion motivo del rechazo; no se envía al cliente
     * @throws IOException si falla la escritura de la respuesta
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException excepcion) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escribirError(request, response, HttpStatus.UNAUTHORIZED, CODIGO_NO_AUTENTICADO, MENSAJE_NO_AUTENTICADO);
    }

    /**
     * Responde 403 cuando el usuario está autenticado pero no tiene el rol requerido.
     *
     * @param request petición rechazada
     * @param response respuesta donde se escribe el error
     * @param excepcion motivo del rechazo; no se envía al cliente
     * @throws IOException si falla la escritura de la respuesta
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException excepcion) throws IOException {
        escribirError(request, response, HttpStatus.FORBIDDEN, CODIGO_ACCESO_DENEGADO, MENSAJE_ACCESO_DENEGADO);
    }

    /**
     * Escribe el error en formato JSON con la estructura de ErrorResponseDTO.
     */
    private void escribirError(HttpServletRequest request, HttpServletResponse response, HttpStatus estado,
                               String codigo, String mensaje) throws IOException {
        ErrorResponseDTO error = new ErrorResponseDTO(
                estado.value(), codigo, mensaje, List.of(), LocalDateTime.now(), request.getRequestURI());
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}