package com.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DireccionResponseDTO {
    private Long idDireccion;
    private String calle;
    private String numero;
    private String comuna;
    private String ciudad;
    private String tipoAlias;
}
