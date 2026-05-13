package com.security_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidarAccesoRequestDTO {
    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "El permiso requerido es obligatorio")
    private String permisoRequerido;
}