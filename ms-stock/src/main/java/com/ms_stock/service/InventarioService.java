package com.ms_stock.service;

import com.ms_stock.dto.InventarioDTO;
import com.ms_stock.dto.ReducirStockDTO;
import com.ms_stock.dto.ReponerStockDTO;
import com.ms_stock.model.Inventario;
import com.ms_stock.exception.StockInsuficienteException;
import com.ms_stock.exception.VarianteNotFoundException;
import com.ms_stock.repository.InventarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioRepository inventarioRepository;

    public InventarioDTO consultarPorVariante(Long idVariante) {
        log.info("[SERVICE] Consultando stock para variante ID: {}", idVariante);

        try {
            Inventario inventario = inventarioRepository
                    .findByIdVariante(idVariante)
                    .orElseThrow(() -> new VarianteNotFoundException(idVariante));

            log.info("[SERVICE] Stock encontrado para variante {}: disponible={}, reservado={}",
                    idVariante,
                    inventario.getCantidadDisponible(),
                    inventario.getCantidadReservada());

            return mapToDTO(inventario);

        } catch (VarianteNotFoundException e) {
            log.warn("[SERVICE] Variante ID {} no encontrada en inventario", idVariante);
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al consultar variante {}: {}", idVariante, e.getMessage(), e);
            throw e;
        }
    }

    public List<InventarioDTO> listarTodo() {
        log.info("[SERVICE] Listando todo el inventario");

        try {
            List<Inventario> lista = inventarioRepository.findAll();
            log.info("[SERVICE] Total de registros encontrados: {}", lista.size());
            return lista.stream().map(this::mapToDTO).toList();

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al listar inventario: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public InventarioDTO crearInventario(Long idVariante) {
        log.info("[SERVICE] Creando registro de inventario para variante ID: {}", idVariante);

        try {
            if (inventarioRepository.findByIdVariante(idVariante).isPresent()) {
                log.warn("[SERVICE] Ya existe inventario para variante ID: {}", idVariante);
                throw new IllegalStateException(
                    "Ya existe un registro de inventario para la variante " + idVariante
                );
            }

            Inventario nuevo = new Inventario();
            nuevo.setIdVariante(idVariante);
            nuevo.setCantidadDisponible(0);
            nuevo.setCantidadReservada(0);

            Inventario guardado = inventarioRepository.save(nuevo);

            log.info("[SERVICE] Inventario creado con ID: {} para variante: {}",
                    guardado.getIdInventario(), idVariante);

            return mapToDTO(guardado);

        } catch (IllegalStateException e) {
            log.warn("[SERVICE] Intento de crear inventario duplicado para variante {}", idVariante);
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al crear inventario para variante {}: {}",
                    idVariante, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public InventarioDTO reponerStock(Long idVariante, ReponerStockDTO dto) {
        log.info("[SERVICE] Reponiendo {} unidades para variante ID: {}",
                dto.getCantidad(), idVariante);

        try {
            Inventario inventario = inventarioRepository
                    .findByIdVariante(idVariante)
                    .orElseThrow(() -> new VarianteNotFoundException(idVariante));

            Integer stockAnterior = inventario.getCantidadDisponible();
            inventario.setCantidadDisponible(stockAnterior + dto.getCantidad());

            Inventario actualizado = inventarioRepository.save(inventario);

            log.info("[SERVICE] Stock repuesto para variante {}. Anterior: {}, Nuevo: {}",
                    idVariante, stockAnterior, actualizado.getCantidadDisponible());

            return mapToDTO(actualizado);

        } catch (VarianteNotFoundException e) {
            log.warn("[SERVICE] Variante {} no encontrada al intentar reponer stock", idVariante);
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al reponer stock de variante {}: {}",
                    idVariante, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public InventarioDTO reducirStock(Long idVariante, ReducirStockDTO dto) {
        log.info("[SERVICE] Reduciendo {} unidades de variante ID: {}",
                dto.getCantidad(), idVariante);

        try {
            Inventario inventario = inventarioRepository
                    .findByIdVariante(idVariante)
                    .orElseThrow(() -> new VarianteNotFoundException(idVariante));

            if (inventario.getCantidadDisponible() < dto.getCantidad()) {
                log.warn("[SERVICE] Stock insuficiente para variante {}. Disponible: {}, Solicitado: {}",
                        idVariante,
                        inventario.getCantidadDisponible(),
                        dto.getCantidad());
                throw new StockInsuficienteException(
                    idVariante,
                    dto.getCantidad(),
                    inventario.getCantidadDisponible()
                );
            }

            Integer stockAnterior = inventario.getCantidadDisponible();
            inventario.setCantidadDisponible(stockAnterior - dto.getCantidad());

            Inventario actualizado = inventarioRepository.save(inventario);

            log.info("[SERVICE] Stock reducido para variante {}. Anterior: {}, Nuevo: {}",
                    idVariante, stockAnterior, actualizado.getCantidadDisponible());

            return mapToDTO(actualizado);

        } catch (StockInsuficienteException | VarianteNotFoundException e) {
            // Excepciones de negocio — ya logueadas arriba, solo se relanzan
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al reducir stock de variante {}: {}",
                    idVariante, e.getMessage(), e);
            throw e;
        }
    }

    private InventarioDTO mapToDTO(Inventario inventario) {
        return new InventarioDTO(
            inventario.getIdInventario(),
            inventario.getIdVariante(),
            inventario.getCantidadDisponible(),
            inventario.getCantidadReservada()
        );
    }
}
