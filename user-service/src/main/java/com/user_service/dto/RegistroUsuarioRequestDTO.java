package com.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistroUsuarioRequestDTO {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene formato valido")
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String telefono;

    @NotBlank(message = "La password es obligatoria")
    private String password;
}
