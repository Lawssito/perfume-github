package com.ms_carrito.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidacion(
            MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(e -> errores.put(e.getField(), e.getDefaultMessage()));
        log.warn("[HANDLER] Validacion fallida: {}", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errores);
    }

    @ExceptionHandler(CarritoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCarritoNotFound(
            CarritoNotFoundException ex) {
        log.warn("[HANDLER] Carrito no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
               .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleItemNotFound(
            ItemNotFoundException ex) {
        log.warn("[HANDLER] Item no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
               .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(StockNoDisponibleException.class)
    public ResponseEntity<Map<String, String>> handleStockNoDisponible(
            StockNoDisponibleException ex) {
        log.warn("[HANDLER] Stock no disponible: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
               .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(
            IllegalStateException ex) {
        log.warn("[HANDLER] Estado invalido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
               .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenerico(Exception ex) {
        log.error("[HANDLER] Error inesperado en ms-carrito: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .body(Map.of("error", "Error interno del servidor"));
    }
}
