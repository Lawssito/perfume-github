package com.ms_carrito.service;

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

    private final CarritoRepository     carritoRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private final StockClient           stockClient;
    private final CatalogoClient        catalogoClient;

    // ─────────────────────────────────────────────────────────
    // HELPER — verificar stock con Feign
    // ─────────────────────────────────────────────────────────

    private boolean verificarStock(Long idVariante, Integer cantidad) {
        log.info("[SERVICE] Verificando stock para variante {} (cantidad: {})",
                idVariante, cantidad);

        try {
            StockResponseDTO respuesta = stockClient.consultarStock(idVariante);
            boolean disponible = respuesta.getCantidadDisponible() >= cantidad;

            log.info("[SERVICE] Stock variante {}: disponible={}, solicitado={}, resultado={}",
                    idVariante, respuesta.getCantidadDisponible(), cantidad, disponible);

            return disponible;

        } catch (feign.FeignException.NotFound e) {
            // La variante no tiene registro en ms-stock todavía
            log.warn("[SERVICE] Variante {} no registrada en ms-stock. Asumiendo disponible.", idVariante);
            return true;

        } catch (feign.FeignException e) {
            // ms-stock caído o error de red
            log.warn("[SERVICE] ms-stock no disponible para variante {}: {}. Asumiendo disponible.",
                    idVariante, e.getMessage());
            return true;
        }
    }

     /**
     * Consulta el precio de una variante en ms-catalogo.
     *
     * FeignException.NotFound → la variante no existe en catálogo
     *   lanza IllegalStateException — no tiene sentido agregar
     *   al carrito un producto que no existe en el catálogo.
     *
     * FeignException → ms-catalogo caído
     *   lanza IllegalStateException — no podemos agregar sin precio,
     *   el carrito quedaría con precio 0 lo cual es un error de negocio.
     *   A diferencia del stock, el precio es obligatorio.
     */
    private BigDecimal obtenerPrecioVariante(Long idVariante) {
        log.info("[SERVICE] Consultando precio de variante {} en ms-catalogo", idVariante);

        try {
            VarianteResponseDTO variante = catalogoClient.consultarVariante(idVariante);

            log.info("[SERVICE] Precio obtenido para variante {}: {}",
                    idVariante, variante.getPrecio());

            return variante.getPrecio();

        } catch (feign.FeignException.NotFound e) {
            log.warn("[SERVICE] Variante {} no encontrada en ms-catalogo", idVariante);
            throw new IllegalStateException(
                "La variante " + idVariante + " no existe en el catalogo."
            );

        } catch (feign.FeignException e) {
            log.warn("[SERVICE] ms-catalogo no disponible al consultar variante {}: {}",
                    idVariante, e.getMessage());
            throw new IllegalStateException(
                "El catalogo no esta disponible. No se puede agregar el producto al carrito."
            );
        }
    }


    // OBTENER O CREAR CARRITO
    @Transactional
    public CarritoDTO obtenerOCrearCarrito(Long idUsuario) {
        log.info("[SERVICE] Obteniendo carrito para usuario ID: {}", idUsuario);

        Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                .orElseGet(() -> {
                    log.info("[SERVICE] No existe carrito para usuario {}. Creando nuevo.", idUsuario);
                    Carrito nuevo = new Carrito();
                    nuevo.setIdUsuario(idUsuario);
                    return carritoRepository.save(nuevo);
                });

        log.info("[SERVICE] Carrito ID {} retornado para usuario {}", carrito.getIdCarrito(), idUsuario);

        return mapToDTO(carrito);
    }

    // AGREGAR ITEM
    @Transactional
    public CarritoDTO agregarItem(Long idUsuario, AgregarItemDTO dto) {
        log.info("[SERVICE] Agregando variante {} (x{}) al carrito del usuario {}",
            dto.getIdVariante(), dto.getCantidad(), idUsuario);

        // ── PASO 1: Verificar stock ──────────────────────────────
        if (!verificarStock(dto.getIdVariante(), dto.getCantidad())) {
            log.warn("[SERVICE] Stock insuficiente para variante {}", dto.getIdVariante());
            throw new StockNoDisponibleException(dto.getIdVariante(), dto.getCantidad());
        }

        // ── PASO 2: Obtener precio real desde ms-catalogo ────────
        BigDecimal precioUnitario = obtenerPrecioVariante(dto.getIdVariante());

        // ── PASO 3: Obtener o crear carrito ──────────────────────
        Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                .orElseGet(() -> {
                    Carrito nuevo = new Carrito();
                    nuevo.setIdUsuario(idUsuario);
                    return carritoRepository.save(nuevo);
                });

        // ── PASO 4: Si existe → sumar cantidad. Si no → crear ────
        itemCarritoRepository
                .findByCarrito_IdCarritoAndIdVariante(carrito.getIdCarrito(), dto.getIdVariante())
                .ifPresentOrElse(
                    itemExistente -> {
                        Integer nuevaCantidad = itemExistente.getCantidad() + dto.getCantidad();
                        itemExistente.setCantidad(nuevaCantidad);
                        // Actualizamos también el precio por si cambió desde
                        // la última vez que se agregó este producto
                        itemExistente.setPrecioUnitario(precioUnitario);
                        itemCarritoRepository.save(itemExistente);
                        log.info("[SERVICE] Variante {} ya existia. Nueva cantidad: {} | Precio: {}",
                                dto.getIdVariante(), nuevaCantidad, precioUnitario);
                    },
                    () -> {
                        ItemCarrito nuevoItem = new ItemCarrito();
                        nuevoItem.setCarrito(carrito);
                        nuevoItem.setIdVariante(dto.getIdVariante());
                        nuevoItem.setCantidad(dto.getCantidad());
                        nuevoItem.setPrecioUnitario(precioUnitario);
                        itemCarritoRepository.save(nuevoItem);
                        log.info("[SERVICE] Nuevo item creado. Variante {}, cantidad: {}, precio: {}",
                                dto.getIdVariante(), dto.getCantidad(), precioUnitario);
                    }
                );

        Carrito actualizado = carritoRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new CarritoNotFoundException(idUsuario));

        log.info("[SERVICE] Item agregado exitosamente al carrito del usuario {}", idUsuario);
        return mapToDTO(actualizado);
    }

    // ACTUALIZAR CANTIDAD
    @Transactional
    public CarritoDTO actualizarCantidad(Long idUsuario, Long idItem, ActualizarCantidadDTO dto) {
        log.info("[SERVICE] Actualizando item {} a cantidad {} para usuario {}",
                idItem, dto.getCantidad(), idUsuario);

        Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Carrito no encontrado para usuario {}", idUsuario);
                    return new CarritoNotFoundException(idUsuario);
                });

        ItemCarrito item = itemCarritoRepository.findById(idItem)
                .filter(i -> i.getCarrito().getIdCarrito().equals(carrito.getIdCarrito()))
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Item {} no encontrado en carrito del usuario {}",
                            idItem, idUsuario);
                    return new ItemNotFoundException(idItem);
                });

        if (!verificarStock(item.getIdVariante(), dto.getCantidad())) {
            log.warn("[SERVICE] Stock insuficiente para variante {} al actualizar cantidad", item.getIdVariante());
            throw new StockNoDisponibleException(item.getIdVariante(), dto.getCantidad());
        }

        Integer cantidadAnterior = item.getCantidad();
        item.setCantidad(dto.getCantidad());
        itemCarritoRepository.save(item);

        log.info("[SERVICE] Item {} actualizado. Anterior: {}, Nueva: {}",
                idItem, cantidadAnterior, dto.getCantidad());

        Carrito actualizado = carritoRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new CarritoNotFoundException(idUsuario));

        return mapToDTO(actualizado);
    }

    // ELIMINAR ITEM
    @Transactional
    public CarritoDTO eliminarItem(Long idUsuario, Long idItem) {
        log.info("[SERVICE] Eliminando item {} del carrito del usuario {}", idItem, idUsuario);

        Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Carrito no encontrado para usuario {}", idUsuario);
                    return new CarritoNotFoundException(idUsuario);
                });

        ItemCarrito item = itemCarritoRepository.findById(idItem)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Item {} no encontrado", idItem);
                    return new ItemNotFoundException(idItem);
                });

        if (!item.getCarrito().getIdCarrito().equals(carrito.getIdCarrito())) {
            log.warn("[SERVICE] Item {} no pertenece al carrito del usuario {}", idItem, idUsuario);
            throw new ItemNotFoundException(idItem);
        }

        itemCarritoRepository.deleteById(idItem);
        itemCarritoRepository.flush();

        log.info("[SERVICE] Item {} eliminado exitosamente del carrito del usuario {}",
                idItem, idUsuario);

        Carrito actualizado = carritoRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new CarritoNotFoundException(idUsuario));

        return mapToDTO(actualizado);
    }

    // VACIAR CARRITO
    @Transactional
    public void vaciarCarrito(Long idUsuario) {
        log.info("[SERVICE] Vaciando carrito del usuario {}", idUsuario);

        Carrito carrito = carritoRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Carrito no encontrado para usuario {}", idUsuario);
                    return new CarritoNotFoundException(idUsuario);
                });

        carrito.getItems().clear();
        carritoRepository.save(carrito);

        log.info("[SERVICE] Carrito {} vaciado exitosamente", carrito.getIdCarrito());
    }

    // MAPPER
    private CarritoDTO mapToDTO(Carrito carrito) {
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