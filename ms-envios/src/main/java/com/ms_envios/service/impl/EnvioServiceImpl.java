package com.ms_envios.service.impl;

import com.ms_envios.dto.AvanzarEstadoDTO;
import com.ms_envios.dto.CrearEnvioDTO;
import com.ms_envios.dto.EnvioDTO;
import com.ms_envios.model.Envio;
import com.ms_envios.model.EstadoEnvio;
import com.ms_envios.exception.EnvioNotFoundException;
import com.ms_envios.exception.TransicionEstadoInvalidaException;
import com.ms_envios.repository.EnvioRepository;
import com.ms_envios.service.EnvioService;
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
public class EnvioServiceImpl implements EnvioService {

    private final EnvioRepository envioRepository;

    // CREAR
    @Override
    @Transactional
    public EnvioDTO crearEnvio(CrearEnvioDTO dto) {
        log.info("[SERVICE] Creando envio para pedido ID: {} | Courier: {}",
                dto.getIdPedido(), dto.getCourier());

        validarPedidoSinEnvio(dto.getIdPedido());

        Envio guardado = envioRepository.save(buildEnvio(dto));

        log.info("[SERVICE] Envio creado exitosamente. ID: {} | Tracking: {} | Estimado: {}",
                guardado.getIdEnvio(),
                guardado.getNumeroTracking(),
                guardado.getEntregaEstimada());

        return mapToResponse(guardado);
    }

    // AVANZAR ESTADO
    @Override
    @Transactional
    public EnvioDTO avanzarEstado(Long idEnvio, AvanzarEstadoDTO dto) {
        log.info("[SERVICE] Cambiando estado del envio {} a {}", idEnvio, dto.getEstado());

        Envio envio        = obtenerPorId(idEnvio);
        EstadoEnvio actual = envio.getEstado();

        validarTransicion(actual, dto.getEstado());
        envio.setEstado(dto.getEstado());

        if (dto.getEstado() == EstadoEnvio.ENTREGADO) {
            envio.setEntregadoEn(LocalDateTime.now());
            log.info("[SERVICE] Envio {} marcado como ENTREGADO en: {}",
                    idEnvio, envio.getEntregadoEn());
        }

        Envio actualizado = envioRepository.save(envio);

        log.info("[SERVICE] Estado del envio {} actualizado: {} → {}",
                idEnvio, actual, dto.getEstado());

        return mapToResponse(actualizado);
    }

    // CANCELAR
    @Override
    @Transactional
    public EnvioDTO cancelarEnvio(Long idEnvio) {
        log.info("[SERVICE] Cancelando envio ID: {}", idEnvio);

        Envio envio            = obtenerPorId(idEnvio);
        EstadoEnvio estadoAnterior = envio.getEstado();

        validarTransicion(estadoAnterior, EstadoEnvio.CANCELADO);
        envio.setEstado(EstadoEnvio.CANCELADO);

        Envio actualizado = envioRepository.save(envio);

        log.info("[SERVICE] Envio {} cancelado. Estado anterior: {}", idEnvio, estadoAnterior);
        return mapToResponse(actualizado);
    }

    // CONSULTAR POR ID
    @Override
    public EnvioDTO consultarPorId(Long idEnvio) {
        log.info("[SERVICE] Consultando envio ID: {}", idEnvio);

        Envio envio = obtenerPorId(idEnvio);

        log.info("[SERVICE] Envio {} encontrado. Estado: {} | Tracking: {}",
                idEnvio, envio.getEstado(), envio.getNumeroTracking());

        return mapToResponse(envio);
    }

    // CONSULTAR POR PEDIDO
    @Override
    public EnvioDTO consultarPorPedido(Long idPedido) {
        log.info("[SERVICE] Consultando envio del pedido ID: {}", idPedido);

        Envio envio = envioRepository.findByIdPedido(idPedido)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] No existe envio para pedido ID {}", idPedido);
                    return new EnvioNotFoundException(idPedido);
                });

        log.info("[SERVICE] Envio encontrado para pedido {}. Estado: {} | Tracking: {}",
                idPedido, envio.getEstado(), envio.getNumeroTracking());

        return mapToResponse(envio);
    }

    // LISTAR TODOS
    @Override
    public List<EnvioDTO> listarTodos() {
        log.info("[SERVICE] Listando todos los envios");

        List<Envio> envios = envioRepository.findAll();

        log.info("[SERVICE] Total de envios encontrados: {}", envios.size());
        return envios.stream().map(this::mapToResponse).toList();
    }

    // LISTAR POR ESTADO
    @Override
    public List<EnvioDTO> listarPorEstado(EstadoEnvio estado) {
        log.info("[SERVICE] Listando envios con estado: {}", estado);

        List<Envio> envios = envioRepository.findByEstado(estado);

        log.info("[SERVICE] Envios encontrados con estado {}: {}", estado, envios.size());
        return envios.stream().map(this::mapToResponse).toList();
    }

    // MÉTODOS AUXILIARES PRIVADOS
    private Envio obtenerPorId(Long idEnvio) {
        return envioRepository.findById(idEnvio)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Envio ID {} no encontrado", idEnvio);
                    return new EnvioNotFoundException(idEnvio);
                });
    }

    private void validarPedidoSinEnvio(Long idPedido) {
        if (envioRepository.findByIdPedido(idPedido).isPresent()) {
            log.warn("[SERVICE] Ya existe envio para pedido ID: {}", idPedido);
            throw new IllegalStateException(
                "Ya existe un envio registrado para el pedido " + idPedido
            );
        }
    }

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
            log.warn("[SERVICE] Transicion invalida: {} → {}", actual, solicitado);
            throw new TransicionEstadoInvalidaException(actual, solicitado);
        }
    }

    private Envio buildEnvio(CrearEnvioDTO dto) {
        Envio envio = new Envio();
        envio.setIdPedido(dto.getIdPedido());
        envio.setDireccionDestino(dto.getDireccionDestino());
        envio.setCourier(dto.getCourier());
        envio.setNumeroTracking(
            "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
        envio.setEntregaEstimada(LocalDate.now().plusDays(5));
        return envio;
    }

    private EnvioDTO mapToResponse(Envio envio) {
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