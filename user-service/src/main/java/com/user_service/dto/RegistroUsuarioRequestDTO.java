package com.user_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistroUsuarioRequestDTO {

    @Schema(example = "usuario@email.com", description = "Correo electrónico del usuario")
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene formato valido")
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String telefono;

    @NotBlank(message = "La password es obligatoria")
    @Schema(description = "Contraseña del usuario", example = "MiPassword123")
    private String password;
}
