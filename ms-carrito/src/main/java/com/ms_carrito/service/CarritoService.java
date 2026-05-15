package com.ms_carrito.service;

import com.ms_carrito.client.StockClient;
import com.ms_carrito.client.StockResponseDTO;
import com.ms_carrito.dto.*;
import com.ms_carrito.model.Carrito;
import com.ms_carrito.model.ItemCarrito;
import com.ms_carrito.exception.*;
import com.ms_carrito.repository.CarritoRepository;
import com.ms_carrito.repository.ItemCarritoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private final StockClient stockClient;

    // HELPER PRIVADO — verifica stock consultando ms-stock

    /**
     * Separarlo en un método privado evita repetir el mismo bloque
     * try/catch en agregarItem y actualizarCantidad.
     */
    private boolean verificarStock(Long idVariante, Integer cantidad) {
        log.info("[SERVICE] Consultando stock en ms-stock para variante {} (cantidad: {})",
                idVariante, cantidad);
        try {
            StockResponseDTO respuesta = stockClient.consultarStock(idVariante);

            boolean disponible = respuesta.getCantidadDisponible() >= cantidad;

            log.info("[SERVICE] ms-stock respondio: disponible={}, solicitado={}, resultado={}",
                    respuesta.getCantidadDisponible(), cantidad, disponible);

            return disponible;

        } catch (Exception e) {
            // ms-stock caído, timeout o variante no registrada aún en stock
            log.warn("[SERVICE] No se pudo contactar ms-stock para variante {}: {}. Asumiendo disponible.",
                    idVariante, e.getMessage());
            return true;
        }
    }

    // OBTENER O CREAR CARRITO

    @Transactional
    public CarritoDTO obtenerOCrearCarrito(Long idUsuario) {
        log.info("[SERVICE] Obteniendo carrito para usuario ID: {}", idUsuario);

        try {
            Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                    .orElseGet(() -> {
                        log.info("[SERVICE] No existe carrito para usuario {}. Creando nuevo.", idUsuario);
                        Carrito nuevo = new Carrito();
                        nuevo.setIdUsuario(idUsuario);
                        return carritoRepository.save(nuevo);
                    });

            log.info("[SERVICE] Carrito ID {} retornado para usuario {}",
                    carrito.getIdCarrito(), idUsuario);

            return mapToDTO(carrito);

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al obtener carrito del usuario {}: {}",
                    idUsuario, e.getMessage(), e);
            throw e;
        }
    }

    // AGREGAR ITEM

    @Transactional
    public CarritoDTO agregarItem(Long idUsuario, AgregarItemDTO dto) {
        log.info("[SERVICE] Agregando variante {} (x{}) al carrito del usuario {}",
                dto.getIdVariante(), dto.getCantidad(), idUsuario);

        try {
            // 1. Verificar stock en ms-stock
            boolean hayStock = verificarStock(dto.getIdVariante(), dto.getCantidad());
            if (!hayStock) {
                log.warn("[SERVICE] Stock insuficiente para variante {} al intentar agregar al carrito",
                        dto.getIdVariante());
                throw new StockNoDisponibleException(dto.getIdVariante(), dto.getCantidad());
            }

            // 2. Obtener o crear carrito
            Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                    .orElseGet(() -> {
                        Carrito nuevo = new Carrito();
                        nuevo.setIdUsuario(idUsuario);
                        return carritoRepository.save(nuevo);
                    });

            // 3. Si la variante ya está en el carrito → sumar cantidad
            // Si no está → crear item nuevo
            itemCarritoRepository
                    .findByCarrito_IdCarritoAndIdVariante(carrito.getIdCarrito(), dto.getIdVariante())
                    .ifPresentOrElse(
                            itemExistente -> {
                                Integer nuevaCantidad = itemExistente.getCantidad() + dto.getCantidad();
                                itemExistente.setCantidad(nuevaCantidad);
                                itemCarritoRepository.save(itemExistente);
                                log.info("[SERVICE] Variante {} ya existia en carrito. Nueva cantidad: {}",
                                        dto.getIdVariante(), nuevaCantidad);
                            },
                            () -> {
                                ItemCarrito nuevoItem = new ItemCarrito();
                                nuevoItem.setCarrito(carrito);
                                nuevoItem.setIdVariante(dto.getIdVariante());
                                nuevoItem.setCantidad(dto.getCantidad());
                                nuevoItem.setPrecioUnitario(BigDecimal.ZERO);
                                itemCarritoRepository.save(nuevoItem);
                                log.info("[SERVICE] Nuevo item creado. Variante {}, cantidad: {}",
                                        dto.getIdVariante(), dto.getCantidad());
                            });

            // 4. Recargar y retornar el carrito actualizado
            Carrito actualizado = carritoRepository.findByIdUsuario(idUsuario)
                    .orElseThrow(() -> new CarritoNotFoundException(idUsuario));

            log.info("[SERVICE] Item agregado exitosamente al carrito del usuario {}", idUsuario);
            return mapToDTO(actualizado);

        } catch (StockNoDisponibleException | CarritoNotFoundException e) {
            // Excepciones de negocio esperadas — ya logueadas, solo se relanzan
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al agregar item al carrito del usuario {}: {}",
                    idUsuario, e.getMessage(), e);
            throw e;
        }
    }

    // ACTUALIZAR CANTIDAD

    @Transactional
    public CarritoDTO actualizarCantidad(Long idUsuario, Long idItem, ActualizarCantidadDTO dto) {
        log.info("[SERVICE] Actualizando item {} a cantidad {} para usuario {}",
                idItem, dto.getCantidad(), idUsuario);

        try {
            // 1. Verificar que el carrito existe
            Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                    .orElseThrow(() -> new CarritoNotFoundException(idUsuario));

            // 2. Verificar que el item existe y pertenece a este carrito
            ItemCarrito item = itemCarritoRepository.findById(idItem)
                    .filter(i -> i.getCarrito().getIdCarrito().equals(carrito.getIdCarrito()))
                    .orElseThrow(() -> new ItemNotFoundException(idItem));

            // 3. Verificar stock para la nueva cantidad
            boolean hayStock = verificarStock(item.getIdVariante(), dto.getCantidad());
            if (!hayStock) {
                log.warn("[SERVICE] Stock insuficiente para variante {} al actualizar cantidad",
                        item.getIdVariante());
                throw new StockNoDisponibleException(item.getIdVariante(), dto.getCantidad());
            }

            // 4. Actualizar
            Integer cantidadAnterior = item.getCantidad();
            item.setCantidad(dto.getCantidad());
            itemCarritoRepository.save(item);

            log.info("[SERVICE] Item {} actualizado. Anterior: {}, Nueva: {}",
                    idItem, cantidadAnterior, dto.getCantidad());

            Carrito actualizado = carritoRepository.findByIdUsuario(idUsuario)
                    .orElseThrow(() -> new CarritoNotFoundException(idUsuario));

            return mapToDTO(actualizado);

        } catch (CarritoNotFoundException | ItemNotFoundException | StockNoDisponibleException e) {
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al actualizar item {} del usuario {}: {}",
                    idItem, idUsuario, e.getMessage(), e);
            throw e;
        }
    }

    // ELIMINAR ITEM

    @Transactional
    public CarritoDTO eliminarItem(Long idUsuario, Long idItem) {
        log.info("[SERVICE] Eliminando item {} del carrito del usuario {}", idItem, idUsuario);

        try {
            // 1. Verificar que el carrito existe
            Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                    .orElseThrow(() -> new CarritoNotFoundException(idUsuario));

            // 2. Verificar que el item existe
            ItemCarrito item = itemCarritoRepository.findById(idItem)
                    .orElseThrow(() -> new ItemNotFoundException(idItem));

            // 3. Verificar que el item pertenece al carrito de este usuario
            if (!item.getCarrito().getIdCarrito().equals(carrito.getIdCarrito())) {
                log.warn("[SERVICE] Item {} no pertenece al carrito del usuario {}", idItem, idUsuario);
                throw new ItemNotFoundException(idItem);
            }

            // 4. Eliminar directo por repositorio + flush para que llegue a MySQL
            // antes de la recarga
            itemCarritoRepository.deleteById(idItem);
            itemCarritoRepository.flush();

            log.info("[SERVICE] Item {} eliminado exitosamente del carrito del usuario {}",
                    idItem, idUsuario);

            // 5. Recargar desde BD con los items actualizados
            Carrito actualizado = carritoRepository.findByIdUsuario(idUsuario)
                    .orElseThrow(() -> new CarritoNotFoundException(idUsuario));

            return mapToDTO(actualizado);

        } catch (CarritoNotFoundException | ItemNotFoundException e) {
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al eliminar item {} del usuario {}: {}",
                    idItem, idUsuario, e.getMessage(), e);
            throw e;
        }
    }

    // VACIAR CARRITO

    @Transactional
    public void vaciarCarrito(Long idUsuario) {
        log.info("[SERVICE] Vaciando carrito del usuario {}", idUsuario);

        try {
            Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                    .orElseThrow(() -> new CarritoNotFoundException(idUsuario));

            carrito.getItems().clear();
            carritoRepository.save(carrito);

            log.info("[SERVICE] Carrito {} vaciado exitosamente", carrito.getIdCarrito());

        } catch (CarritoNotFoundException e) {
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al vaciar carrito del usuario {}: {}",
                    idUsuario, e.getMessage(), e);
            throw e;
        }
    }

    // MAPPER

    private CarritoDTO mapToDTO(Carrito carrito) {
        try {
            List<ItemCarritoDTO> itemsDTO = carrito.getItems().stream()
                    .map(item -> {
                        BigDecimal subtotal = item.getPrecioUnitario()
                                .multiply(BigDecimal.valueOf(item.getCantidad()));
                        return new ItemCarritoDTO(
                                item.getIdItem(),
                                item.getIdVariante(),
                                item.getCantidad(),
                                item.getPrecioUnitario(),
                                subtotal);
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
                    total);

        } catch (Exception e) {
            log.error("[SERVICE] Error al convertir carrito {} a DTO: {}",
                    carrito.getIdCarrito(), e.getMessage(), e);
            throw e;
        }
    }
}