package com.ms_envios.controller;

import com.ms_envios.dto.AvanzarEstadoDTO;
import com.ms_envios.dto.CrearEnvioDTO;
import com.ms_envios.dto.EnvioDTO;
import com.ms_envios.model.EstadoEnvio;
import com.ms_envios.service.EnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Envíos", description = "Gestión de envíos, tracking y couriers")
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
    @Operation(summary = "Crear envío", description = "Crea un nuevo envío para un pedido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Envío creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<EnvioDTO> crearEnvio(@Valid @RequestBody CrearEnvioDTO dto) {
        log.info("[AUDIT] POST /api/envios - Pedido: {} | Courier: {}",
                dto.getIdPedido(), dto.getCourier());

        EnvioDTO respuesta = envioService.crearEnvio(dto);

        log.info("[AUDIT] Envio creado con ID: {} | Tracking: {}",
                respuesta.getIdEnvio(), respuesta.getNumeroTracking());

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    @Operation(summary = "Listar envíos", description = "Obtiene todos los envíos registrados (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de envíos obtenida")
    })
    public ResponseEntity<List<EnvioDTO>> listarTodos() {
        exigirAdmin();
        log.info("[AUDIT] GET /api/envios - Listando todos");
        List<EnvioDTO> respuesta = envioService.listarTodos();
        log.info("[AUDIT] Retornando {} envios", respuesta.size());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/estado/{estado}")
    @Operation(summary = "Listar envíos por estado", description = "Obtiene los envíos filtrados por estado (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de envíos por estado obtenida")
    })
    public ResponseEntity<List<EnvioDTO>> listarPorEstado(
            @Parameter(description = "Estado del envío", example = "EN_TRANSITO") @PathVariable EstadoEnvio estado) {

        exigirAdmin();
        log.info("[AUDIT] GET /api/envios/estado/{}", estado);
        List<EnvioDTO> respuesta = envioService.listarPorEstado(estado);
        log.info("[AUDIT] Retornando {} envios con estado {}", respuesta.size(), estado);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar envío por ID", description = "Obtiene los detalles de un envío específico (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Envío encontrado"),
            @ApiResponse(responseCode = "404", description = "Envío no encontrado")
    })
    public ResponseEntity<EnvioDTO> consultarPorId(@Parameter(description = "ID del envío", example = "1") @PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] GET /api/envios/{}", id);
        EnvioDTO respuesta = envioService.consultarPorId(id);
        log.info("[AUDIT] Envio {} retornado. Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/pedido/{idPedido}")
    @Operation(summary = "Consultar envío por pedido", description = "Obtiene el envío asociado a un pedido (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Envío encontrado"),
            @ApiResponse(responseCode = "404", description = "Envío no encontrado para el pedido")
    })
    public ResponseEntity<EnvioDTO> consultarPorPedido(
            @Parameter(description = "ID del pedido", example = "1") @PathVariable Long idPedido) {

        exigirAdmin();
        log.info("[AUDIT] GET /api/envios/pedido/{}", idPedido);
        EnvioDTO respuesta = envioService.consultarPorPedido(idPedido);
        log.info("[AUDIT] Envio del pedido {} retornado. Estado: {}",
                idPedido, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Avanzar estado de envío", description = "Actualiza el estado de un envío (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Envío no encontrado")
    })
    public ResponseEntity<EnvioDTO> avanzarEstado(
            @Parameter(description = "ID del envío", example = "1") @PathVariable Long id,
            @Valid @RequestBody AvanzarEstadoDTO dto) {

        exigirAdmin();
        log.info("[AUDIT] PUT /api/envios/{}/estado - Nuevo estado: {}", id, dto.getEstado());
        EnvioDTO respuesta = envioService.avanzarEstado(id, dto);
        log.info("[AUDIT] Estado del envio {} actualizado a {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar envío", description = "Cancela un envío existente (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Envío cancelado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Envío no encontrado")
    })
    public ResponseEntity<EnvioDTO> cancelarEnvio(@Parameter(description = "ID del envío", example = "1") @PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/envios/{}/cancelar", id);
        EnvioDTO respuesta = envioService.cancelarEnvio(id);
        log.info("[AUDIT] Envio {} cancelado exitosamente", id);
        return ResponseEntity.ok(respuesta);
    }
}
