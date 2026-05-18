package com.auth_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredencialResponseDTO {
    private Long idCredencial;
    private Long idUsuario;
    private String email;
    private String estadoCuenta;
    private LocalDateTime creadoEn;

}
