package com.ms_pagos.service;

import com.ms_pagos.dto.CrearPagoDTO;
import com.ms_pagos.dto.PagoDTO;
import com.ms_pagos.model.EstadoPago;
import com.ms_pagos.model.Pago;
import com.ms_pagos.exception.PagoNotFoundException;
import com.ms_pagos.exception.TransicionEstadoInvalidaException;
import com.ms_pagos.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;

    // CREAR PAGO
    @Transactional
    public PagoDTO crearPago(CrearPagoDTO dto) {
        log.info("[SERVICE] Creando pago para pedido ID: {} | Monto: {} | Metodo: {}",
                dto.getIdPedido(), dto.getMontoTotal(), dto.getMetodoPago());

        // Verificar duplicado — DataIntegrityViolationException como respaldo
        // si hay condición de carrera, el GlobalExceptionHandler la captura
        if (pagoRepository.findByIdPedido(dto.getIdPedido()).isPresent()) {
            log.warn("[SERVICE] Ya existe pago para pedido ID: {}", dto.getIdPedido());
            throw new IllegalStateException(
                "Ya existe un pago registrado para el pedido " + dto.getIdPedido()
            );
        }

        Pago pago = new Pago();
        pago.setIdPedido(dto.getIdPedido());
        pago.setMontoTotal(dto.getMontoTotal());
        pago.setMetodoPago(dto.getMetodoPago());
        pago.setReferenciaExterna(
            "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );

        Pago guardado = pagoRepository.save(pago);

        log.info("[SERVICE] Pago creado exitosamente. ID: {} | Referencia: {} | Estado: {}",
                guardado.getIdTransaccion(),
                guardado.getReferenciaExterna(),
                guardado.getEstado());

        return mapToDTO(guardado);
    }

    // PROCESAR PAGO
    @Transactional
    public PagoDTO procesarPago(Long idTransaccion) {
        log.info("[SERVICE] Procesando pago ID: {}", idTransaccion);

        Pago pago = pagoRepository.findById(idTransaccion)
            .orElseThrow(() -> {
                log.warn("[SERVICE] Pago ID {} no encontrado al procesar", idTransaccion);
                return new PagoNotFoundException(idTransaccion);
            });

        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            log.warn("[SERVICE] Pago {} ya procesado. Estado actual: {}",
                    idTransaccion, pago.getEstado());
            throw new TransicionEstadoInvalidaException(pago.getEstado(), EstadoPago.COMPLETADO);
        }

        // Simulación bancaria aleatoria: 80% COMPLETADO, 20% RECHAZADO
        boolean pagoExitoso = Math.random() < 0.8;
        EstadoPago nuevoEstado = pagoExitoso ? EstadoPago.COMPLETADO : EstadoPago.RECHAZADO;

        pago.setEstado(nuevoEstado);
        pago.setProcesadoEn(LocalDateTime.now());

        Pago actualizado = pagoRepository.save(pago);

        log.info("[SERVICE] Pago {} procesado. Resultado: {} | Procesado en: {}",
            idTransaccion, nuevoEstado, actualizado.getProcesadoEn());

        return mapToDTO(actualizado);
    }

    // ANULAR PAGO
    @Transactional
    public PagoDTO anularPago(Long idTransaccion) {
        log.info("[SERVICE] Anulando pago ID: {}", idTransaccion);

        Pago pago = pagoRepository.findById(idTransaccion)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Pago ID {} no encontrado al anular", idTransaccion);
                    return new PagoNotFoundException(idTransaccion);
                });

        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            log.warn("[SERVICE] No se puede anular pago {}. Estado actual: {}",
                    idTransaccion, pago.getEstado());
            throw new TransicionEstadoInvalidaException(pago.getEstado(), EstadoPago.ANULADO);
        }

        pago.setEstado(EstadoPago.ANULADO);
        pago.setProcesadoEn(LocalDateTime.now());

        Pago actualizado = pagoRepository.save(pago);

        log.info("[SERVICE] Pago {} anulado exitosamente", idTransaccion);
        return mapToDTO(actualizado);
    }

    // CONSULTAR POR ID
    public PagoDTO consultarPorId(Long idTransaccion) {
        log.info("[SERVICE] Consultando pago ID: {}", idTransaccion);

        Pago pago = pagoRepository.findById(idTransaccion)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Pago ID {} no encontrado", idTransaccion);
                    return new PagoNotFoundException(idTransaccion);
                });

        log.info("[SERVICE] Pago encontrado. ID: {} | Estado: {} | Pedido: {}",
                idTransaccion, pago.getEstado(), pago.getIdPedido());

        return mapToDTO(pago);
    }

    // CONSULTAR POR PEDIDO
    public PagoDTO consultarPorPedido(Long idPedido) {
        log.info("[SERVICE] Consultando pago del pedido ID: {}", idPedido);

        Pago pago = pagoRepository.findByIdPedido(idPedido)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] No existe pago para pedido ID {}", idPedido);
                    return new PagoNotFoundException(idPedido);
                });

        log.info("[SERVICE] Pago encontrado para pedido {}. Estado: {}",
                idPedido, pago.getEstado());

        return mapToDTO(pago);
    }

    // LISTAR TODOS
    public List<PagoDTO> listarTodos() {
        log.info("[SERVICE] Listando todos los pagos");

        List<Pago> pagos = pagoRepository.findAll();

        log.info("[SERVICE] Total de pagos encontrados: {}", pagos.size());
        return pagos.stream().map(this::mapToDTO).toList();
    }

    // MAPPER
    private PagoDTO mapToDTO(Pago pago) {
        return new PagoDTO(
            pago.getIdTransaccion(),
            pago.getIdPedido(),
            pago.getMontoTotal(),
            pago.getMetodoPago(),
            pago.getEstado(),
            pago.getReferenciaExterna(),
            pago.getCreadoEn(),
            pago.getProcesadoEn()
        );
    }
}