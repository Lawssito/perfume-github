package com.ms_envios.service;

import com.ms_envios.dto.AvanzarEstadoDTO;
import com.ms_envios.dto.CrearEnvioDTO;
import com.ms_envios.dto.EnvioDTO;
import com.ms_envios.model.Envio;
import com.ms_envios.model.EstadoEnvio;
import com.ms_envios.exception.EnvioNotFoundException;
import com.ms_envios.exception.TransicionEstadoInvalidaException;
import com.ms_envios.repository.EnvioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvioService {

    private final EnvioRepository envioRepository;

    // ─────────────────────────────────────────────────────────
    // HELPER — validar transiciones
    // ─────────────────────────────────────────────────────────

    private void validarTransicion(EstadoEnvio actual, EstadoEnvio solicitado) {
        boolean valida = switch (actual) {
            case PENDIENTE      -> solicitado == EstadoEnvio.EN_PREPARACION
                                || solicitado == EstadoEnvio.CANCELADO;
            case EN_PREPARACION -> solicitado == EstadoEnvio.DESPACHADO
                                || solicitado == EstadoEnvio.CANCELADO;
            case DESPACHADO     -> solicitado == EstadoEnvio.EN_CAMINO;
            case EN_CAMINO      -> solicitado == EstadoEnvio.ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };

        if (!valida) {
            throw new TransicionEstadoInvalidaException(actual, solicitado);
        }
    }

    // CREAR ENVÍO
    @Transactional
    public EnvioDTO crearEnvio(CrearEnvioDTO dto) {
        log.info("[SERVICE] Creando envio para pedido ID: {} | Courier: {} | Destino: {}",
                dto.getIdPedido(), dto.getCourier(), dto.getDireccionDestino());

        // Verificar duplicado explícitamente
        // DataIntegrityViolationException actúa como respaldo en condición de carrera
        if (envioRepository.findByIdPedido(dto.getIdPedido()).isPresent()) {
            log.warn("[SERVICE] Ya existe envio para pedido ID: {}", dto.getIdPedido());
            throw new IllegalStateException(
                "Ya existe un envio registrado para el pedido " + dto.getIdPedido()
            );
        }

        Envio envio = new Envio();
        envio.setIdPedido(dto.getIdPedido());
        envio.setDireccionDestino(dto.getDireccionDestino());
        envio.setCourier(dto.getCourier());
        envio.setNumeroTracking("TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
        envio.setEntregaEstimada(LocalDate.now().plusDays(5));

        Envio guardado = envioRepository.save(envio);

        log.info("[SERVICE] Envio creado exitosamente. ID: {} | Tracking: {} | Estimado: {}",
                guardado.getIdEnvio(),
                guardado.getNumeroTracking(),
                guardado.getEntregaEstimada());

        return mapToDTO(guardado);
    }

    // AVANZAR ESTADO
    @Transactional
    public EnvioDTO avanzarEstado(Long idEnvio, AvanzarEstadoDTO dto) {
        log.info("[SERVICE] Cambiando estado del envio {} a {}", idEnvio, dto.getEstado());

        Envio envio = envioRepository.findById(idEnvio)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Envio ID {} no encontrado al avanzar estado", idEnvio);
                    return new EnvioNotFoundException(idEnvio);
                });

        EstadoEnvio estadoActual    = envio.getEstado();
        EstadoEnvio estadoSolicitado = dto.getEstado();

        validarTransicion(estadoActual, estadoSolicitado);

        envio.setEstado(estadoSolicitado);

        if (estadoSolicitado == EstadoEnvio.ENTREGADO) {
            envio.setEntregadoEn(LocalDateTime.now());
            log.info("[SERVICE] Envio {} marcado como ENTREGADO en: {}",
                    idEnvio, envio.getEntregadoEn());
        }

        Envio actualizado = envioRepository.save(envio);

        log.info("[SERVICE] Estado del envio {} actualizado: {} → {}",
                idEnvio, estadoActual, estadoSolicitado);

        return mapToDTO(actualizado);
    }

    // CANCELAR ENVÍO
    @Transactional
    public EnvioDTO cancelarEnvio(Long idEnvio) {
        log.info("[SERVICE] Cancelando envio ID: {}", idEnvio);

        Envio envio = envioRepository.findById(idEnvio)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Envio ID {} no encontrado al cancelar", idEnvio);
                    return new EnvioNotFoundException(idEnvio);
                });

        EstadoEnvio estadoAnterior = envio.getEstado();
        validarTransicion(estadoAnterior, EstadoEnvio.CANCELADO);

        envio.setEstado(EstadoEnvio.CANCELADO);
        Envio actualizado = envioRepository.save(envio);

        log.info("[SERVICE] Envio {} cancelado. Estado anterior: {}", idEnvio, estadoAnterior);
        return mapToDTO(actualizado);
    }

    // CONSULTAR POR ID
    public EnvioDTO consultarPorId(Long idEnvio) {
        log.info("[SERVICE] Consultando envio ID: {}", idEnvio);

        Envio envio = envioRepository.findById(idEnvio)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Envio ID {} no encontrado", idEnvio);
                    return new EnvioNotFoundException(idEnvio);
                });

        log.info("[SERVICE] Envio {} encontrado. Estado: {} | Tracking: {}",
                idEnvio, envio.getEstado(), envio.getNumeroTracking());

        return mapToDTO(envio);
    }

    // CONSULTAR POR PEDIDO
    public EnvioDTO consultarPorPedido(Long idPedido) {
        log.info("[SERVICE] Consultando envio del pedido ID: {}", idPedido);

        Envio envio = envioRepository.findByIdPedido(idPedido)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] No existe envio para pedido ID {}", idPedido);
                    return new EnvioNotFoundException(idPedido);
                });

        log.info("[SERVICE] Envio encontrado para pedido {}. Estado: {} | Tracking: {}",
                idPedido, envio.getEstado(), envio.getNumeroTracking());

        return mapToDTO(envio);
    }

    // LISTAR TODOS
    public List<EnvioDTO> listarTodos() {
        log.info("[SERVICE] Listando todos los envios");

        List<Envio> envios = envioRepository.findAll();

        log.info("[SERVICE] Total de envios encontrados: {}", envios.size());
        return envios.stream().map(this::mapToDTO).toList();
    }

    // LISTAR POR ESTADO
    public List<EnvioDTO> listarPorEstado(EstadoEnvio estado) {
        log.info("[SERVICE] Listando envios con estado: {}", estado);

        List<Envio> envios = envioRepository.findByEstado(estado);

        log.info("[SERVICE] Envios encontrados con estado {}: {}", estado, envios.size());
        return envios.stream().map(this::mapToDTO).toList();
    }

    // MAPPER
    private EnvioDTO mapToDTO(Envio envio) {
        return new EnvioDTO(
            envio.getIdEnvio(),
            envio.getIdPedido(),
            envio.getDireccionDestino(),
            envio.getEstado(),
            envio.getNumeroTracking(),
            envio.getCourier(),
            envio.getFechaCreacion(),
            envio.getEntregaEstimada(),
            envio.getEntregadoEn()
        );
    }
}