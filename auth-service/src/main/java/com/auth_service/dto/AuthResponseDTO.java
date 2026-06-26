package com.auth_service.dto;

import java.util.List;

import lombok.Data;

@Data
public class AuthResponseDTO {
    private String token;
    private String tokenType = "Bearer";
    private Long idUsuario;
    private String email;
    private List<String> roles;
    private String mensaje;
    private String refreshToken;
}
