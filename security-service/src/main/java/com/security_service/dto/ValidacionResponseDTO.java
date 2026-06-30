package com.security_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ValidacionResponseDTO {

    @Schema(description = "Indica si el acceso está permitido", example = "true")
    private boolean valido;
    private String mensaje;
    private Long idUsuario; // Útil para que el Gateway sepa quién es y lo inyecte en el request
}