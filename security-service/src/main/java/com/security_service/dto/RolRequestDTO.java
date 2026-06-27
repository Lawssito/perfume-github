package com.security_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RolRequestDTO {

    @Schema(description = "Nombre del rol", example = "ROLE_USER")
    @NotBlank(message = "El nombre del rol es obligatorio")
    private String nombre;
}
