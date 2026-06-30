package com.user_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("[HANDLER] Recurso no encontrado en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(LocalDateTime.now(), 404, "NOT_FOUND", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> negocio(IllegalStateException ex, HttpServletRequest request) {
        log.warn("[HANDLER] Error de negocio en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(LocalDateTime.now(), 400, "BAD_REQUEST", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> noAutenticado(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("[HANDLER] No autenticado en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(LocalDateTime.now(), 401, "UNAUTHORIZED", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> sinPermisos(ForbiddenException ex, HttpServletRequest request) {
        log.warn("[HANDLER] Acceso denegado en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(LocalDateTime.now(), 403, "FORBIDDEN", ex.getMessage(), request.getRequestURI()));
    }


    @ExceptionHandler(RemoteServiceException.class)
    public ResponseEntity<ErrorResponse> remoto(RemoteServiceException ex, HttpServletRequest request) {
        log.error("[HANDLER] Error de comunicación remota en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(LocalDateTime.now(), 502, "REMOTE_SERVICE_ERROR", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[HANDLER] Error de validación en {}: {}", request.getRequestURI(), msg);
        return ResponseEntity.badRequest().body(new ErrorResponse(LocalDateTime.now(), 400, "VALIDATION_ERROR", msg, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> general(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(LocalDateTime.now(), 500, "INTERNAL_ERROR", ex.getMessage(), request.getRequestURI()));
    }
}
