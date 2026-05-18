package com.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidateTokenRequestDTO {

    @NotBlank(message = "El token es obligatorio")
    private String token;
}
