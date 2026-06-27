package com.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequestDTO {

    @Schema(description = "Refresh token JWT", example = "eyJhbGciOiJIUzI1NiIs...")

    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;
}
