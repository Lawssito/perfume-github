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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarritoServiceImpl implements CarritoService {

    private final CarritoRepository     carritoRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private final StockClient           stockClient;
    private final CatalogoClient        catalogoClient;

    // OBTENER O CREAR
    @Override
    @Transactional
    public CarritoDTO obtenerOCrearCarrito(Long idUsuario) {
        log.info("[SERVICE] Obteniendo carrito para usuario ID: {}", idUsuario);

        Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                .orElseGet(() -> crearNuevoCarrito(idUsuario));

        log.info("[SERVICE] Carrito ID {} retornado para usuario {}",
                carrito.getIdCarrito(), idUsuario);

        return mapToResponse(carrito);
    }

    // AGREGAR ITEM
    @Override
    @Transactional
    public CarritoDTO agregarItem(Long idUsuario, AgregarItemDTO dto) {
        log.info("[SERVICE] Agregando variante {} (x{}) al carrito del usuario {}",
                dto.getIdVariante(), dto.getCantidad(), idUsuario);

        validarStockDisponible(dto.getIdVariante(), dto.getCantidad());
        BigDecimal precio = obtenerPrecioVariante(dto.getIdVariante());

        Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                .orElseGet(() -> crearNuevoCarrito(idUsuario));

        agregarOActualizarItem(carrito, dto.getIdVariante(), dto.getCantidad(), precio);

        Carrito actualizado = obtenerCarritoPorUsuario(idUsuario);

        log.info("[SERVICE] Item agregado exitosamente al carrito del usuario {}", idUsuario);
        return mapToResponse(actualizado);
    }

    // ACTUALIZAR CANTIDAD
    @Override
    @Transactional
    public CarritoDTO actualizarCantidad(Long idUsuario, Long idItem, ActualizarCantidadDTO dto) {
        log.info("[SERVICE] Actualizando item {} a cantidad {} para usuario {}",
                idItem, dto.getCantidad(), idUsuario);

        Carrito carrito = obtenerCarritoPorUsuario(idUsuario);
        ItemCarrito item = obtenerItemDelCarrito(idItem, carrito.getIdCarrito());

        validarStockDisponible(item.getIdVariante(), dto.getCantidad());

        Integer cantidadAnterior = item.getCantidad();
        item.setCantidad(dto.getCantidad());
        itemCarritoRepository.save(item);

        log.info("[SERVICE] Item {} actualizado. Anterior: {}, Nueva: {}",
                idItem, cantidadAnterior, dto.getCantidad());

        return mapToResponse(obtenerCarritoPorUsuario(idUsuario));
    }

    // ELIMINAR ITEM
    @Override
    @Transactional
    public CarritoDTO eliminarItem(Long idUsuario, Long idItem) {
        log.info("[SERVICE] Eliminando item {} del carrito del usuario {}", idItem, idUsuario);

        Carrito carrito = obtenerCarritoPorUsuario(idUsuario);
        ItemCarrito item = obtenerItemDelCarrito(idItem, carrito.getIdCarrito());

        itemCarritoRepository.deleteById(item.getIdItem());
        itemCarritoRepository.flush();

        log.info("[SERVICE] Item {} eliminado exitosamente del carrito del usuario {}",
                idItem, idUsuario);

        return mapToResponse(obtenerCarritoPorUsuario(idUsuario));
    }


    // VACIAR CARRITO
    @Override
    @Transactional
    public void vaciarCarrito(Long idUsuario) {
        log.info("[SERVICE] Vaciando carrito del usuario {}", idUsuario);

        Carrito carrito = obtenerCarritoPorUsuario(idUsuario);
        carrito.getItems().clear();
        carritoRepository.save(carrito);

        log.info("[SERVICE] Carrito {} vaciado exitosamente", carrito.getIdCarrito());
    }

    // MÉTODOS AUXILIARES PRIVADOS
    private Carrito crearNuevoCarrito(Long idUsuario) {
        log.info("[SERVICE] Creando nuevo carrito para usuario {}", idUsuario);
        Carrito nuevo = new Carrito();
        nuevo.setIdUsuario(idUsuario);
        return carritoRepository.save(nuevo);
    }

    private Carrito obtenerCarritoPorUsuario(Long idUsuario) {
        return carritoRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Carrito no encontrado para usuario {}", idUsuario);
                    return new CarritoNotFoundException(idUsuario);
                });
    }

    private ItemCarrito obtenerItemDelCarrito(Long idItem, Long idCarrito) {
        return itemCarritoRepository.findById(idItem)
                .filter(i -> i.getCarrito().getIdCarrito().equals(idCarrito))
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Item {} no encontrado en carrito {}", idItem, idCarrito);
                    return new ItemNotFoundException(idItem);
                });
    }

    private void validarStockDisponible(Long idVariante, Integer cantidad) {
        try {
            StockResponseDTO stock = stockClient.consultarStock(idVariante);
            if (stock.getCantidadDisponible() < cantidad) {
                log.warn("[SERVICE] Stock insuficiente para variante {}. Disponible: {}, Solicitado: {}",
                        idVariante, stock.getCantidadDisponible(), cantidad);
                throw new StockNoDisponibleException(idVariante, cantidad);
            }
            log.info("[SERVICE] Stock OK para variante {}. Disponible: {}", idVariante, stock.getCantidadDisponible());

        } catch (StockNoDisponibleException e) {
            throw e;

        } catch (feign.FeignException.NotFound e) {
            log.warn("[SERVICE] Variante {} sin registro en ms-stock. Asumiendo disponible.", idVariante);

        } catch (feign.FeignException e) {
            log.warn("[SERVICE] ms-stock no disponible para variante {}: {}. Asumiendo disponible.",
                    idVariante, e.getMessage());
        }
    }

    private BigDecimal obtenerPrecioVariante(Long idVariante) {
        try {
            VarianteResponseDTO variante = catalogoClient.consultarVariante(idVariante);
            if (variante.getPrecio() == null) {
                throw new IllegalStateException("La variante " + idVariante + " no tiene precio en el catalogo.");
            }
            log.info("[SERVICE] Precio obtenido para variante {}: {}", idVariante, variante.getPrecio());
            return variante.getPrecio();

        } catch (feign.FeignException.NotFound e) {
            log.warn("[SERVICE] Variante {} no encontrada en ms-catalogo (404)", idVariante);
            throw new IllegalStateException(
                    "La variante " + idVariante + " no existe. Crea la variante en ms-catalogo (puerto 8084) primero.");

        } catch (feign.FeignException e) {
            log.error("[SERVICE] Error Feign ms-catalogo variante {}: status={} msg={}",
                    idVariante, e.status(), e.getMessage());
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
                        existente.setPrecioUnitario(precio);
                        itemCarritoRepository.save(existente);
                        log.info("[SERVICE] Item existente actualizado. Variante {}, cantidad: {}",
                                idVariante, nuevaCantidad);
                    },
                    () -> {
                        ItemCarrito nuevo = buildItem(carrito, idVariante, cantidad, precio);
                        itemCarritoRepository.save(nuevo);
                        log.info("[SERVICE] Nuevo item creado. Variante {}, cantidad: {}, precio: {}",
                                idVariante, cantidad, precio);
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
}