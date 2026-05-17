package com.ms_envios.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validación @Valid ──
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("[HANDLER] Validacion fallida en {}: {}", request.getRequestURI(), mensaje);
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", mensaje, request);
    }

    // ── JSON malformado o enum inválido ──
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonInvalido(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("[HANDLER] JSON invalido en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_JSON",
                "Valor de estado invalido. Valores aceptados: " +
                "PENDIENTE, EN_PREPARACION, DESPACHADO, EN_CAMINO, ENTREGADO, CANCELADO",
                request);
    }

    // ── Excepción de dominio: Envio no encontrado ──
    @ExceptionHandler(EnvioNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEnvioNotFound(
            EnvioNotFoundException ex,
            HttpServletRequest request) {

        log.warn("[HANDLER] Envio no encontrado en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    // ── Excepción de dominio: transicion de estado invalida ──
    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleTransicionInvalida(
            TransicionEstadoInvalidaException ex,
            HttpServletRequest request) {

        log.warn("[HANDLER] Transicion invalida en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION",
                ex.getMessage(), request);
    }

    // ── Violación de integridad en BD ─
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.error("[HANDLER] Violacion de integridad en {}: {}",
                request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "Ya existe un envio registrado para ese pedido", request);
    }

    // ── Estado de negocio inválido ──
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request) {

        log.warn("[HANDLER] Estado invalido en {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "INVALID_STATE", ex.getMessage(), request);
    }

    // ── Argumento inválido ──
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenerico(
            Exception ex,
            HttpServletRequest request) {

        log.error("[HANDLER] Error inesperado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Error interno del servidor", request);
    }

    // ── Builder ──
    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String error, String message, HttpServletRequest request) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(body);
    }
}