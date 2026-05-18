package com.user_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenClaimsClientDTO {
    private boolean valido;
    private String email;
    private Long idUsuario;
    private List<String> roles;
    private String mensaje;
}
