package com.ms_pagos.controller;

import com.ms_pagos.dto.CrearPagoDTO;
import com.ms_pagos.dto.PagoDTO;
import com.ms_pagos.service.PagoService;
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
    public ResponseEntity<PagoDTO> crearPago(
            @Valid @RequestBody CrearPagoDTO dto) {

        log.info("[AUDIT] POST /api/pagos - Pedido: {} | Monto: {}",
                dto.getIdPedido(), dto.getMontoTotal());

        PagoDTO respuesta = pagoService.crearPago(dto);

        log.info("[AUDIT] Pago creado con ID: {}", respuesta.getIdTransaccion());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<PagoDTO>> listarTodos() {
        exigirAdmin();
        log.info("[AUDIT] GET /api/pagos - Listando todos los pagos");
        List<PagoDTO> respuesta = pagoService.listarTodos();
        log.info("[AUDIT] Retornando {} pagos", respuesta.size());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoDTO> consultarPorId(@PathVariable Long id) {
        log.info("[AUDIT] GET /api/pagos/{}", id);
        PagoDTO respuesta = pagoService.consultarPorId(id);
        log.info("[AUDIT] Pago {} retornado. Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/pedido/{idPedido}")
    public ResponseEntity<PagoDTO> consultarPorPedido(
            @PathVariable Long idPedido) {

        log.info("[AUDIT] GET /api/pagos/pedido/{}", idPedido);
        PagoDTO respuesta = pagoService.consultarPorPedido(idPedido);
        log.info("[AUDIT] Pago del pedido {} retornado. Estado: {}",
                idPedido, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/procesar")
    public ResponseEntity<PagoDTO> procesarPago(@PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/pagos/{}/procesar", id);
        PagoDTO respuesta = pagoService.procesarPago(id);
        log.info("[AUDIT] Pago {} procesado. Estado final: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/anular")
    public ResponseEntity<PagoDTO> anularPago(@PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/pagos/{}/anular", id);
        PagoDTO respuesta = pagoService.anularPago(id);
        log.info("[AUDIT] Pago {} anulado exitosamente", id);
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/reintentar")
    public ResponseEntity<PagoDTO> reintentarPago(@PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/pagos/{}/reintentar", id);
        PagoDTO respuesta = pagoService.reintentarPago(id);
        log.info("[AUDIT] Reintento de pago {} procesado. Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }
}
