package com.ms_pagos.service.impl;

import com.ms_pagos.dto.CrearPagoDTO;
import com.ms_pagos.dto.PagoDTO;
import com.ms_pagos.model.EstadoPago;
import com.ms_pagos.model.Pago;
import com.ms_pagos.exception.PagoNotFoundException;
import com.ms_pagos.exception.NoHayMontoPorPagarException;
import com.ms_pagos.exception.TransicionEstadoInvalidaException;
import com.ms_pagos.repository.PagoRepository;
import com.ms_pagos.service.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;

    // CREAR
    @Override
    @Transactional
    public PagoDTO crearPago(CrearPagoDTO dto) {
        log.info("[AUDIT] Creando pago para pedido ID: {} | Monto: {} | Metodo: {}",
                dto.getIdPedido(), dto.getMontoTotal(), dto.getMetodoPago());

        validarMontoPorPagar(dto.getMontoTotal());
        validarPedidoSinPago(dto.getIdPedido());

        Pago guardado = pagoRepository.save(buildPago(dto));

        log.info("[AUDIT] Pago creado exitosamente. ID: {} | Referencia: {} | Estado: {}",
                guardado.getIdTransaccion(),
                guardado.getReferenciaExterna(),
                guardado.getEstado());

        return mapToResponse(guardado);
    }

    // PROCESAR
    @Override
    @Transactional
    public PagoDTO procesarPago(Long idTransaccion) {
        log.info("[AUDIT] Procesando pago ID: {}", idTransaccion);

        Pago pago = obtenerPorId(idTransaccion);
        validarMontoPorPagar(pago.getMontoTotal());
        validarEstadoPendiente(pago, EstadoPago.COMPLETADO);

        EstadoPago nuevoEstado = simularResultadoBancario();
        pago.setEstado(nuevoEstado);
        pago.setProcesadoEn(LocalDateTime.now());

        Pago actualizado = pagoRepository.save(pago);

        log.info("[AUDIT] Pago {} procesado. Resultado: {} | Procesado en: {}",
                idTransaccion, nuevoEstado, actualizado.getProcesadoEn());

        return mapToResponse(actualizado);
    }

    // ANULAR
    @Override
    @Transactional
    public PagoDTO anularPago(Long idTransaccion) {
        log.info("[AUDIT] Anulando pago ID: {}", idTransaccion);

        Pago pago = obtenerPorId(idTransaccion);
        validarEstadoPendiente(pago, EstadoPago.ANULADO);

        pago.setEstado(EstadoPago.ANULADO);
        pago.setProcesadoEn(LocalDateTime.now());

        Pago actualizado = pagoRepository.save(pago);

        log.info("[AUDIT] Pago {} anulado exitosamente", idTransaccion);
        return mapToResponse(actualizado);
    }

    // CONSULTAR POR ID
    @Override
    public PagoDTO consultarPorId(Long idTransaccion) {
        log.info("[AUDIT] Consultando pago ID: {}", idTransaccion);

        Pago pago = obtenerPorId(idTransaccion);

        log.info("[AUDIT] Pago encontrado. ID: {} | Estado: {} | Pedido: {}",
                idTransaccion, pago.getEstado(), pago.getIdPedido());

        return mapToResponse(pago);
    }

    // CONSULTAR POR PEDIDO
    @Override
    public PagoDTO consultarPorPedido(Long idPedido) {
        log.info("[AUDIT] Consultando pago del pedido ID: {}", idPedido);

        Pago pago = pagoRepository.findByIdPedido(idPedido)
                .orElseThrow(() -> {
                    log.warn("[AUDIT] No existe pago para pedido ID {}", idPedido);
                    return new PagoNotFoundException(idPedido);
                });

        log.info("[AUDIT] Pago encontrado para pedido {}. Estado: {}",
                idPedido, pago.getEstado());

        return mapToResponse(pago);
    }

    // LISTAR TODOS
    @Override
    public List<PagoDTO> listarTodos() {
        log.info("[AUDIT] Listando todos los pagos");

        List<Pago> pagos = pagoRepository.findAll();

        log.info("[AUDIT] Total de pagos encontrados: {}", pagos.size());
        return pagos.stream().map(this::mapToResponse).toList();
    }

    // REINTENTAR
    @Override
    @Transactional
    public PagoDTO reintentarPago(Long idTransaccion) {
        log.info("[AUDIT] Reintentando pago ID: {}", idTransaccion);

        Pago pago = obtenerPorId(idTransaccion);

        if (pago.getEstado() != EstadoPago.RECHAZADO) {
            log.warn("[AUDIT] Pago {} no esta RECHAZADO. Estado actual: {}", idTransaccion, pago.getEstado());
            throw new TransicionEstadoInvalidaException(pago.getEstado(), EstadoPago.PENDIENTE);
        }

        // Volver a estado PENDIENTE y reprocesar
        pago.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(pago);

        EstadoPago nuevoEstado = simularResultadoBancario();
        pago.setEstado(nuevoEstado);
        pago.setProcesadoEn(LocalDateTime.now());
        Pago actualizado = pagoRepository.save(pago);

        log.info("[AUDIT] Reintento de pago {} procesado. Resultado: {}", idTransaccion, nuevoEstado);
        return mapToResponse(actualizado);
    }

    // MÉTODOS AUXILIARES PRIVADOS
    private Pago obtenerPorId(Long idTransaccion) {
        return pagoRepository.findById(idTransaccion)
                .orElseThrow(() -> {
                    log.warn("[AUDIT] Pago ID {} no encontrado", idTransaccion);
                    return new PagoNotFoundException(idTransaccion);
                });
    }

    private void validarMontoPorPagar(BigDecimal montoTotal) {
        if (montoTotal == null || montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[AUDIT] Intento de pago sin monto por pagar. Monto recibido: {}", montoTotal);
            throw new NoHayMontoPorPagarException(montoTotal);
        }
    }

    private void validarPedidoSinPago(Long idPedido) {
        if (pagoRepository.findByIdPedido(idPedido).isPresent()) {
            log.warn("[AUDIT] Ya existe pago para pedido ID: {}", idPedido);
            throw new IllegalStateException("Ya existe un pago registrado para el pedido " + idPedido);
        }
    }

    private void validarEstadoPendiente(Pago pago, EstadoPago destino) {
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            log.warn("[AUDIT] Pago {} no esta en estado PENDIENTE. Estado actual: {}",
                    pago.getIdTransaccion(), pago.getEstado());
            throw new TransicionEstadoInvalidaException(pago.getEstado(), destino);
        }
    }

    private EstadoPago simularResultadoBancario() {
        // 80% COMPLETADO, 20% RECHAZADO
        boolean exitoso = Math.random() < 0.8;
        EstadoPago resultado = exitoso ? EstadoPago.COMPLETADO : EstadoPago.RECHAZADO;
        log.info("[AUDIT] Simulacion bancaria: {}", resultado);
        return resultado;
    }

    private Pago buildPago(CrearPagoDTO dto) {
        Pago pago = new Pago();
        pago.setIdPedido(dto.getIdPedido());
        pago.setMontoTotal(dto.getMontoTotal());
        pago.setMetodoPago(dto.getMetodoPago());
        pago.setReferenciaExterna(
            "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
        return pago;
    }

    private PagoDTO mapToResponse(Pago pago) {
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