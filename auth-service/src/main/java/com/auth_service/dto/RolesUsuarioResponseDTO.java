package com.auth_service.dto;

import  java.util.List;

import lombok.Data;

@Data
public class RolesUsuarioResponseDTO {
    private Long idUsuario;
    private List<String> roles;
}
