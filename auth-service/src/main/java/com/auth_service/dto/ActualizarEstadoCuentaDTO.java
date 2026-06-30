package com.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActualizarEstadoCuentaDTO {

    @Schema(description = "Nuevo estado de la cuenta", example = "ACTIVO")
    @NotBlank(message = "El estado de cuenta es obligatorio (ACTIVO, INACTIVO)")
    private String estadoCuenta;

}
