package com.security_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AsignarRolRequestDTO {

    @Schema(description = "ID del usuario", example = "1")
    @NotNull(message = "El idUsuario es obligatorio")
    private Long idUsuario;

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Schema(description = "Nombre del rol", example = "ROLE_ADMIN")
    private String rolNombre;
}
