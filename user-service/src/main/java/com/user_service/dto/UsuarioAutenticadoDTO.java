package com.user_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UsuarioAutenticadoDTO {
    private Long idUsuario;
    private String email;
    private List<String> roles;

    public boolean esAdmin() {
        return roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_ADMINISTRADOR"));
    }
}
