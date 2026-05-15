package com.ms_notificaciones.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ms_notificaciones.dto.EventoNotificacionDTO;
import com.ms_notificaciones.model.Notificacion;
import com.ms_notificaciones.repository.NotificacionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    @Transactional
    public Notificacion procesarEventoYEnviar(EventoNotificacionDTO evento) {
        log.info("Iniciando procesamiento de evento [{}] para el usuario ID: {}", 
                 evento.getTipoEvento(), evento.getIdUsuario());

        // 1. Guardar la notificación en estado PENDIENTE
        Notificacion notificacion = Notificacion.builder()
                .idUsuario(evento.getIdUsuario())
                .mensaje(evento.getMensaje())
                .estadoEnvio("PENDIENTE")
                .build();

        notificacion = notificacionRepository.save(notificacion);
        log.info("Notificación registrada en BD con ID [{}] y estado PENDIENTE", notificacion.getIdNotificacion());

        // 2. Simular el envío de Email o Alerta
        try {
            enviarEmailSimulado(evento);
            notificacion.setEstadoEnvio("ENVIADO");
            log.info("Email enviado con éxito para la notificación ID [{}]", notificacion.getIdNotificacion());
        } catch (Exception e) {
            notificacion.setEstadoEnvio("FALLIDO");
            log.error("Fallo al enviar el email para la notificación ID [{}]. Motivo: {}", 
                      notificacion.getIdNotificacion(), e.getMessage());
        }

        // 3. Actualizar el estado final en la BD
        return notificacionRepository.save(notificacion);
    }

    private void enviarEmailSimulado(EventoNotificacionDTO evento) throws InterruptedException {
        log.info("Conectando con el servidor SMTP / API de envíos...");
        // Simula el tiempo que tarda en enviar un correo
        Thread.sleep(500); 
        log.info(">> ENVIANDO CORREO a Usuario {}: {}", evento.getIdUsuario(), evento.getMensaje());
    }
}