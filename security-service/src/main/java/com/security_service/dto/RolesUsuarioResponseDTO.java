package com.security_service.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolesUsuarioResponseDTO {

    @Schema(description = "ID del usuario", example = "1")
    private Long idUsuario;
    private List<String> roles;
}
