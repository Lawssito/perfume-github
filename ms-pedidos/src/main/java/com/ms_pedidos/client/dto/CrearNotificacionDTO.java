package com.ms_pedidos.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CrearNotificacionDTO {
    private Long   idUsuario;
    private String mensaje;
    private String tipoEvento;

    public CrearNotificacionDTO(Long idUsuario, String mensaje) {
        this.idUsuario = idUsuario;
        this.mensaje = mensaje;
        this.tipoEvento = "PEDIDO_EVENTO";
    }
}