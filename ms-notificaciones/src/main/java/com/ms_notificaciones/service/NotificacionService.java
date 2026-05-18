package com.ms_notificaciones.service;

import java.util.List;

import com.ms_notificaciones.dto.EventoNotificacionDTO;
import com.ms_notificaciones.dto.NotificacionResponseDTO;

public interface NotificacionService {

    NotificacionResponseDTO procesarEventoYEnviar(EventoNotificacionDTO evento);

    List<NotificacionResponseDTO> listarTodas();

    NotificacionResponseDTO obtenerPorId(Long id);

    List<NotificacionResponseDTO> listarPorUsuario(Long idUsuario);

    void eliminar(Long id);
}
