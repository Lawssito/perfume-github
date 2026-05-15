package com.ms_pagos.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ms_pagos.dto.CrearPagoDTO;
import com.ms_pagos.dto.PagoDTO;
import com.ms_pagos.exception.PagoNotFoundException;
import com.ms_pagos.exception.TransicionEstadoInvalidaException;
import com.ms_pagos.model.EstadoPago;
import com.ms_pagos.model.Pago;
import com.ms_pagos.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {
    
    private final PagoRepository pagoRepository;

    @Transactional
    public PagoDTO crearPago(CrearPagoDTO dto) {
        log.info("[SERVICE] Creando pago para pedido ID: {} | Monto: {} | Metodo: {}",
                dto.getIdPedido(), dto.getMontoTotal(), dto.getMetodoPago());
        try {
            if (pagoRepository.findByIdPedido(dto.getIdPedido()).isPresent()) {
                log.warn("[SERVICE] Ya existe un pago para el pedido ID: {}", dto.getIdPedido());
                throw new IllegalStateException(
                    "Ya existe un pago registrado para el pedido " + dto.getIdPedido()
                );
            }

            Pago pago = new Pago();
            pago.setIdPedido(dto.getIdPedido());
            pago.setMontoTotal(dto.getMontoTotal());
            pago.setMetodoPago(dto.getMetodoPago());

            pago.setReferenciaExterna("REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

            Pago guardado = pagoRepository.save(pago);

            log.info("[SERVICE] Pago creado exitosamente. ID: {} | Referencia: {} | Estado: {}",
                    guardado.getIdTransaccion(), guardado.getReferenciaExterna(), guardado.getEstado());

            return mapToDTO(guardado);

        } catch (IllegalStateException e) {
            log.warn("[SERVICE] Intento de crear pago duplicado para pedido {}", dto.getIdPedido());
            throw e;
        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al crear pago para pedido {}: {}",
                    dto.getIdPedido(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public PagoDTO procesarPago(Long idTransaccion) {
        log.info("[SERVICE] Procesando pago ID: {}", idTransaccion);

        try {
            Pago pago = pagoRepository.findById(idTransaccion)
                    .orElseThrow(() -> new PagoNotFoundException(idTransaccion));

            // Solo se pueden procesar pagos PENDIENTES
            if (pago.getEstado() != EstadoPago.PENDIENTE) {
                log.warn("[SERVICE] Pago {} ya fue procesado. Estado actual: {}",
                        idTransaccion, pago.getEstado());
                throw new TransicionEstadoInvalidaException(
                    pago.getEstado(), EstadoPago.COMPLETADO
                );
            }

            // Simulación del procesamiento bancario
            boolean pagoExitoso = Math.random() < 0.8; // 80% de probabilidad de éxito
            EstadoPago nuevoEstado = pagoExitoso ? EstadoPago.COMPLETADO : EstadoPago.RECHAZADO;

            pago.setEstado(nuevoEstado);
            pago.setProcesadoEn(LocalDateTime.now());

            Pago actualizado = pagoRepository.save(pago);

            log.info("[SERVICE] Pago {} procesado. Resultado: {} | Procesado en: {}",
                    idTransaccion, nuevoEstado, actualizado.getProcesadoEn());

            return mapToDTO(actualizado);

        } catch (PagoNotFoundException | TransicionEstadoInvalidaException e) {
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al procesar pago {}: {}",
                    idTransaccion, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public PagoDTO anularPago(Long idTransaccion) {
        log.info("[SERVICE] Anulando pago ID: {}", idTransaccion);

        try {
            Pago pago = pagoRepository.findById(idTransaccion)
                    .orElseThrow(() -> new PagoNotFoundException(idTransaccion));

            if (pago.getEstado() != EstadoPago.PENDIENTE) {
                log.warn("[SERVICE] No se puede anular pago {}. Estado actual: {}",
                        idTransaccion, pago.getEstado());
                throw new TransicionEstadoInvalidaException(
                    pago.getEstado(), EstadoPago.ANULADO
                );
            }

            pago.setEstado(EstadoPago.ANULADO);
            pago.setProcesadoEn(LocalDateTime.now());

            Pago actualizado = pagoRepository.save(pago);

            log.info("[SERVICE] Pago {} anulado exitosamente", idTransaccion);

            return mapToDTO(actualizado);

        } catch (PagoNotFoundException | TransicionEstadoInvalidaException e) {
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al anular pago {}: {}",
                    idTransaccion, e.getMessage(), e);
            throw e;
        }
    }

    public PagoDTO consultarPorId(Long idTransaccion) {
        log.info("[SERVICE] Consultando pago ID: {}", idTransaccion);

        try {
            Pago pago = pagoRepository.findById(idTransaccion)
                    .orElseThrow(() -> new PagoNotFoundException(idTransaccion));

            log.info("[SERVICE] Pago encontrado. ID: {} | Estado: {} | Pedido: {}",
                    idTransaccion, pago.getEstado(), pago.getIdPedido());

            return mapToDTO(pago);

        } catch (PagoNotFoundException e) {
            log.warn("[SERVICE] Pago ID {} no encontrado", idTransaccion);
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al consultar pago {}: {}",
                    idTransaccion, e.getMessage(), e);
            throw e;
        }
    }


    public PagoDTO consultarPorPedido(Long idPedido) {
        log.info("[SERVICE] Consultando pago del pedido ID: {}", idPedido);

        try {
            Pago pago = pagoRepository.findByIdPedido(idPedido)
                    .orElseThrow(() -> new PagoNotFoundException(idPedido));

            log.info("[SERVICE] Pago encontrado para pedido {}. Estado: {}",
                    idPedido, pago.getEstado());

            return mapToDTO(pago);

        } catch (PagoNotFoundException e) {
            log.warn("[SERVICE] No existe pago para pedido ID {}", idPedido);
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al consultar pago del pedido {}: {}",
                    idPedido, e.getMessage(), e);
            throw e;
        }
    }

    public List<PagoDTO> listarTodos() {
        log.info("[SERVICE] Listando todos los pagos");

        try {
            List<Pago> pagos = pagoRepository.findAll();
            log.info("[SERVICE] Total de pagos encontrados: {}", pagos.size());
            return pagos.stream().map(this::mapToDTO).toList();

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al listar pagos: {}", e.getMessage(), e);
            throw e;
        }
    }

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