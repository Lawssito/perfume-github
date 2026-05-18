package com.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DireccionDTO {

    @NotBlank(message = "La calle es obligatoria")
    private String calle;

    @NotBlank(message = "El número es obligatorio")
    private String numero;

    @NotBlank(message = "La comuna es obligatoria")
    private String comuna;

    @NotBlank(message = "La ciudad es obligatoria")
    private String ciudad;

    @NotBlank(message = "El tipo o alias (Casa, Trabajo, etc.) es obligatorio")
    private String tipoAlias;
}