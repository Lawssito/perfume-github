package com.ms_carrito.service.impl;

import com.ms_carrito.client.CatalogoClient;
import com.ms_carrito.client.StockClient;
import com.ms_carrito.client.StockResponseDTO;
import com.ms_carrito.client.VarianteResponseDTO;
import com.ms_carrito.dto.*;
import com.ms_carrito.model.Carrito;
import com.ms_carrito.model.ItemCarrito;
import com.ms_carrito.exception.*;
import com.ms_carrito.repository.CarritoRepository;
import com.ms_carrito.repository.ItemCarritoRepository;
import com.ms_carrito.service.CarritoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarritoServiceImpl implements CarritoService {

    private final CarritoRepository     carritoRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private final StockClient           stockClient;
    private final CatalogoClient        catalogoClient;

    @Override
    @Transactional
    public CarritoDTO obtenerOCrearCarrito(Long idUsuario) {
        log.info("[AUDIT idUsuario={}] Obteniendo carrito", idUsuario);
        Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                .orElseGet(() -> crearNuevoCarrito(idUsuario));
        log.info("[AUDIT idUsuario={}] Carrito ID {} retornado", idUsuario, carrito.getIdCarrito());
        return mapToResponse(carrito);
    }

    @Override
    @Transactional
    public CarritoDTO agregarItem(Long idUsuario, AgregarItemDTO dto) {
        log.info("[AUDIT idUsuario={}] Agregando variante {} x{}", idUsuario, dto.getIdVariante(), dto.getCantidad());
        validarStockDisponible(dto.getIdVariante(), dto.getCantidad());
        BigDecimal precio = obtenerPrecioVariante(dto.getIdVariante());
        Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                .orElseGet(() -> crearNuevoCarrito(idUsuario));
        agregarOActualizarItem(carrito, dto.getIdVariante(), dto.getCantidad(), precio);
        Carrito actualizado = obtenerCarritoPorUsuario(idUsuario);
        log.info("[AUDIT idUsuario={}] Item agregado exitosamente", idUsuario);
        return mapToResponse(actualizado);
    }

    @Override
    @Transactional
    public CarritoDTO actualizarCantidad(Long idUsuario, Long idItem, ActualizarCantidadDTO dto) {
        log.info("[AUDIT idUsuario={}] Actualizando item {} cantidad → {}", idUsuario, idItem, dto.getCantidad());
        Carrito carrito = obtenerCarritoPorUsuario(idUsuario);
        ItemCarrito item = obtenerItemDelCarrito(idItem, carrito.getIdCarrito());
        validarStockDisponible(item.getIdVariante(), dto.getCantidad());
        Integer cantidadAnterior = item.getCantidad();
        item.setCantidad(dto.getCantidad());
        itemCarritoRepository.save(item);
        log.info("[AUDIT idUsuario={}] Item {} actualizado: {} → {}", idUsuario, idItem, cantidadAnterior, dto.getCantidad());
        return mapToResponse(obtenerCarritoPorUsuario(idUsuario));
    }

    @Override
    @Transactional
    public CarritoDTO eliminarItem(Long idUsuario, Long idItem) {
        log.info("[AUDIT idUsuario={}] Eliminando item {}", idUsuario, idItem);
        Carrito carrito = obtenerCarritoPorUsuario(idUsuario);
        ItemCarrito item = obtenerItemDelCarrito(idItem, carrito.getIdCarrito());
        itemCarritoRepository.deleteById(item.getIdItem());
        itemCarritoRepository.flush();

        log.info("[AUDIT idUsuario={}] Item {} eliminado exitosamente del carrito", idUsuario, idItem);

        return mapToResponse(obtenerCarritoPorUsuario(idUsuario));
    }


    @Override
    @Transactional
    public void vaciarCarrito(Long idUsuario) {
        log.info("[AUDIT idUsuario={}] Vaciando carrito completo", idUsuario);
        Carrito carrito = obtenerCarritoPorUsuario(idUsuario);
        carrito.getItems().clear();
        carritoRepository.save(carrito);
        log.info("[AUDIT idUsuario={}] Carrito {} vaciado", idUsuario, carrito.getIdCarrito());
    }

    private Carrito crearNuevoCarrito(Long idUsuario) {
        log.info("[AUDIT idUsuario={}] Creando nuevo carrito", idUsuario);
        Carrito nuevo = new Carrito();
        nuevo.setIdUsuario(idUsuario);
        return carritoRepository.save(nuevo);
    }

    private Carrito obtenerCarritoPorUsuario(Long idUsuario) {
        return carritoRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> {
                    log.warn("[AUDIT idUsuario={}] Carrito no encontrado", idUsuario);
                    return new CarritoNotFoundException(idUsuario);
                });
    }

    private ItemCarrito obtenerItemDelCarrito(Long idItem, Long idCarrito) {
        return itemCarritoRepository.findById(idItem)
                .filter(i -> i.getCarrito().getIdCarrito().equals(idCarrito))
                .orElseThrow(() -> {
                    log.warn("[AUDIT] Item {} no encontrado en carrito {}", idItem, idCarrito);
                    return new ItemNotFoundException(idItem);
                });
    }

    private void validarStockDisponible(Long idVariante, Integer cantidad) {
        try {
            StockResponseDTO stock = stockClient.consultarStock(idVariante);
            if (stock.getCantidadDisponible() < cantidad) {
                log.warn("[AUDIT] Stock insuficiente variante {}. Disp: {}, Req: {}",
                        idVariante, stock.getCantidadDisponible(), cantidad);
                throw new StockNoDisponibleException(idVariante, cantidad);
            }
            log.info("[AUDIT] Stock OK variante {}: {}", idVariante, stock.getCantidadDisponible());
        } catch (StockNoDisponibleException e) {
            throw e;
        } catch (feign.FeignException.NotFound e) {
            log.warn("[AUDIT] Variante {} sin registro de stock. Cancelando operacion.", idVariante);
            throw new StockNoDisponibleException(idVariante, cantidad);
        } catch (feign.FeignException e) {
            log.warn("[AUDIT] ms-stock no disponible variante {}: {}", idVariante, e.getMessage());
            throw new RuntimeException("Servicio de stock no disponible. Intenta de nuevo.");
        }
    }

    private BigDecimal obtenerPrecioVariante(Long idVariante) {
        try {
            VarianteResponseDTO variante = catalogoClient.consultarVariante(idVariante);
            if (variante.getPrecio() == null) {
                throw new IllegalStateException("La variante " + idVariante + " no tiene precio en el catalogo.");
            }
            log.info("[AUDIT] Precio variante {}: {}", idVariante, variante.getPrecio());
            return variante.getPrecio();
        } catch (feign.FeignException.NotFound e) {
            log.warn("[AUDIT] Variante {} no encontrada en ms-catalogo", idVariante);
            throw new IllegalStateException(
                    "La variante " + idVariante + " no existe. Crea la variante en ms-catalogo (puerto 8084) primero.");
        } catch (feign.FeignException e) {
            log.error("[AUDIT] Error ms-catalogo variante {}: status={} msg={}", idVariante, e.status(), e.getMessage());
            throw new IllegalStateException(
                    "El catalogo no esta disponible (ms-catalogo:8084). Verifica que este encendido y prueba: "
                    + "GET http://localhost:8084/api/catalogo/variantes/" + idVariante);
        }
    }

    private void agregarOActualizarItem(Carrito carrito, Long idVariante,
                                         Integer cantidad, BigDecimal precio) {
        itemCarritoRepository
                .findByCarrito_IdCarritoAndIdVariante(carrito.getIdCarrito(), idVariante)
                .ifPresentOrElse(
                    existente -> {
                        Integer nuevaCantidad = existente.getCantidad() + cantidad;
                        existente.setCantidad(nuevaCantidad);
                        itemCarritoRepository.save(existente);
                        log.info("[AUDIT] Item existente variante {} cantidad ahora {}", idVariante, nuevaCantidad);
                    },
                    () -> {
                        ItemCarrito nuevo = buildItem(carrito, idVariante, cantidad, precio);
                        itemCarritoRepository.save(nuevo);
                        log.info("[AUDIT] Nuevo item variante {} x{} precio {}", idVariante, cantidad, precio);
                    }
                );
    }

    private ItemCarrito buildItem(Carrito carrito, Long idVariante,
                                   Integer cantidad, BigDecimal precio) {
        ItemCarrito item = new ItemCarrito();
        item.setCarrito(carrito);
        item.setIdVariante(idVariante);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(precio);
        return item;
    }

    private CarritoDTO mapToResponse(Carrito carrito) {
        List<ItemCarritoDTO> itemsDTO = carrito.getItems().stream()
                .map(item -> {
                    BigDecimal subtotal = item.getPrecioUnitario()
                            .multiply(BigDecimal.valueOf(item.getCantidad()));
                    return new ItemCarritoDTO(
                        item.getIdItem(),
                        item.getIdVariante(),
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        subtotal
                    );
                })
                .toList();

        BigDecimal total = itemsDTO.stream()
                .map(ItemCarritoDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarritoDTO(
            carrito.getIdCarrito(),
            carrito.getIdUsuario(),
            carrito.getCreadoEn(),
            itemsDTO,
            total
        );
    }

    // ─────────────────────────────────────────────────────────
    // LIMPIEZA PROGRAMADA de carritos abandonados (Fix #6)
    // ─────────────────────────────────────────────────────────

    /**
     * Ejecuta todas las noches (3:00 AM) la limpieza de carritos
     * abandonados con mas de 7 dias de antigüedad.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void limpiarCarritosAbandonados() {
        LocalDateTime fechaLimite = LocalDateTime.now().minusDays(7);
        log.info("[AUDIT] Iniciando limpieza de carritos anteriores a {}", fechaLimite);

        List<Carrito> antiguos = carritoRepository.findByCreadoEnBefore(fechaLimite);
        if (antiguos.isEmpty()) {
            log.info("[AUDIT] No hay carritos abandonados para limpiar");
            return;
        }

        log.info("[AUDIT] Eliminando {} carritos abandonados", antiguos.size());
        carritoRepository.deleteAll(antiguos);
        carritoRepository.flush();
        log.info("[AUDIT] Limpieza completada: {} carritos eliminados", antiguos.size());
    }
}