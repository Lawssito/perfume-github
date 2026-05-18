package com.security_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PermisoRequestDTO {
    @NotBlank(message = "El nombre del permiso es obligatorio")
    private String nombre;
}