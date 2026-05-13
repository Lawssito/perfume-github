package com.ms_notificaciones.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ms_notificaciones.dto.EventoNotificacionDTO;
import com.ms_notificaciones.model.Notificacion;
import com.ms_notificaciones.service.NotificacionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    // Endpoint para que otros microservicios avisen de un evento
    @PostMapping("/eventos")
    public ResponseEntity<Notificacion> recibirEvento(@Valid @RequestBody EventoNotificacionDTO eventoDTO) {
        log.info("Recibida peticion POST (Evento) en ms-notificaciones: {}", eventoDTO);
        
        Notificacion resultado = notificacionService.procesarEventoYEnviar(eventoDTO);
        
        log.info("Evento procesado y notificación finalizada con ID: {} - Estado: {}", 
                 resultado.getIdNotificacion(), resultado.getEstadoEnvio());
                 
        return ResponseEntity.ok(resultado);
    }
}
