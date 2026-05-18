package com.auth_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenClaimsResponseDTO {
    private boolean valido;
    private String email;
    private Long idUsuario;
    private List<String> roles;
    private String mensaje;
}
