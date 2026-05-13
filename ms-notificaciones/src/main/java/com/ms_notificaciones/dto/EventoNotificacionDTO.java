package com.ms_notificaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventoNotificacionDTO {
    
    @NotNull(message = "El ID del usuario destinatario es obligatorio")
    private Long idUsuario;

    @NotBlank(message = "El mensaje o cuerpo de la notificación es obligatorio")
    private String mensaje;

    @NotBlank(message = "El tipo de evento es obligatorio (ej: PEDIDO_CREADO)")
    private String tipoEvento;
}
