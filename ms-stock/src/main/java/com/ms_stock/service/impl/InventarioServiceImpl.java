package com.ms_stock.service.impl;

import com.ms_stock.dto.ConfirmarReservaDTO;
import com.ms_stock.dto.InventarioDTO;
import com.ms_stock.dto.LiberarReservaDTO;
import com.ms_stock.dto.ReducirStockDTO;
import com.ms_stock.dto.ReponerStockDTO;
import com.ms_stock.dto.ReservarStockDTO;
import com.ms_stock.model.IdempotenciaKey;
import com.ms_stock.model.Inventario;
import com.ms_stock.exception.StockInsuficienteException;
import com.ms_stock.exception.VarianteNotFoundException;
import com.ms_stock.repository.IdempotenciaKeyRepository;
import com.ms_stock.repository.InventarioRepository;
import com.ms_stock.service.InventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioServiceImpl implements InventarioService {

    private final InventarioRepository inventarioRepository;
    private final IdempotenciaKeyRepository idempotenciaKeyRepository;

    // CONSULTAR
    @Override
    public InventarioDTO consultarPorVariante(Long idVariante) {
        log.info("[AUDIT] Consultando stock para variante ID: {}", idVariante);

        Inventario inventario = obtenerPorVariante(idVariante);

        log.info("[AUDIT] Stock encontrado para variante {}: disponible={}", idVariante,
                inventario.getCantidadDisponible());

        return mapToResponse(inventario);
    }

    // LISTAR
    @Override
    public List<InventarioDTO> listarTodo() {
        log.info("[AUDIT] Listando todo el inventario");

        List<Inventario> lista = inventarioRepository.findAll();

        log.info("[AUDIT] Total de registros encontrados: {}", lista.size());

        return lista.stream()
                .map(this::mapToResponse)
                .toList();
    }

    // CREAR
    @Override
    @Transactional
    public InventarioDTO crearInventario(Long idVariante) {
        log.info("[AUDIT] Creando registro de inventario para variante ID: {}", idVariante);

        validarVarianteSinInventario(idVariante);

        Inventario guardado = inventarioRepository.save(buildInventario(idVariante));

        log.info("[AUDIT] Inventario creado con ID: {} para variante: {}",
                guardado.getIdInventario(), idVariante);

        return mapToResponse(guardado);
    }

    // REPONER
    @Override
    @Transactional
    public InventarioDTO reponerStock(Long idVariante, ReponerStockDTO dto) {
        log.info("[AUDIT] Reponiendo {} unidades para variante ID: {}",
                dto.getCantidad(), idVariante);

        Inventario inventario = obtenerPorVariante(idVariante);
        Integer stockAnterior = inventario.getCantidadDisponible();

        inventario.setCantidadDisponible(stockAnterior + dto.getCantidad());
        Inventario actualizado = inventarioRepository.save(inventario);

        log.info("[AUDIT] Stock repuesto para variante {}. Anterior: {}, Nuevo: {}",
                idVariante, stockAnterior, actualizado.getCantidadDisponible());

        return mapToResponse(actualizado);
    }

    // REDUCIR (con idempotencia)
    @Override
    @Transactional
    public InventarioDTO reducirStock(Long idVariante, ReducirStockDTO dto) {
        log.info("[AUDIT] Reduciendo {} unidades de variante ID: {} — idempotencyKey={}",
                dto.getCantidad(), idVariante, dto.getIdempotencyKey());

        // Verificar idempotencia: si ya se procesó esta key, devolver resultado previo
        if (idempotenciaKeyRepository.existsByIdempotencyKey(dto.getIdempotencyKey())) {
            log.info("[AUDIT] idempotencyKey={} ya procesada. Retornando estado actual.", dto.getIdempotencyKey());
            return mapToResponse(obtenerPorVariante(idVariante));
        }

        Inventario inventario = obtenerPorVariante(idVariante);
        validarStockDisponibleSuficiente(inventario, dto.getCantidad(), idVariante);

        Integer stockAnterior = inventario.getCantidadDisponible();
        inventario.setCantidadDisponible(stockAnterior - dto.getCantidad());
        Inventario actualizado = inventarioRepository.save(inventario);

        // Registrar idempotencyKey
        idempotenciaKeyRepository.save(
            IdempotenciaKey.builder()
                .idempotencyKey(dto.getIdempotencyKey())
                .idVariante(idVariante)
                .operacion("REDUCIR_STOCK")
                .build()
        );

        log.info("[AUDIT] Stock reducido para variante {}. Anterior: {}, Nuevo: {}",
                idVariante, stockAnterior, actualizado.getCantidadDisponible());

        return mapToResponse(actualizado);
    }

    // RESERVAR (con idempotencia)
    @Override
    @Transactional
    public InventarioDTO reservarStock(Long idVariante, ReservarStockDTO dto) {
        log.info("[AUDIT] Reservando {} unidades de variante ID: {} — idempotencyKey={}",
                dto.getCantidad(), idVariante, dto.getIdempotencyKey());

        if (idempotenciaKeyRepository.existsByIdempotencyKey(dto.getIdempotencyKey())) {
            log.info("[AUDIT] idempotencyKey={} ya procesada. Retornando estado actual.", dto.getIdempotencyKey());
            return mapToResponse(obtenerPorVariante(idVariante));
        }

        Inventario inventario = obtenerPorVariante(idVariante);
        if (inventario.getCantidadDisponible() < dto.getCantidad()) {
            log.warn("[AUDIT] Stock insuficiente para reservar en variante {}. Disponible: {}, Solicitado: {}",
                    idVariante, inventario.getCantidadDisponible(), dto.getCantidad());
            throw new StockInsuficienteException(idVariante, dto.getCantidad(), Math.max(0, inventario.getCantidadDisponible()));
        }

        inventario.setCantidadReservada(inventario.getCantidadReservada() + dto.getCantidad());
        // Disminuimos el disponible para que no pueda ser usado por otros
        inventario.setCantidadDisponible(inventario.getCantidadDisponible() - dto.getCantidad());
        Inventario actualizado = inventarioRepository.save(inventario);

        idempotenciaKeyRepository.save(
            IdempotenciaKey.builder()
                .idempotencyKey(dto.getIdempotencyKey())
                .idVariante(idVariante)
                .operacion("RESERVAR_STOCK")
                .build()
        );

        log.info("[AUDIT] Stock reservado para variante {}. Disponible restante: {}, Reservado: {}",
                idVariante, actualizado.getCantidadDisponible(), actualizado.getCantidadReservada());

        return mapToResponse(actualizado);
    }

    // CONFIRMAR RESERVA (reduce reservada, el stock ya salio del disponible en el paso anterior)
    @Override
    @Transactional
    public InventarioDTO confirmarReserva(Long idVariante, ConfirmarReservaDTO dto) {
        log.info("[AUDIT] Confirmando reserva de {} unidades para variante ID: {} — idempotencyKey={}",
                dto.getCantidad(), idVariante, dto.getIdempotencyKey());

        if (idempotenciaKeyRepository.existsByIdempotencyKey(dto.getIdempotencyKey())) {
            log.info("[AUDIT] idempotencyKey={} ya procesada. Retornando estado actual.", dto.getIdempotencyKey());
            return mapToResponse(obtenerPorVariante(idVariante));
        }

        Inventario inventario = obtenerPorVariante(idVariante);
        if (inventario.getCantidadReservada() < dto.getCantidad()) {
            log.warn("[AUDIT] No hay suficiente stock reservado en variante {}. Reservado: {}, Solicitado: {}",
                    idVariante, inventario.getCantidadReservada(), dto.getCantidad());
            throw new StockInsuficienteException(idVariante, dto.getCantidad(), inventario.getCantidadReservada());
        }

        inventario.setCantidadReservada(inventario.getCantidadReservada() - dto.getCantidad());
        // NOTA: el disponible ya se redujo al reservar, no se vuelve a reducir aqui
        Inventario actualizado = inventarioRepository.save(inventario);

        idempotenciaKeyRepository.save(
            IdempotenciaKey.builder()
                .idempotencyKey(dto.getIdempotencyKey())
                .idVariante(idVariante)
                .operacion("CONFIRMAR_RESERVA")
                .build()
        );

        log.info("[AUDIT] Reserva confirmada para variante {}. Reservado restante: {}",
                idVariante, actualizado.getCantidadReservada());

        return mapToResponse(actualizado);
    }

    // LIBERAR RESERVA (devuelve al disponible lo reservado)
    @Override
    @Transactional
    public InventarioDTO liberarReserva(Long idVariante, LiberarReservaDTO dto) {
        log.info("[AUDIT] Liberando reserva de {} unidades para variante ID: {} — idempotencyKey={}",
                dto.getCantidad(), idVariante, dto.getIdempotencyKey());

        if (idempotenciaKeyRepository.existsByIdempotencyKey(dto.getIdempotencyKey())) {
            log.info("[AUDIT] idempotencyKey={} ya procesada. Retornando estado actual.", dto.getIdempotencyKey());
            return mapToResponse(obtenerPorVariante(idVariante));
        }

        Inventario inventario = obtenerPorVariante(idVariante);
        if (inventario.getCantidadReservada() < dto.getCantidad()) {
            log.warn("[AUDIT] No hay suficiente stock reservado para liberar en variante {}. Reservado: {}, Solicitado: {}",
                    idVariante, inventario.getCantidadReservada(), dto.getCantidad());
            throw new StockInsuficienteException(idVariante, dto.getCantidad(), inventario.getCantidadReservada());
        }

        inventario.setCantidadReservada(inventario.getCantidadReservada() - dto.getCantidad());
        inventario.setCantidadDisponible(inventario.getCantidadDisponible() + dto.getCantidad());
        Inventario actualizado = inventarioRepository.save(inventario);

        idempotenciaKeyRepository.save(
            IdempotenciaKey.builder()
                .idempotencyKey(dto.getIdempotencyKey())
                .idVariante(idVariante)
                .operacion("LIBERAR_RESERVA")
                .build()
        );

        log.info("[AUDIT] Reserva liberada para variante {}. Disponible: {}, Reservado: {}",
                idVariante, actualizado.getCantidadDisponible(), actualizado.getCantidadReservada());

        return mapToResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminarInventario(Long idVariante) {
        log.info("[AUDIT] Eliminando inventario de variante ID: {}", idVariante);
        Inventario inventario = obtenerPorVariante(idVariante);
        inventarioRepository.delete(inventario);
        log.info("[AUDIT] Inventario eliminado para variante ID: {}", idVariante);
    }

    // MÉTODOS AUXILIARES PRIVADOS
    private Inventario obtenerPorVariante(Long idVariante) {
        return inventarioRepository.findByIdVariante(idVariante)
                .orElseThrow(() -> {
                    log.warn("[AUDIT] Variante ID {} no encontrada en inventario", idVariante);
                    return new VarianteNotFoundException(idVariante);
                });
    }

    private void validarVarianteSinInventario(Long idVariante) {
        if (inventarioRepository.findByIdVariante(idVariante).isPresent()) {
            log.warn("[AUDIT] Ya existe inventario para variante ID: {}", idVariante);
            throw new IllegalStateException(
                "Ya existe un registro de inventario para la variante " + idVariante
            );
        }
    }

    private void validarStockDisponibleSuficiente(Inventario inventario, Integer cantidad, Long idVariante) {
        if (inventario.getCantidadDisponible() < cantidad) {
            log.warn("[AUDIT] Stock disponible insuficiente para variante {}. Disponible: {}, Solicitado: {}",
                    idVariante, inventario.getCantidadDisponible(), cantidad);
            throw new StockInsuficienteException(
                idVariante, cantidad, inventario.getCantidadDisponible()
            );
        }
    }

    private Inventario buildInventario(Long idVariante) {
        Inventario nuevo = new Inventario();
        nuevo.setIdVariante(idVariante);
        nuevo.setCantidadDisponible(0);
        nuevo.setCantidadReservada(0);
        return nuevo;
    }

    private InventarioDTO mapToResponse(Inventario inventario) {
        return new InventarioDTO(
            inventario.getIdInventario(),
            inventario.getIdVariante(),
            inventario.getCantidadDisponible(),
            inventario.getCantidadReservada()
        );
    }
}