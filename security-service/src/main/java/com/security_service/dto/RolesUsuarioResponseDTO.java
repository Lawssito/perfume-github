package com.security_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolesUsuarioResponseDTO {
    private Long idUsuario;
    private List<String> roles;
}
