package com.ms_stock.service.impl;

import com.ms_stock.dto.InventarioDTO;
import com.ms_stock.dto.ReducirStockDTO;
import com.ms_stock.dto.ReponerStockDTO;
import com.ms_stock.model.Inventario;
import com.ms_stock.exception.StockInsuficienteException;
import com.ms_stock.exception.VarianteNotFoundException;
import com.ms_stock.repository.InventarioRepository;
import com.ms_stock.service.InventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioServiceImpl implements InventarioService {

    private final InventarioRepository inventarioRepository;

    // CONSULTAR
    @Override
    public InventarioDTO consultarPorVariante(Long idVariante) {
        log.info("[SERVICE] Consultando stock para variante ID: {}", idVariante);

        Inventario inventario = obtenerPorVariante(idVariante);

        log.info("[SERVICE] Stock encontrado para variante {}: disponible={}, reservado={}",
                idVariante,
                inventario.getCantidadDisponible(),
                inventario.getCantidadReservada());

        return mapToResponse(inventario);
    }

    // LISTAR
    @Override
    public List<InventarioDTO> listarTodo() {
        log.info("[SERVICE] Listando todo el inventario");

        List<Inventario> lista = inventarioRepository.findAll();

        log.info("[SERVICE] Total de registros encontrados: {}", lista.size());

        return lista.stream()
                .map(this::mapToResponse)
                .toList();
    }

    // CREAR
    @Override
    @Transactional
    public InventarioDTO crearInventario(Long idVariante) {
        log.info("[SERVICE] Creando registro de inventario para variante ID: {}", idVariante);

        validarVarianteSinInventario(idVariante);

        Inventario guardado = inventarioRepository.save(buildInventario(idVariante));

        log.info("[SERVICE] Inventario creado con ID: {} para variante: {}",
                guardado.getIdInventario(), idVariante);

        return mapToResponse(guardado);
    }

    // REPONER
    @Override
    @Transactional
    public InventarioDTO reponerStock(Long idVariante, ReponerStockDTO dto) {
        log.info("[SERVICE] Reponiendo {} unidades para variante ID: {}",
                dto.getCantidad(), idVariante);

        Inventario inventario = obtenerPorVariante(idVariante);
        Integer stockAnterior = inventario.getCantidadDisponible();

        inventario.setCantidadDisponible(stockAnterior + dto.getCantidad());
        Inventario actualizado = inventarioRepository.save(inventario);

        log.info("[SERVICE] Stock repuesto para variante {}. Anterior: {}, Nuevo: {}",
                idVariante, stockAnterior, actualizado.getCantidadDisponible());

        return mapToResponse(actualizado);
    }

    // REDUCIR
    @Override
    @Transactional
    public InventarioDTO reducirStock(Long idVariante, ReducirStockDTO dto) {
        log.info("[SERVICE] Reduciendo {} unidades de variante ID: {}",
                dto.getCantidad(), idVariante);

        Inventario inventario = obtenerPorVariante(idVariante);
        validarStockSuficiente(inventario, dto.getCantidad(), idVariante);

        Integer stockAnterior = inventario.getCantidadDisponible();
        inventario.setCantidadDisponible(stockAnterior - dto.getCantidad());
        Inventario actualizado = inventarioRepository.save(inventario);

        log.info("[SERVICE] Stock reducido para variante {}. Anterior: {}, Nuevo: {}",
                idVariante, stockAnterior, actualizado.getCantidadDisponible());

        return mapToResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminarInventario(Long idVariante) {
        log.info("[SERVICE] Eliminando inventario de variante ID: {}", idVariante);
        Inventario inventario = obtenerPorVariante(idVariante);
        inventarioRepository.delete(inventario);
        log.info("[SERVICE] Inventario eliminado para variante ID: {}", idVariante);
    }

    // MÉTODOS AUXILIARES PRIVADOS
    private Inventario obtenerPorVariante(Long idVariante) {
        return inventarioRepository.findByIdVariante(idVariante)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Variante ID {} no encontrada en inventario", idVariante);
                    return new VarianteNotFoundException(idVariante);
                });
    }

    private void validarVarianteSinInventario(Long idVariante) {
        if (inventarioRepository.findByIdVariante(idVariante).isPresent()) {
            log.warn("[SERVICE] Ya existe inventario para variante ID: {}", idVariante);
            throw new IllegalStateException(
                "Ya existe un registro de inventario para la variante " + idVariante
            );
        }
    }

    private void validarStockSuficiente(Inventario inventario, Integer cantidad, Long idVariante) {
        if (inventario.getCantidadDisponible() < cantidad) {
            log.warn("[SERVICE] Stock insuficiente para variante {}. Disponible: {}, Solicitado: {}",
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