package com.user_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {

    @Schema(description = "ID del usuario", example = "1")
    private Long idUsuario;
    @Schema(example = "usuario@email.com")
    private String email;
    @Schema(example = "Juan Pérez")
    private String nombre;
    @Schema(example = "+56912345678")
    private String telefono;
    private String estado;
}
