package com.ms_stock.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errores.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validacion fallida en request: {}", errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errores);
    }

    // VARIANTE INEXISTENTE
    @ExceptionHandler(VarianteNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlerVarianteNotFound(
        VarianteNotFoundException ex) {

            log.warn("Variante no encontrada: {}", ex.getMessage());

            Map<String, String> respuesta = new HashMap<>();
            respuesta.put("Error", ex.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }
    
    // STOCK INSUFICIENTE 
    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Map<String, String>> handleStockInsuficiente(
        StockInsuficienteException ex) {

        log.warn("Intento de reduccion con stock insuficiente: {}", ex.getMessage());

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("error", ex.getMessage());

    // 409 Conflict → la solicitud es válida pero el estado actual no la permite
        return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {

        // Usamos log.error porque esto no se esperaba — hay que investigarlo
        log.error("Error inesperado en ms-stock: {}", ex.getMessage(), ex);

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("error", "Error interno del servidor");

        // 500 Internal Server Error — nunca exponemos el stack trace al cliente
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
    }
}
