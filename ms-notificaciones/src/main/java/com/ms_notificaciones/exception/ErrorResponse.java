package com.ms_notificaciones.exception;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String mensaje;
    private List<String> detalles; // Aquí guardaremos los errores específicos, ej: "El mensaje es obligatorio"
}

