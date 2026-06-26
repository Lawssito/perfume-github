package com.ms_notificaciones.controller;

import com.ms_notificaciones.dto.EventoNotificacionDTO;
import com.ms_notificaciones.dto.NotificacionResponseDTO;
import com.ms_notificaciones.service.NotificacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ms_notificaciones.exception.ForbiddenException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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

    private void exigirAdmin() {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String roles = req.getHeader("X-User-Roles");
        if (roles == null) return;
        if (!roles.contains("ROLE_ADMIN")) {
            throw new ForbiddenException("No tienes permisos de administrador");
        }
    }

    private Long getIdUsuarioAutenticado() {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String id = req.getHeader("X-User-Id");
        if (id == null) {
            throw new ForbiddenException("Usuario no autenticado");
        }
        return Long.parseLong(id);
    }

    private void exigirMismoUsuario(Long idUsuarioPath) {
        Long idToken = getIdUsuarioAutenticado();
        if (idToken == null) return;
        if (!idToken.equals(idUsuarioPath)) {
            throw new ForbiddenException("Solo puedes acceder a tus propios datos");
        }
    }

    @PostMapping("/enviar")
    public ResponseEntity<NotificacionResponseDTO> enviarNotificacion(
            @Valid @RequestBody EventoNotificacionDTO eventoDTO) {
        log.info("[AUDIT] POST /api/notificaciones/enviar usuario={}", eventoDTO.getIdUsuario());
        NotificacionResponseDTO respuesta = notificacionService.procesarEventoYEnviar(eventoDTO);
        log.info("[AUDIT] Notificacion procesada id={}", respuesta.getIdNotificacion());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<NotificacionResponseDTO>> listarTodas() {
        exigirAdmin();
        log.info("[AUDIT] GET /api/notificaciones");
        return ResponseEntity.ok(notificacionService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificacionResponseDTO> obtenerPorId(@PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] GET /api/notificaciones/{}", id);
        return ResponseEntity.ok(notificacionService.obtenerPorId(id));
    }

    @GetMapping("/mis-notificaciones")
    public ResponseEntity<List<NotificacionResponseDTO>> listarMisNotificaciones() {
        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT] GET /api/notificaciones/mis-notificaciones usuario={}", idUsuario);
        return ResponseEntity.ok(notificacionService.listarPorUsuario(idUsuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/notificaciones/{}", id);
        notificacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
