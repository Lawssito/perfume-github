package com.ms_pagos.controller;

import com.ms_pagos.dto.CrearPagoDTO;
import com.ms_pagos.dto.PagoDTO;
import com.ms_pagos.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ms_pagos.exception.ForbiddenException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Procesamiento de pagos y transacciones")
public class PagoController {

    private final PagoService pagoService;

    private void exigirAdmin() {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String roles = req.getHeader("X-User-Roles");
        if (roles == null) return;
        if (!roles.contains("ROLE_ADMIN")) {
            throw new ForbiddenException("No tienes permisos de administrador");
        }
    }

    @PostMapping
    @Operation(summary = "Crear pago", description = "Registra un nuevo pago asociado a un pedido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pago creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<PagoDTO> crearPago(
            @Valid @RequestBody CrearPagoDTO dto) {

        log.info("[AUDIT] POST /api/pagos - Pedido: {} | Monto: {}",
                dto.getIdPedido(), dto.getMontoTotal());

        PagoDTO respuesta = pagoService.crearPago(dto);

        log.info("[AUDIT] Pago creado con ID: {}", respuesta.getIdTransaccion());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    @Operation(summary = "Listar pagos", description = "Obtiene todos los pagos registrados (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pagos obtenida")
    })
    public ResponseEntity<List<PagoDTO>> listarTodos() {
        exigirAdmin();
        log.info("[AUDIT] GET /api/pagos - Listando todos los pagos");
        List<PagoDTO> respuesta = pagoService.listarTodos();
        log.info("[AUDIT] Retornando {} pagos", respuesta.size());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar pago por ID", description = "Obtiene los detalles de un pago específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pago encontrado"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<PagoDTO> consultarPorId(@Parameter(description = "ID de la transacción", example = "1") @PathVariable Long id) {
        log.info("[AUDIT] GET /api/pagos/{}", id);
        PagoDTO respuesta = pagoService.consultarPorId(id);
        log.info("[AUDIT] Pago {} retornado. Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/pedido/{idPedido}")
    @Operation(summary = "Consultar pago por pedido", description = "Obtiene el pago asociado a un pedido específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pago encontrado"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado para el pedido")
    })
    public ResponseEntity<PagoDTO> consultarPorPedido(
            @Parameter(description = "ID del pedido", example = "1") @PathVariable Long idPedido) {

        log.info("[AUDIT] GET /api/pagos/pedido/{}", idPedido);
        PagoDTO respuesta = pagoService.consultarPorPedido(idPedido);
        log.info("[AUDIT] Pago del pedido {} retornado. Estado: {}",
                idPedido, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/procesar")
    @Operation(summary = "Procesar pago", description = "Procesa un pago pendiente (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pago procesado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<PagoDTO> procesarPago(@Parameter(description = "ID de la transacción", example = "1") @PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/pagos/{}/procesar", id);
        PagoDTO respuesta = pagoService.procesarPago(id);
        log.info("[AUDIT] Pago {} procesado. Estado final: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/anular")
    @Operation(summary = "Anular pago", description = "Anula un pago existente (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pago anulado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<PagoDTO> anularPago(@Parameter(description = "ID de la transacción", example = "1") @PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/pagos/{}/anular", id);
        PagoDTO respuesta = pagoService.anularPago(id);
        log.info("[AUDIT] Pago {} anulado exitosamente", id);
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/reintentar")
    @Operation(summary = "Reintentar pago", description = "Reintenta el procesamiento de un pago fallido (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reintento procesado"),
            @ApiResponse(responseCode = "404", description = "Pago no encontrado")
    })
    public ResponseEntity<PagoDTO> reintentarPago(@Parameter(description = "ID de la transacción", example = "1") @PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/pagos/{}/reintentar", id);
        PagoDTO respuesta = pagoService.reintentarPago(id);
        log.info("[AUDIT] Reintento de pago {} procesado. Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }
}
