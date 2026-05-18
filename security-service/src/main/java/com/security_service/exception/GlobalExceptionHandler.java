package com.security_service.exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Manejo de validaciones de DTOs (@Valid, @NotBlank, etc)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarExcepcionesDeValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            detalles.add(error.getField() + ": " + error.getDefaultMessage());
        }

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Error de validación en los datos enviados",
                detalles
        );

        log.warn("Petición rechazada por validación (400): {}", detalles);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // 2. Manejo de Tokens Expirados
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> manejarTokenExpirado(ExpiredJwtException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "El token de seguridad ha expirado",
                List.of("Por favor, solicite un nuevo token iniciando sesión nuevamente.")
        );

        log.warn("Intento de acceso con token expirado (401): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    // 3. Manejo de Firmas de Token Inválidas o Manipuladas
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> manejarFirmaInvalida(SignatureException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Firma del token inválida",
                List.of("El token provisto ha sido alterado o no es válido.")
        );

        log.warn("Intento de acceso con firma de token inválida (401): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    // 4. Excepciones genéricas no controladas
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarExcepcionGlobal(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ha ocurrido un error interno en el servidor",
                List.of(ex.getMessage())
        );

        log.error("Error interno no controlado (500): {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}