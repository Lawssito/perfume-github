package com.auth_service.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredencialResponseDTO {

    @Schema(description = "ID de la credencial", example = "1")
    private Long idCredencial;
    @Schema(description = "ID del usuario", example = "1")
    private Long idUsuario;
    @Schema(description = "Email del usuario", example = "usuario@email.com")
    private String email;
    @Schema(description = "Estado de la cuenta", example = "ACTIVO")
    private String estadoCuenta;
    private LocalDateTime creadoEn;

}
