package com.auth_service.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Respuesta de autenticación con tokens JWT")
public class AuthResponseDTO {

    @Schema(description = "Token JWT de acceso", example = "eyJhbGciOiJIUzI1NiIs...")
    private String token;
    @Schema(description = "Tipo de token", example = "Bearer")
    private String tokenType = "Bearer";
    private Long idUsuario;
    private String email;
    @ArraySchema(schema = @Schema(description = "Roles del usuario", example = "ROLE_USER"))
    private List<String> roles;
    @Schema(description = "Mensaje informativo", example = "Autenticación exitosa")
    private String mensaje;
    @Schema(description = "Token JWT para renovar el access token", example = "eyJhbGciOiJIUzI1NiIs...")
    private String refreshToken;
}
