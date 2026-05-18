package com.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AsignarRolClientDTO {
    private Long idUsuario;
    private String rolNombre;
}
