package com.ms_notificaciones.service.impl;

import com.ms_notificaciones.dto.EventoNotificacionDTO;
import com.ms_notificaciones.dto.NotificacionResponseDTO;
import com.ms_notificaciones.model.Notificacion;
import com.ms_notificaciones.repository.NotificacionRepository;
import com.ms_notificaciones.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;

    @Override
    @Transactional
    public NotificacionResponseDTO procesarEventoYEnviar(EventoNotificacionDTO evento) {
        log.info("[SERVICE] Procesando evento [{}] para usuario {}", evento.getTipoEvento(), evento.getIdUsuario());

        Notificacion notificacion = Notificacion.builder()
                .idUsuario(evento.getIdUsuario())
                .mensaje(evento.getMensaje())
                .estadoEnvio("PENDIENTE")
                .build();

        notificacion = notificacionRepository.save(notificacion);
        log.info("[SERVICE] Notificacion registrada id={} estado=PENDIENTE", notificacion.getIdNotificacion());

        try {
            enviarEmailSimulado(evento);
            notificacion.setEstadoEnvio("ENVIADO");
            log.info("[SERVICE] Email enviado para notificacion id={}", notificacion.getIdNotificacion());
        } catch (Exception e) {
            notificacion.setEstadoEnvio("FALLIDO");
            log.error("[SERVICE] Fallo envio notificacion id={}: {}", notificacion.getIdNotificacion(), e.getMessage());
        }

        return mapToResponse(notificacionRepository.save(notificacion));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionResponseDTO> listarTodas() {
        log.info("[SERVICE] Listando todas las notificaciones");
        return notificacionRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificacionResponseDTO obtenerPorId(Long id) {
        log.info("[SERVICE] Consultando notificacion id={}", id);
        return mapToResponse(obtenerEntidad(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionResponseDTO> listarPorUsuario(Long idUsuario) {
        log.info("[SERVICE] Listando notificaciones de usuario {}", idUsuario);
        return notificacionRepository.findByIdUsuarioOrderByFechaDesc(idUsuario).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        log.info("[SERVICE] Eliminando notificacion id={}", id);
        notificacionRepository.delete(obtenerEntidad(id));
    }

    private Notificacion obtenerEntidad(Long id) {
        return notificacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada"));
    }

    private void enviarEmailSimulado(EventoNotificacionDTO evento) throws InterruptedException {
        log.info("[SERVICE] Conectando con servidor SMTP simulado...");
        Thread.sleep(300);
        log.info("[SERVICE] Correo enviado a usuario {}: {}", evento.getIdUsuario(), evento.getMensaje());
    }

    private NotificacionResponseDTO mapToResponse(Notificacion notificacion) {
        return new NotificacionResponseDTO(
                notificacion.getIdNotificacion(),
                notificacion.getIdUsuario(),
                notificacion.getMensaje(),
                notificacion.getEstadoEnvio(),
                notificacion.getFecha());
    }
}
