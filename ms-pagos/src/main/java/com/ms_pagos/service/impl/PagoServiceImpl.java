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
        log.info("[SERVICE] Creando pago para pedido ID: {} | Monto: {} | Metodo: {}",
                dto.getIdPedido(), dto.getMontoTotal(), dto.getMetodoPago());

        validarMontoPorPagar(dto.getMontoTotal());
        validarPedidoSinPago(dto.getIdPedido());

        Pago guardado = pagoRepository.save(buildPago(dto));

        log.info("[SERVICE] Pago creado exitosamente. ID: {} | Referencia: {} | Estado: {}",
                guardado.getIdTransaccion(),
                guardado.getReferenciaExterna(),
                guardado.getEstado());

        return mapToResponse(guardado);
    }

    // PROCESAR
    @Override
    @Transactional
    public PagoDTO procesarPago(Long idTransaccion) {
        log.info("[SERVICE] Procesando pago ID: {}", idTransaccion);

        Pago pago = obtenerPorId(idTransaccion);
        validarMontoPorPagar(pago.getMontoTotal());
        validarEstadoPendiente(pago, EstadoPago.COMPLETADO);

        EstadoPago nuevoEstado = simularResultadoBancario();
        pago.setEstado(nuevoEstado);
        pago.setProcesadoEn(LocalDateTime.now());

        Pago actualizado = pagoRepository.save(pago);

        log.info("[SERVICE] Pago {} procesado. Resultado: {} | Procesado en: {}",
                idTransaccion, nuevoEstado, actualizado.getProcesadoEn());

        return mapToResponse(actualizado);
    }

    // ANULAR
    @Override
    @Transactional
    public PagoDTO anularPago(Long idTransaccion) {
        log.info("[SERVICE] Anulando pago ID: {}", idTransaccion);

        Pago pago = obtenerPorId(idTransaccion);
        validarEstadoPendiente(pago, EstadoPago.ANULADO);

        pago.setEstado(EstadoPago.ANULADO);
        pago.setProcesadoEn(LocalDateTime.now());

        Pago actualizado = pagoRepository.save(pago);

        log.info("[SERVICE] Pago {} anulado exitosamente", idTransaccion);
        return mapToResponse(actualizado);
    }

    // CONSULTAR POR ID
    @Override
    public PagoDTO consultarPorId(Long idTransaccion) {
        log.info("[SERVICE] Consultando pago ID: {}", idTransaccion);

        Pago pago = obtenerPorId(idTransaccion);

        log.info("[SERVICE] Pago encontrado. ID: {} | Estado: {} | Pedido: {}",
                idTransaccion, pago.getEstado(), pago.getIdPedido());

        return mapToResponse(pago);
    }

    // CONSULTAR POR PEDIDO
    @Override
    public PagoDTO consultarPorPedido(Long idPedido) {
        log.info("[SERVICE] Consultando pago del pedido ID: {}", idPedido);

        Pago pago = pagoRepository.findByIdPedido(idPedido)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] No existe pago para pedido ID {}", idPedido);
                    return new PagoNotFoundException(idPedido);
                });

        log.info("[SERVICE] Pago encontrado para pedido {}. Estado: {}",
                idPedido, pago.getEstado());

        return mapToResponse(pago);
    }

    // LISTAR TODOS
    @Override
    public List<PagoDTO> listarTodos() {
        log.info("[SERVICE] Listando todos los pagos");

        List<Pago> pagos = pagoRepository.findAll();

        log.info("[SERVICE] Total de pagos encontrados: {}", pagos.size());
        return pagos.stream().map(this::mapToResponse).toList();
    }

    // MÉTODOS AUXILIARES PRIVADOS
    private Pago obtenerPorId(Long idTransaccion) {
        return pagoRepository.findById(idTransaccion)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Pago ID {} no encontrado", idTransaccion);
                    return new PagoNotFoundException(idTransaccion);
                });
    }

    private void validarMontoPorPagar(BigDecimal montoTotal) {
        if (montoTotal == null || montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[SERVICE] Intento de pago sin monto por pagar. Monto recibido: {}", montoTotal);
            throw new NoHayMontoPorPagarException(montoTotal);
        }
    }

    private void validarPedidoSinPago(Long idPedido) {
        if (pagoRepository.findByIdPedido(idPedido).isPresent()) {
            log.warn("[SERVICE] Ya existe pago para pedido ID: {}", idPedido);
            throw new IllegalStateException("Ya existe un pago registrado para el pedido " + idPedido);
        }
    }

    private void validarEstadoPendiente(Pago pago, EstadoPago destino) {
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            log.warn("[SERVICE] Pago {} no esta en estado PENDIENTE. Estado actual: {}",
                    pago.getIdTransaccion(), pago.getEstado());
            throw new TransicionEstadoInvalidaException(pago.getEstado(), destino);
        }
    }

    private EstadoPago simularResultadoBancario() {
        // 80% COMPLETADO, 20% RECHAZADO
        boolean exitoso = Math.random() < 0.8;
        EstadoPago resultado = exitoso ? EstadoPago.COMPLETADO : EstadoPago.RECHAZADO;
        log.info("[SERVICE] Simulacion bancaria: {}", resultado);
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