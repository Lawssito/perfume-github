package com.ms_envios.Exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex){
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(e -> errores.put(e.getField(), e.getDefaultMessage()));
        log.warn("[HANDLER] Validacion fallida en ms-envios: {}", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleJsonInvalido(
            HttpMessageNotReadableException ex) {
        log.warn("[HANDLER] JSON invalido o enum no reconocido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
               .body(Map.of("error", "Valor de estado invalido. Valores aceptados: " +
                    "PENDIENTE, EN_PREPARACION, DESPACHADO, EN_CAMINO, ENTREGADO, CANCELADO"));
    }

    @ExceptionHandler(EnvioNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEnvioNotFound(
            EnvioNotFoundException ex) {
        log.warn("[HANDLER] Envio no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
               .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleTransicionInvalida(
            TransicionEstadoInvalidaException ex) {
        log.warn("[HANDLER] Transicion de estado invalida: {}", ex.getMessage());
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
        log.error("[HANDLER] Error inesperado en ms-envios: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .body(Map.of("error", "Error interno del servidor"));
    }
}
