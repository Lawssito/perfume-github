package com.ms_notificaciones.controller;

import com.ms_notificaciones.dto.EventoNotificacionDTO;
import com.ms_notificaciones.dto.NotificacionResponseDTO;
import com.ms_notificaciones.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Notificaciones", description = "Envío y consulta de notificaciones a usuarios")
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
    @Operation(summary = "Enviar notificación", description = "Procesa un evento y envía una notificación al usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Notificación enviada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<NotificacionResponseDTO> enviarNotificacion(
            @Valid @RequestBody EventoNotificacionDTO eventoDTO) {
        log.info("[AUDIT] POST /api/notificaciones/enviar usuario={}", eventoDTO.getIdUsuario());
        NotificacionResponseDTO respuesta = notificacionService.procesarEventoYEnviar(eventoDTO);
        log.info("[AUDIT] Notificacion procesada id={}", respuesta.getIdNotificacion());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    @Operation(summary = "Listar todas las notificaciones", description = "Obtiene todas las notificaciones (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de notificaciones obtenida")
    })
    public ResponseEntity<List<NotificacionResponseDTO>> listarTodas() {
        exigirAdmin();
        log.info("[AUDIT] GET /api/notificaciones");
        return ResponseEntity.ok(notificacionService.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener notificación por ID", description = "Obtiene los detalles de una notificación (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificación encontrada"),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    })
    public ResponseEntity<NotificacionResponseDTO> obtenerPorId(@Parameter(description = "ID de la notificación", example = "1") @PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] GET /api/notificaciones/{}", id);
        return ResponseEntity.ok(notificacionService.obtenerPorId(id));
    }

    @GetMapping("/mis-notificaciones")
    @Operation(summary = "Listar mis notificaciones", description = "Obtiene las notificaciones del usuario autenticado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificaciones del usuario obtenidas")
    })
    public ResponseEntity<List<NotificacionResponseDTO>> listarMisNotificaciones() {
        Long idUsuario = getIdUsuarioAutenticado();
        log.info("[AUDIT] GET /api/notificaciones/mis-notificaciones usuario={}", idUsuario);
        return ResponseEntity.ok(notificacionService.listarPorUsuario(idUsuario));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar notificación", description = "Elimina una notificación del sistema (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Notificación eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    })
    public ResponseEntity<Void> eliminar(@Parameter(description = "ID de la notificación", example = "1") @PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/notificaciones/{}", id);
        notificacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
