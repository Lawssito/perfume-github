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

// Cambiado para coincidir con tu nombre de clase en español
    private final NotificacionService notificacionService;

    @PostMapping("/enviar")
    public ResponseEntity<Notificacion> enviarNotificacion(@Valid @RequestBody EventoNotificacionDTO eventoDTO) {
        // Ajustado para usar getIdUsuario() según tu Service
        log.info("Petición POST recibida en /api/notificaciones/enviar para el usuario ID: {}", eventoDTO.getIdUsuario());
        
        // Ajustado para usar el método exacto que definiste en la línea 21 de tu captura
        Notificacion notificacionGuardada = notificacionService.procesarEventoYEnviar(eventoDTO);
        
        log.info("Notificación procesada y guardada con éxito. ID: {}", notificacionGuardada.getIdNotificacion());
        return ResponseEntity.ok(notificacionGuardada);
    }
}