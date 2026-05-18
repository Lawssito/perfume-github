package com.security_service.dto;

import lombok.Data;

@Data
public class ValidacionResponseDTO {
    private boolean valido;
    private String mensaje;
    private Long idUsuario; // Útil para que el Gateway sepa quién es y lo inyecte en el request
}