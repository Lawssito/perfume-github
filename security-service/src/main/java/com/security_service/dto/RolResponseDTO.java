package com.security_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolResponseDTO {
    private Long idRol;
    private String nombre;
    private List<String> permisos;
}
