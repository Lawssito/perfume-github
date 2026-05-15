package com.ms_pagos.controller;

import com.ms_pagos.dto.CrearPagoDTO;
import com.ms_pagos.dto.PagoDTO;
import com.ms_pagos.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @PostMapping
    public ResponseEntity<PagoDTO> crearPago(
            @Valid @RequestBody CrearPagoDTO dto) {

        log.info("[CONTROLLER] POST /api/pagos - Pedido: {} | Monto: {}",
                dto.getIdPedido(), dto.getMontoTotal());

        PagoDTO respuesta = pagoService.crearPago(dto);

        log.info("[CONTROLLER] Pago creado con ID: {}", respuesta.getIdTransaccion());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    public ResponseEntity<List<PagoDTO>> listarTodos() {
        log.info("[CONTROLLER] GET /api/pagos - Listando todos los pagos");
        List<PagoDTO> respuesta = pagoService.listarTodos();
        log.info("[CONTROLLER] Retornando {} pagos", respuesta.size());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoDTO> consultarPorId(@PathVariable Long id) {
        log.info("[CONTROLLER] GET /api/pagos/{}", id);
        PagoDTO respuesta = pagoService.consultarPorId(id);
        log.info("[CONTROLLER] Pago {} retornado. Estado: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/pedido/{idPedido}")
    public ResponseEntity<PagoDTO> consultarPorPedido(
            @PathVariable Long idPedido) {

        log.info("[CONTROLLER] GET /api/pagos/pedido/{}", idPedido);
        PagoDTO respuesta = pagoService.consultarPorPedido(idPedido);
        log.info("[CONTROLLER] Pago del pedido {} retornado. Estado: {}",
                idPedido, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/procesar")
    public ResponseEntity<PagoDTO> procesarPago(@PathVariable Long id) {
        log.info("[CONTROLLER] POST /api/pagos/{}/procesar", id);
        PagoDTO respuesta = pagoService.procesarPago(id);
        log.info("[CONTROLLER] Pago {} procesado. Estado final: {}", id, respuesta.getEstado());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/anular")
    public ResponseEntity<PagoDTO> anularPago(@PathVariable Long id) {
        log.info("[CONTROLLER] POST /api/pagos/{}/anular", id);
        PagoDTO respuesta = pagoService.anularPago(id);
        log.info("[CONTROLLER] Pago {} anulado exitosamente", id);
        return ResponseEntity.ok(respuesta);
    }
}
