package com.ms_envios.service;

import com.ms_envios.Exception.EnvioNotFoundException;
import com.ms_envios.Exception.TransicionEstadoInvalidaException;
import com.ms_envios.dto.AvanzarEstadoDTO;
import com.ms_envios.dto.CrearEnvioDTO;
import com.ms_envios.dto.EnvioDTO;
import com.ms_envios.model.Envio;
import com.ms_envios.model.EstadoEnvio;
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

    private void validarTransicion(EstadoEnvio actual, EstadoEnvio solicitado) {
        boolean valida = switch (actual) {
            case PENDIENTE       -> solicitado == EstadoEnvio.EN_PREPARACION
                                 || solicitado == EstadoEnvio.CANCELADO;
            case EN_PREPARACION  -> solicitado == EstadoEnvio.DESPACHADO
                                 || solicitado == EstadoEnvio.CANCELADO;
            case DESPACHADO      -> solicitado == EstadoEnvio.EN_CAMINO;
            case EN_CAMINO       -> solicitado == EstadoEnvio.ENTREGADO;
            // ENTREGADO y CANCELADO son terminales — ninguna transición es válida
            case ENTREGADO, CANCELADO -> false;
        };

        if (!valida) {
            throw new TransicionEstadoInvalidaException(actual, solicitado);
        }
    }

    @Transactional
    public EnvioDTO crearEnvio(CrearEnvioDTO dto) {
        log.info("[SERVICE] Creando envio para pedido ID: {} | Courier: {} | Destino: {}",
                dto.getIdPedido(), dto.getCourier(), dto.getDireccionDestino());

        try {
            // Verificar que no exista ya un envío para este pedido
            if (envioRepository.findByIdPedido(dto.getIdPedido()).isPresent()) {
                log.warn("[SERVICE] Ya existe un envio para el pedido ID: {}", dto.getIdPedido());
                throw new IllegalStateException(
                    "Ya existe un envio registrado para el pedido " + dto.getIdPedido()
                );
            }

            Envio envio = new Envio();
            envio.setIdPedido(dto.getIdPedido());
            envio.setDireccionDestino(dto.getDireccionDestino());
            envio.setCourier(dto.getCourier());

            // Tracking simulado — en producción vendría del courier real
            envio.setNumeroTracking("TRK-" + UUID.randomUUID()
                    .toString().substring(0, 8).toUpperCase());

            // Entrega estimada: 5 días hábiles desde hoy
            envio.setEntregaEstimada(LocalDate.now().plusDays(5));

            // estado y fechaCreacion los asigna @PrePersist

            Envio guardado = envioRepository.save(envio);

            log.info("[SERVICE] Envio creado exitosamente. ID: {} | Tracking: {} | Estimado: {}",
                    guardado.getIdEnvio(),
                    guardado.getNumeroTracking(),
                    guardado.getEntregaEstimada());

            return mapToDTO(guardado);

        } catch (IllegalStateException e) {
            log.warn("[SERVICE] Intento de crear envio duplicado para pedido {}",
                    dto.getIdPedido());
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al crear envio para pedido {}: {}",
                    dto.getIdPedido(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public EnvioDTO avanzarEstado(Long idEnvio, AvanzarEstadoDTO dto) {
        log.info("[SERVICE] Cambiando estado del envio {} a {}",
                idEnvio, dto.getEstado());

        try {
            Envio envio = envioRepository.findById(idEnvio)
                    .orElseThrow(() -> new EnvioNotFoundException(idEnvio));

            EstadoEnvio estadoActual    = envio.getEstado();
            EstadoEnvio estadoSolicitado = dto.getEstado();

            // Valida que la transición sea permitida
            validarTransicion(estadoActual, estadoSolicitado);

            envio.setEstado(estadoSolicitado);

            // Si el paquete fue entregado, registrar la fecha exacta
            if (estadoSolicitado == EstadoEnvio.ENTREGADO) {
                envio.setEntregadoEn(LocalDateTime.now());
                log.info("[SERVICE] Envio {} marcado como ENTREGADO en: {}",
                        idEnvio, envio.getEntregadoEn());
            }

            Envio actualizado = envioRepository.save(envio);

            log.info("[SERVICE] Estado del envio {} actualizado: {} → {}",
                    idEnvio, estadoActual, estadoSolicitado);

            return mapToDTO(actualizado);

        } catch (EnvioNotFoundException | TransicionEstadoInvalidaException e) {
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al avanzar estado del envio {}: {}",
                    idEnvio, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public EnvioDTO cancelarEnvio(Long idEnvio) {
        log.info("[SERVICE] Cancelando envio ID: {}", idEnvio);

        try {
            Envio envio = envioRepository.findById(idEnvio)
                    .orElseThrow(() -> new EnvioNotFoundException(idEnvio));

            // Reutiliza la misma lógica de validación
            validarTransicion(envio.getEstado(), EstadoEnvio.CANCELADO);

            EstadoEnvio estadoAnterior = envio.getEstado();
            envio.setEstado(EstadoEnvio.CANCELADO);

            Envio actualizado = envioRepository.save(envio);

            log.info("[SERVICE] Envio {} cancelado exitosamente. Estado anterior: {}",
                    idEnvio, estadoAnterior);

            return mapToDTO(actualizado);

        } catch (EnvioNotFoundException | TransicionEstadoInvalidaException e) {
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al cancelar envio {}: {}",
                    idEnvio, e.getMessage(), e);
            throw e;
        }
    }

    public EnvioDTO consultarPorId(Long idEnvio) {
        log.info("[SERVICE] Consultando envio ID: {}", idEnvio);

        try {
            Envio envio = envioRepository.findById(idEnvio)
                    .orElseThrow(() -> new EnvioNotFoundException(idEnvio));

            log.info("[SERVICE] Envio {} encontrado. Estado: {} | Tracking: {}",
                    idEnvio, envio.getEstado(), envio.getNumeroTracking());

            return mapToDTO(envio);

        } catch (EnvioNotFoundException e) {
            log.warn("[SERVICE] Envio ID {} no encontrado", idEnvio);
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al consultar envio {}: {}",
                    idEnvio, e.getMessage(), e);
            throw e;
        }
    }

    public EnvioDTO consultarPorPedido(Long idPedido) {
        log.info("[SERVICE] Consultando envio del pedido ID: {}", idPedido);

        try {
            Envio envio = envioRepository.findByIdPedido(idPedido)
                    .orElseThrow(() -> new EnvioNotFoundException(idPedido));

            log.info("[SERVICE] Envio encontrado para pedido {}. Estado: {} | Tracking: {}",
                    idPedido, envio.getEstado(), envio.getNumeroTracking());

            return mapToDTO(envio);

        } catch (EnvioNotFoundException e) {
            log.warn("[SERVICE] No existe envio para pedido ID {}", idPedido);
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al consultar envio del pedido {}: {}",
                    idPedido, e.getMessage(), e);
            throw e;
        }
    }

    public List<EnvioDTO> listarTodos() {
        log.info("[SERVICE] Listando todos los envios");

        try {
            List<Envio> envios = envioRepository.findAll();
            log.info("[SERVICE] Total de envios encontrados: {}", envios.size());
            return envios.stream().map(this::mapToDTO).toList();

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al listar envios: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<EnvioDTO> listarPorEstado(EstadoEnvio estado) {
        log.info("[SERVICE] Listando envios con estado: {}", estado);

        try {
            List<Envio> envios = envioRepository.findByEstado(estado);
            log.info("[SERVICE] Envios encontrados con estado {}: {}", estado, envios.size());
            return envios.stream().map(this::mapToDTO).toList();

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al listar envios por estado {}: {}",
                    estado, e.getMessage(), e);
            throw e;
        }
    }

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
