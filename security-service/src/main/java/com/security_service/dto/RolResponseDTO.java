package com.security_service.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rol con sus permisos asociados")
public class RolResponseDTO {

    @Schema(description = "ID del rol", example = "1")
    private Long idRol;
    private String nombre;
    @ArraySchema(schema = @Schema(description = "Permisos del rol", example = "ACCESS_ADMIN"))
    private List<String> permisos;
}
