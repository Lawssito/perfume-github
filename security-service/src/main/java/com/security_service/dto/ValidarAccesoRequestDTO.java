package com.security_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ValidarAccesoRequestDTO {
    @NotNull(message = "El idUsuario es obligatorio")
    private Long idUsuario;

    @NotBlank(message = "El permiso requerido es obligatorio")
    private String permisoRequerido;
}