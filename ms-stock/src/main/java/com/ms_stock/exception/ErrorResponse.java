package com.ms_stock.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Contrato estándar de error para todos los microservicios.
 * Todos los servicios deben retornar exactamente esta estructura.
 */

@Data
@Builder
public class ErrorResponse {
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private int    status;
    private String error;    // nombre del código HTTP: NOT_FOUND, CONFLICT, etc.
    private String message;
    private String path; 
}
