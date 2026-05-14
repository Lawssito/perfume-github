package com.auth_service.dto;

import lombok.Data;

@Data
public class AuthResponseDTO {
    private String token;
    private String refreshToken;
    private Long idUsuario;
    private String mensaje;
}
