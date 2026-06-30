package com.ms_notificaciones.service.impl;

import com.ms_notificaciones.dto.EventoNotificacionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimuladorEmail {

    public void enviar(EventoNotificacionDTO evento) throws InterruptedException {
        log.info("[AUDIT] Conectando con servidor SMTP simulado...");
        Thread.sleep(300);
        log.info("[AUDIT] Correo enviado a usuario {}: {}", evento.getIdUsuario(), evento.getMensaje());
    }
}
