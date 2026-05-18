package com.ms_notificaciones.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionResponseDTO {
    private Long idNotificacion;
    private Long idUsuario;
    private String mensaje;
    private String estadoEnvio;
    private LocalDateTime fecha;
}