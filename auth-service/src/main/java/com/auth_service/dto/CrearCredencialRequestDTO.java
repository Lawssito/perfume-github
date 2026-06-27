package com.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearCredencialRequestDTO {

    @Schema(description = "ID del usuario", example = "1")
    @NotNull(message = "El idUsuario es obligatorio")
    private Long idUsuario;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene formato valido")
    private String email;

    @NotBlank(message = "La password es obligatoria")
    @Schema(description = "Contraseña del usuario", example = "MiPassword123")
    private String password;

}
