package com.ms_pedidos.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CrearNotificacionDTO {
    private Long   idUsuario;
    private String mensaje;
}
