package com.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CrearCredencialClientDTO {
    private Long idUsuario;
    private String email;
    private String password;
}
