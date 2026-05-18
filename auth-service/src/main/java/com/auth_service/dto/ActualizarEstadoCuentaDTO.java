package com.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActualizarEstadoCuentaDTO {
    @NotBlank(message = "El estado de cuenta es obligatorio (ACTIVO, INACTIVO)")
    private String estadoCuenta;

}
