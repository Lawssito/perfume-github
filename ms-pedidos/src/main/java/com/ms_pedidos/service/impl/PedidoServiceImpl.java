package com.ms_pedidos.service.impl;

import com.ms_pedidos.client.*;
import com.ms_pedidos.client.dto.*;
import com.ms_pedidos.dto.*;
import com.ms_pedidos.model.DetallePedido;
import com.ms_pedidos.model.EstadoPedido;
import com.ms_pedidos.model.Pedido;
import com.ms_pedidos.exception.PedidoNotFoundException;
import com.ms_pedidos.exception.TransicionEstadoInvalidaException;
import com.ms_pedidos.repository.PedidoRepository;
import com.ms_pedidos.service.PedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository   pedidoRepository;
    private final CarritoClient      carritoClient;
    private final StockClient        stockClient;
    private final PagoClient         pagoClient;
    private final EnvioClient        envioClient;
    private final NotificacionClient notificacionClient;

    // CREAR PEDIDO
    @Override
    @Transactional
    public PedidoDTO crearPedido(CrearPedidoDTO dto) {
        log.info("[SERVICE] Iniciando creacion de pedido para usuario ID: {}", dto.getIdUsuario());

        CarritoDTO carrito = obtenerCarrito(dto.getIdUsuario());
        validarCarritoNoVacio(carrito, dto.getIdUsuario());
        validarMontoTotalPorPagar(carrito);

        // Verificamos stock ANTES de crear el pedido
        // pero NO lo descontamos todavía
        verificarStockItems(carrito.getItems());

        Pedido pedidoGuardado = pedidoRepository.save(
            buildPedido(dto.getIdUsuario(), carrito));
        log.info("[SERVICE] Pedido creado con ID: {} | Total: {} | Estado: {}",
                pedidoGuardado.getIdPedido(), pedidoGuardado.getTotal(), pedidoGuardado.getEstado());

        // Crear y procesar pago — pasamos los items para descontar
        // stock SOLO si el pago queda COMPLETADO
        PagoDTO pagoProcessado = crearYProcesarPago(pedidoGuardado);

        if ("COMPLETADO".equals(pagoProcessado.getEstado())) {
            // Pago exitoso → recién ahora se descuenta stock
            procesarPagoExitoso(pedidoGuardado, dto, carrito.getItems());
        } else {
            procesarPagoRechazado(pedidoGuardado, dto.getIdUsuario());
        }

        vaciarCarritoUsuario(dto.getIdUsuario());

        return mapToResponse(pedidoGuardado);
    }

    // ACTUALIZAR ESTADO
    @Override
    @Transactional
    public PedidoDTO actualizarEstado(Long idPedido, EstadoPedido nuevoEstado) {
        log.info("[SERVICE] Actualizando estado del pedido {} a {}", idPedido, nuevoEstado);

        Pedido pedido       = obtenerPorId(idPedido);
        EstadoPedido actual = pedido.getEstado();

        validarTransicion(actual, nuevoEstado);
        pedido.setEstado(nuevoEstado);

        Pedido actualizado = pedidoRepository.save(pedido);
        log.info("[SERVICE] Pedido {} actualizado: {} → {}", idPedido, actual, nuevoEstado);

        notificarCambioEstado(pedido.getIdUsuario(), idPedido, nuevoEstado);

        return mapToResponse(actualizado);
    }

    // ─────────────────────────────────────────────────────────
    // CONSULTAR POR ID
    // ─────────────────────────────────────────────────────────

    @Override
    public PedidoDTO consultarPorId(Long idPedido) {
        log.info("[SERVICE] Consultando pedido ID: {}", idPedido);

        Pedido pedido = obtenerPorId(idPedido);

        log.info("[SERVICE] Pedido {} encontrado. Estado: {} | Total: {}",
                idPedido, pedido.getEstado(), pedido.getTotal());

        return mapToResponse(pedido);
    }

    // ─────────────────────────────────────────────────────────
    // LISTAR POR USUARIO
    // ─────────────────────────────────────────────────────────

    @Override
    public List<PedidoDTO> listarPorUsuario(Long idUsuario) {
        log.info("[SERVICE] Listando pedidos del usuario ID: {}", idUsuario);

        List<Pedido> pedidos = pedidoRepository.findByIdUsuario(idUsuario);

        log.info("[SERVICE] Usuario {} tiene {} pedidos", idUsuario, pedidos.size());
        return pedidos.stream().map(this::mapToResponse).toList();
    }

    // ─────────────────────────────────────────────────────────
    // LISTAR TODOS
    // ─────────────────────────────────────────────────────────

    @Override
    public List<PedidoDTO> listarTodos() {
        log.info("[SERVICE] Listando todos los pedidos");

        List<Pedido> pedidos = pedidoRepository.findAll();

        log.info("[SERVICE] Total de pedidos: {}", pedidos.size());
        return pedidos.stream().map(this::mapToResponse).toList();
    }

    // ─────────────────────────────────────────────────────────
    // MÉTODOS AUXILIARES PRIVADOS — Feign
    // ─────────────────────────────────────────────────────────

    private CarritoDTO obtenerCarrito(Long idUsuario) {
        try {
            CarritoDTO carrito = carritoClient.obtenerCarrito(idUsuario);
            log.info("[SERVICE] Carrito obtenido para usuario {}. Items: {}",
                    idUsuario, carrito.getItems().size());
            return carrito;

        } catch (feign.FeignException.NotFound e) {
            log.warn("[SERVICE] No existe carrito para usuario {}", idUsuario);
            throw new IllegalStateException(
                "No existe carrito para el usuario " + idUsuario
            );

        } catch (feign.FeignException e) {
            log.warn("[SERVICE] ms-carrito no disponible: {}", e.getMessage());
            throw new IllegalStateException(
                "El servicio de carrito no esta disponible. Intenta nuevamente."
            );
        }
    }

    private void verificarStockItems(List<ItemCarritoDTO> items) {
        log.info("[SERVICE] Verificando stock para {} items", items.size());

        for (ItemCarritoDTO item : items) {
            try {
                StockDTO stock = stockClient.consultarStock(item.getIdVariante());
                if (stock.getCantidadDisponible() < item.getCantidad()) {
                    log.warn("[SERVICE] Stock insuficiente para variante {}. Disponible: {}, Requerido: {}",
                            item.getIdVariante(),
                            stock.getCantidadDisponible(),
                            item.getCantidad());
                    throw new IllegalStateException(
                        "Stock insuficiente para variante " + item.getIdVariante()
                        + ". Disponible: " + stock.getCantidadDisponible()
                        + ", requerido: " + item.getCantidad()
                    );
                }
                log.info("[SERVICE] Stock OK para variante {}", item.getIdVariante());

            } catch (IllegalStateException e) {
                throw e;

            } catch (feign.FeignException.NotFound e) {
                log.warn("[SERVICE] Variante {} sin registro en ms-stock. Continuando.",
                        item.getIdVariante());

            } catch (feign.FeignException e) {
                log.warn("[SERVICE] ms-stock no disponible para variante {}. Continuando.",
                        item.getIdVariante());
            }
        }
    }

    private void reducirStockItems(List<ItemCarritoDTO> items) {
        for (ItemCarritoDTO item : items) {
            try {
                stockClient.reducirStock(
                    item.getIdVariante(),
                    new ReducirStockClientDTO(item.getCantidad())
                );
                log.info("[SERVICE] Stock reducido para variante {}. Cantidad: {}",
                        item.getIdVariante(), item.getCantidad());

            } catch (feign.FeignException e) {
                log.warn("[SERVICE] No se pudo reducir stock para variante {}: {}",
                        item.getIdVariante(), e.getMessage());
            }
        }
    }

    private PagoDTO crearYProcesarPago(Pedido pedido) {
        try {
            log.info("[SERVICE] Creando pago para pedido {}", pedido.getIdPedido());

            PagoDTO pago = pagoClient.crearPago(
                new CrearPagoClientDTO(pedido.getIdPedido(), pedido.getTotal(), "TARJETA")
            );
            log.info("[SERVICE] Pago creado con ID: {}. Procesando...", pago.getIdTransaccion());

            PagoDTO procesado = pagoClient.procesarPago(pago.getIdTransaccion());
            log.info("[SERVICE] Resultado del pago: {}", procesado.getEstado());
            return procesado;

        } catch (feign.FeignException e) {
            log.error("[SERVICE] ms-pagos no disponible. Cancelando pedido {}: status={} {}",
                    pedido.getIdPedido(), e.status(), e.getMessage());

            pedido.setEstado(EstadoPedido.CANCELADO);
            pedidoRepository.save(pedido);

            notificar(pedido.getIdUsuario(),
                "No se pudo procesar tu pago. El pedido #"
                + pedido.getIdPedido() + " fue cancelado.");

            throw new IllegalStateException(
                "El servicio de pagos no esta disponible (ms-pagos:8087). "
                + "Enciende ms-pagos y verifica: POST http://localhost:8087/api/pagos"
            );
        }
    }

    private void procesarPagoExitoso(Pedido pedido, CrearPedidoDTO dto, List<ItemCarritoDTO> items) {
        // 1. Descontar stock — recién aquí, después de confirmar el pago
        reducirStockItems(items);

        // 2. Cambiar estado del pedido
        pedido.setEstado(EstadoPedido.PAGADO);
        pedidoRepository.save(pedido);
        log.info("[SERVICE] Pedido {} actualizado a PAGADO", pedido.getIdPedido());

        // 3. Crear envío
        crearEnvio(pedido.getIdPedido(), dto.getDireccionEntrega(), dto.getCourier());

        // 4. Notificar
        notificar(dto.getIdUsuario(), "Tu pedido #" + pedido.getIdPedido() + " fue confirmado y esta siendo procesado.");
    }

    // ── PAGO RECHAZADO — sin tocar stock ─────────────────────────
    private void procesarPagoRechazado(Pedido pedido, Long idUsuario) {
        // El stock NO se toca — nunca se descontó
        pedido.setEstado(EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);
        log.warn("[SERVICE] Pago rechazado. Pedido {} cancelado. Stock intacto.",
                pedido.getIdPedido());

        notificar(idUsuario,
            "Tu pago fue rechazado. El pedido #"
            + pedido.getIdPedido() + " fue cancelado.");
    }

    private void crearEnvio(Long idPedido, String direccion, String courier) {
        try {
            EnvioDTO envio = envioClient.crearEnvio(
                new CrearEnvioClientDTO(idPedido, direccion, courier)
            );
            log.info("[SERVICE] Envio creado para pedido {}. Tracking: {}",
                    idPedido, envio.getNumeroTracking());

        } catch (feign.FeignException e) {
            log.warn("[SERVICE] No se pudo crear envio para pedido {}: {}. " +
                    "El pago fue exitoso — el envio debe gestionarse manualmente.",
                    idPedido, e.getMessage());
        }
    }

    private void vaciarCarritoUsuario(Long idUsuario) {
        try {
            carritoClient.vaciarCarrito(idUsuario);
            log.info("[SERVICE] Carrito del usuario {} vaciado", idUsuario);

        } catch (feign.FeignException e) {
            log.warn("[SERVICE] No se pudo vaciar carrito del usuario {}: {}",
                    idUsuario, e.getMessage());
        }
    }

    private void notificar(Long idUsuario, String mensaje) {
        try {
            notificacionClient.enviarNotificacion(
                new CrearNotificacionDTO(idUsuario, mensaje)
            );
            log.info("[SERVICE] Notificacion enviada al usuario {}: {}", idUsuario, mensaje);

        } catch (feign.FeignException e) {
            // Si ms-notificaciones está caído, solo se loguea
            // El pedido ya fue creado y pagado — no se revierte nada
            log.warn("[SERVICE] No se pudo notificar al usuario {}: {}", idUsuario, e.getMessage());
        }
    }

    // MÉTODOS AUXILIARES PRIVADOS — dominio
    private Pedido obtenerPorId(Long idPedido) {
        return pedidoRepository.findById(idPedido)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Pedido ID {} no encontrado", idPedido);
                    return new PedidoNotFoundException(idPedido);
                });
    }

    private void validarCarritoNoVacio(CarritoDTO carrito, Long idUsuario) {
        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            log.warn("[SERVICE] Carrito del usuario {} esta vacio", idUsuario);
            throw new IllegalStateException(
                "El carrito esta vacio. Agrega productos antes de confirmar."
            );
        }
    }

    private void validarMontoTotalPorPagar(CarritoDTO carrito) {
        if (carrito.getTotal() == null || carrito.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[SERVICE] Carrito sin monto por pagar. idCarrito={} total={}",
                    carrito.getIdCarrito(), carrito.getTotal());
            throw new IllegalStateException(
                "No se puede crear el pedido porque no hay nada que pagar. " +
                "Agrega productos con precio mayor a 0 antes de confirmar."
            );
        }
    }

    private void validarTransicion(EstadoPedido actual, EstadoPedido solicitado) {
        boolean valida = switch (actual) {
            case CREADO         -> solicitado == EstadoPedido.PAGADO
                                || solicitado == EstadoPedido.CANCELADO;
            case PAGADO         -> solicitado == EstadoPedido.EN_PREPARACION
                                || solicitado == EstadoPedido.CANCELADO;
            case EN_PREPARACION -> solicitado == EstadoPedido.ENVIADO;
            case ENVIADO        -> solicitado == EstadoPedido.ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };

        if (!valida) {
            log.warn("[SERVICE] Transicion invalida: {} → {}", actual, solicitado);
            throw new TransicionEstadoInvalidaException(actual, solicitado);
        }
    }

    private void notificarCambioEstado(Long idUsuario, Long idPedido, EstadoPedido estado) {
        String mensaje = switch (estado) {
            case EN_PREPARACION -> "Tu pedido #" + idPedido + " esta siendo preparado.";
            case ENVIADO        -> "Tu pedido #" + idPedido + " ha sido despachado.";
            case ENTREGADO      -> "Tu pedido #" + idPedido + " fue entregado. Gracias por tu compra.";
            case CANCELADO      -> "Tu pedido #" + idPedido + " ha sido cancelado.";
            default             -> null;
        };

        if (mensaje != null) {
            notificar(idUsuario, mensaje);
        }
    }

    private Pedido buildPedido(Long idUsuario, CarritoDTO carrito) {
        Pedido pedido = new Pedido();
        pedido.setIdUsuario(idUsuario);
        pedido.setTotal(carrito.getTotal());

        List<DetallePedido> detalles = carrito.getItems().stream()
                .map(item -> buildDetalle(pedido, item))
                .toList();

        pedido.setDetalles(detalles);
        return pedido;
    }

    private DetallePedido buildDetalle(Pedido pedido, ItemCarritoDTO item) {
        DetallePedido detalle = new DetallePedido();
        detalle.setPedido(pedido);
        detalle.setIdVariante(item.getIdVariante());
        detalle.setNombreProducto("Variante " + item.getIdVariante());
        detalle.setMl(0);
        detalle.setCantidad(item.getCantidad());
        detalle.setPrecioUnitario(item.getPrecioUnitario());
        return detalle;
    }

    private PedidoDTO mapToResponse(Pedido pedido) {
        List<DetallePedidoDTO> detallesDTO = pedido.getDetalles().stream()
                .map(d -> {
                    BigDecimal subtotal = d.getPrecioUnitario()
                            .multiply(BigDecimal.valueOf(d.getCantidad()));
                    return new DetallePedidoDTO(
                        d.getIdDetalle(),
                        d.getIdVariante(),
                        d.getNombreProducto(),
                        d.getMl(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        subtotal
                    );
                })
                .toList();

        return new PedidoDTO(
            pedido.getIdPedido(),
            pedido.getIdUsuario(),
            pedido.getEstado(),
            pedido.getTotal(),
            pedido.getFechaCreacion(),
            detallesDTO
        );
    }
}