package com.ms_envios.controller;

import com.ms_envios.dto.AvanzarEstadoDTO;
import com.ms_envios.dto.CrearEnvioDTO;
import com.ms_envios.dto.EnvioDTO;
import com.ms_envios.model.EstadoEnvio;
import com.ms_envios.service.EnvioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ms_envios.exception.ForbiddenException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/envios")
@RequiredArgsConstructor
public class EnvioController {

    private final EnvioService envioService;

    private void exigirAdmin() {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String roles = req.getHeader("X-User-Roles");
        if (roles == null) return;
        if (!roles.contains("ROLE_ADMIN")) {
            throw new ForbiddenException("No tienes permisos de administrador");
        }
    }

    @PostMapping
    public ResponseEntity<EnvioDTO> crearEnvio(@Valid @RequestBody CrearEnvioDTO dto) {
        log.info("[AUDIT] POST /api/envios - Pedido: {} | Courier: {}",
                dto.getIdPedido(), dto.getCourier());

        EnvioDTO respuesta = envioService.crearEnvio(dto);

        log.info("[AUDIT] Envio creado con ID: {} | Tracking: {}",
                respuesta.getIdEnvio(), respuesta.getNumeroTracking());

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<EnvioDTO>> listarTodos() {
        exigirAdmin();
        log.info("[AUDIT] GET /api/envios - Listando todos");
        List<EnvioDTO> respuesta = envioService.listarTodos();
        log.info("[AUDIT] Retornando {} envios", respuesta.size());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<EnvioDTO>> listarPorEstado(
            @PathVariable EstadoEnvio estado) {

        exigirAdmin();
        log.info("[AUDIT] GET /api/envios/estado/{}", estado);
        List<EnvioDTO> respuesta = envioService.listarPorEstado(estado);
        log.info("[AUDIT] Retornando {} envios con estado {}", respuesta.size(), estado);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnvioDTO> consultarPorId(@PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] GET /api/envios/{}", id);
        EnvioDTO respuesta = envioService.consultarPorId(id);
        log.info("[AUDIT] Envio {} retornado. Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/pedido/{idPedido}")
    public ResponseEntity<EnvioDTO> consultarPorPedido(
            @PathVariable Long idPedido) {

        exigirAdmin();
        log.info("[AUDIT] GET /api/envios/pedido/{}", idPedido);
        EnvioDTO respuesta = envioService.consultarPorPedido(idPedido);
        log.info("[AUDIT] Envio del pedido {} retornado. Estado: {}",
                idPedido, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<EnvioDTO> avanzarEstado(
            @PathVariable Long id,
            @Valid @RequestBody AvanzarEstadoDTO dto) {

        exigirAdmin();
        log.info("[AUDIT] PUT /api/envios/{}/estado - Nuevo estado: {}", id, dto.getEstado());
        EnvioDTO respuesta = envioService.avanzarEstado(id, dto);
        log.info("[AUDIT] Estado del envio {} actualizado a {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<EnvioDTO> cancelarEnvio(@PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/envios/{}/cancelar", id);
        EnvioDTO respuesta = envioService.cancelarEnvio(id);
        log.info("[AUDIT] Envio {} cancelado exitosamente", id);
        return ResponseEntity.ok(respuesta);
    }
}
