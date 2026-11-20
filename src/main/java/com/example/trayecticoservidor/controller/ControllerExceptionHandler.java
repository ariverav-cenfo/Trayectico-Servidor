package com.example.trayecticoservidor.controller;

import com.example.trayecticoservidor.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Proposito: Clase que maneja los errores de los restcontrollers y genera respuestas genericas
 */
@Slf4j
@RestControllerAdvice
public class ControllerExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDTO> handleInvalidCoordinates(IllegalArgumentException ex, HttpServletRequest request){
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST.value(),
                "COORDENADAS_INVALIDAS",
                ex.getMessage(),
                Collections.emptyList(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(
            Exception ex, HttpServletRequest request) {

        // Respeta el estado de excepciones del framework (404, 405, 415...)
        HttpStatusCode status = ex instanceof ErrorResponse er
                ? er.getStatusCode()
                : HttpStatus.INTERNAL_SERVER_ERROR;

        if (status.is5xxServerError()) {
            log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        }

        ErrorResponseDTO error = new ErrorResponseDTO(
                status.value(),
                status.is5xxServerError() ? "ERROR_INTERNO" : "SOLICITUD_INVALIDA",
                status.is5xxServerError() ? "Ocurrio un error al procesar la solicitud" : ex.getMessage(),
                Collections.emptyList(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(error, status);
    }

}
