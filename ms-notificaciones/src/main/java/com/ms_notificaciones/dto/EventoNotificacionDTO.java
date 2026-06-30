package com.ms_notificaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventoNotificacionDTO {

    @Schema(description = "ID del usuario destinatario", example = "1")
    
    @NotNull(message = "El ID del usuario destinatario es obligatorio")
    private Long idUsuario;

    @NotBlank(message = "El mensaje o cuerpo de la notificación es obligatorio")
    @Schema(example = "Su pedido #123 ha sido confirmado")
    private String mensaje;

    @NotBlank(message = "El tipo de evento es obligatorio (ej: PEDIDO_CREADO)")
    @Schema(description = "Tipo de evento", example = "PEDIDO_CREADO")
    private String tipoEvento;
}
