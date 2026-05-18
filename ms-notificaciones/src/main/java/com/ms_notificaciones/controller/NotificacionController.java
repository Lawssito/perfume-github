package com.ms_notificaciones.controller;

import com.ms_notificaciones.dto.EventoNotificacionDTO;
import com.ms_notificaciones.dto.NotificacionResponseDTO;
import com.ms_notificaciones.service.NotificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @PostMapping("/enviar")
    public ResponseEntity<NotificacionResponseDTO> enviarNotificacion(
            @Valid @RequestBody EventoNotificacionDTO eventoDTO) {
        log.info("[CONTROLLER] POST /api/notificaciones/enviar usuario={}", eventoDTO.getIdUsuario());
        NotificacionResponseDTO respuesta = notificacionService.procesarEventoYEnviar(eventoDTO);
        log.info("[CONTROLLER] Notificacion procesada id={}", respuesta.getIdNotificacion());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<NotificacionResponseDTO>> listarTodas() {
        log.info("[CONTROLLER] GET /api/notificaciones");
        return ResponseEntity.ok(notificacionService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificacionResponseDTO> obtenerPorId(@PathVariable Long id) {
        log.info("[CONTROLLER] GET /api/notificaciones/{}", id);
        return ResponseEntity.ok(notificacionService.obtenerPorId(id));
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<NotificacionResponseDTO>> listarPorUsuario(@PathVariable Long idUsuario) {
        log.info("[CONTROLLER] GET /api/notificaciones/usuario/{}", idUsuario);
        return ResponseEntity.ok(notificacionService.listarPorUsuario(idUsuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("[CONTROLLER] DELETE /api/notificaciones/{}", id);
        notificacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
