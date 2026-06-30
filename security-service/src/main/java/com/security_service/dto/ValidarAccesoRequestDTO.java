package com.security_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ValidarAccesoRequestDTO {

    @Schema(description = "ID del usuario", example = "1")
    @NotNull(message = "El idUsuario es obligatorio")
    private Long idUsuario;

    @NotBlank(message = "El permiso requerido es obligatorio")
    @Schema(description = "Permiso requerido", example = "ACCESS_ADMIN")
    private String permisoRequerido;
}