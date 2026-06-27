package com.security_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PermisoRequestDTO {

    @Schema(description = "Nombre del permiso", example = "ACCESS_ADMIN")
    @NotBlank(message = "El nombre del permiso es obligatorio")
    private String nombre;
}