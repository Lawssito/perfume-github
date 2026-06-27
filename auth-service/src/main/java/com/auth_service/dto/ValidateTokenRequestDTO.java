package com.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateTokenRequestDTO {

    @Schema(description = "Token JWT a validar", example = "eyJhbGciOiJIUzI1NiIs...")

    @NotBlank(message = "El token es obligatorio")
    private String token;
}
