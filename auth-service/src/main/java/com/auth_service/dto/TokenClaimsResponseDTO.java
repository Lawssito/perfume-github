package com.auth_service.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Claims extraídos de un token JWT")
public class TokenClaimsResponseDTO {

    @Schema(description = "Indica si el token es válido", example = "true")
    private boolean valido;
    @Schema(description = "Email del usuario", example = "usuario@email.com")
    private String email;
    @Schema(description = "ID del usuario", example = "1")
    private Long idUsuario;
    @ArraySchema(schema = @Schema(description = "Roles del usuario", example = "ROLE_USER"))
    private List<String> roles;
    private String mensaje;
}
