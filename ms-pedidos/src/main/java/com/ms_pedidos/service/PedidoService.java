package com.ms_pedidos.service;

import com.ms_pedidos.client.*;
import com.ms_pedidos.client.dto.*;
import com.ms_pedidos.dto.*;
import com.ms_pedidos.model.DetallePedido;
import com.ms_pedidos.model.EstadoPedido;
import com.ms_pedidos.model.Pedido;
import com.ms_pedidos.exception.PedidoNotFoundException;
import com.ms_pedidos.exception.TransicionEstadoInvalidaException;
import com.ms_pedidos.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository   pedidoRepository;
    private final CarritoClient      carritoClient;
    private final StockClient        stockClient;
    private final PagoClient         pagoClient;
    private final EnvioClient        envioClient;
    private final NotificacionClient notificacionClient;

    // ─────────────────────────────────────────────────────────
    // HELPER — validar transiciones de estado
    // ─────────────────────────────────────────────────────────

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
            throw new TransicionEstadoInvalidaException(actual, solicitado);
        }
    }

    // ─────────────────────────────────────────────────────────
    // HELPER — notificar sin romper el flujo principal
    // ─────────────────────────────────────────────────────────

    /**
     * Las notificaciones son informativas.
     * Si ms-notificaciones está caído, el pedido sigue adelante.
     * FeignException se captura aquí para no propagar el error.
     */
    private void notificar(Long idUsuario, String mensaje) {
        try {
            notificacionClient.enviarNotificacion(
                new CrearNotificacionDTO(idUsuario, mensaje)
            );
            log.info("[SERVICE] Notificacion enviada al usuario {}: {}", idUsuario, mensaje);

        } catch (feign.FeignException e) {
            log.warn("[SERVICE] No se pudo notificar al usuario {}. ms-notificaciones no disponible: {}",
                    idUsuario, e.getMessage());
        }
    }

    // CREAR PEDIDO
    @Transactional
    public PedidoDTO crearPedido(CrearPedidoDTO dto) {
        log.info("[SERVICE] Iniciando creacion de pedido para usuario ID: {}", dto.getIdUsuario());

        // ── PASO 1: Obtener carrito ───
        CarritoDTO carrito;
        try {
            carrito = carritoClient.obtenerCarrito(dto.getIdUsuario());
            log.info("[SERVICE] Carrito obtenido para usuario {}. Items: {}",
                    dto.getIdUsuario(), carrito.getItems().size());

        } catch (feign.FeignException.NotFound e) {
            log.warn("[SERVICE] No existe carrito para usuario {}", dto.getIdUsuario());
            throw new IllegalStateException(
                "No existe carrito para el usuario " + dto.getIdUsuario()
            );

        } catch (feign.FeignException e) {
            log.warn("[SERVICE] ms-carrito no disponible: {}", e.getMessage());
            throw new IllegalStateException(
                "El servicio de carrito no esta disponible. Intenta nuevamente."
            );
        }

        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            log.warn("[SERVICE] Carrito del usuario {} esta vacio", dto.getIdUsuario());
            throw new IllegalStateException(
                "El carrito esta vacio. Agrega productos antes de confirmar."
            );
        }

        // ── PASO 2: Verificar stock de cada item ─────────────
        log.info("[SERVICE] Verificando stock para {} items", carrito.getItems().size());

        for (ItemCarritoDTO item : carrito.getItems()) {
            try {
                StockDTO stock = stockClient.consultarStock(item.getIdVariante());

                if (stock.getCantidadDisponible() < item.getCantidad()) {
                    log.warn("[SERVICE] Stock insuficiente para variante {}. Disponible: {}, Requerido: {}",
                            item.getIdVariante(), stock.getCantidadDisponible(), item.getCantidad());
                    throw new IllegalStateException(
                        "Stock insuficiente para la variante " + item.getIdVariante()
                        + ". Disponible: " + stock.getCantidadDisponible()
                        + ", requerido: " + item.getCantidad()
                    );
                }

                log.info("[SERVICE] Stock OK para variante {}. Disponible: {}",
                        item.getIdVariante(), stock.getCantidadDisponible());

            } catch (IllegalStateException e) {
                // Error de negocio — propagar
                throw e;

            } catch (feign.FeignException.NotFound e) {
                // Variante sin registro en ms-stock — asumimos disponible
                log.warn("[SERVICE] Variante {} sin registro en ms-stock. Continuando.",
                        item.getIdVariante());

            } catch (feign.FeignException e) {
                // ms-stock caído — asumimos disponible y continuamos
                log.warn("[SERVICE] ms-stock no disponible para variante {}: {}. Continuando.",
                        item.getIdVariante(), e.getMessage());
            }
        }

        // ── PASO 3: Crear pedido con snapshot ──
        Pedido pedido = new Pedido();
        pedido.setIdUsuario(dto.getIdUsuario());
        pedido.setTotal(carrito.getTotal());

        List<DetallePedido> detalles = carrito.getItems().stream()
                .map(item -> {
                    DetallePedido detalle = new DetallePedido();
                    detalle.setPedido(pedido);
                    detalle.setIdVariante(item.getIdVariante());
                    detalle.setNombreProducto("Variante " + item.getIdVariante());
                    detalle.setMl(0);
                    detalle.setCantidad(item.getCantidad());
                    detalle.setPrecioUnitario(item.getPrecioUnitario());
                    return detalle;
                })
                .toList();

        pedido.setDetalles(detalles);
        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        log.info("[SERVICE] Pedido creado con ID: {} | Total: {} | Estado: {}",
                pedidoGuardado.getIdPedido(), pedidoGuardado.getTotal(), pedidoGuardado.getEstado());

        // ── PASO 4: Reducir stock ─────────
        for (ItemCarritoDTO item : carrito.getItems()) {
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

        // ── PASO 5: Crear y procesar pago ────────────────────
        PagoDTO pagoProcessado;
        try {
            log.info("[SERVICE] Creando pago para pedido {}", pedidoGuardado.getIdPedido());

            PagoDTO pago = pagoClient.crearPago(
                new CrearPagoClientDTO(
                    pedidoGuardado.getIdPedido(),
                    pedidoGuardado.getTotal(),
                    "TARJETA"
                )
            );
            log.info("[SERVICE] Pago creado con ID: {}. Procesando...", pago.getIdTransaccion());

            pagoProcessado = pagoClient.procesarPago(pago.getIdTransaccion());
            log.info("[SERVICE] Resultado del pago: {}", pagoProcessado.getEstado());

        } catch (feign.FeignException e) {
            // ms-pagos caído — cancelamos el pedido
            log.error("[SERVICE] ms-pagos no disponible. Cancelando pedido {}: {}",
                    pedidoGuardado.getIdPedido(), e.getMessage());

            pedidoGuardado.setEstado(EstadoPedido.CANCELADO);
            pedidoRepository.save(pedidoGuardado);

            notificar(dto.getIdUsuario(),
                "No se pudo procesar tu pago. El pedido #"
                + pedidoGuardado.getIdPedido() + " fue cancelado. Intenta nuevamente.");

            throw new IllegalStateException(
                "El servicio de pagos no esta disponible. El pedido fue cancelado."
            );
        }

        // ── PASO 6 o 7: Según resultado del pago ─────────────
        if ("COMPLETADO".equals(pagoProcessado.getEstado())) {

            // Pago exitoso → PAGADO → crear envío → notificar
            pedidoGuardado.setEstado(EstadoPedido.PAGADO);
            pedidoRepository.save(pedidoGuardado);
            log.info("[SERVICE] Pedido {} actualizado a PAGADO", pedidoGuardado.getIdPedido());

            try {
                EnvioDTO envio = envioClient.crearEnvio(
                    new CrearEnvioClientDTO(
                        pedidoGuardado.getIdPedido(),
                        dto.getDireccionEntrega(),
                        dto.getCourier()
                    )
                );
                log.info("[SERVICE] Envio creado para pedido {}. Tracking: {}",
                        pedidoGuardado.getIdPedido(), envio.getNumeroTracking());

            } catch (feign.FeignException e) {
                // El pago ya fue procesado — el envío se puede crear manualmente después
                log.warn("[SERVICE] No se pudo crear envio para pedido {}: {}. " +
                        "El pago fue exitoso — el envio debe gestionarse manualmente.",
                        pedidoGuardado.getIdPedido(), e.getMessage());
            }

            notificar(dto.getIdUsuario(),
                "Tu pedido #" + pedidoGuardado.getIdPedido()
                + " fue confirmado y esta siendo procesado.");

        } else {

            // Pago rechazado → CANCELADO → notificar
            pedidoGuardado.setEstado(EstadoPedido.CANCELADO);
            pedidoRepository.save(pedidoGuardado);
            log.warn("[SERVICE] Pago rechazado. Pedido {} cancelado.",
                    pedidoGuardado.getIdPedido());

            notificar(dto.getIdUsuario(),
                "Tu pago fue rechazado. El pedido #"
                + pedidoGuardado.getIdPedido() + " fue cancelado.");
        }

        // ── PASO 8: Vaciar carrito ───────────────────────────
        try {
            carritoClient.vaciarCarrito(dto.getIdUsuario());
            log.info("[SERVICE] Carrito del usuario {} vaciado", dto.getIdUsuario());

        } catch (feign.FeignException e) {
            log.warn("[SERVICE] No se pudo vaciar carrito del usuario {}: {}",
                    dto.getIdUsuario(), e.getMessage());
        }

        return mapToDTO(pedidoGuardado);
    }
    
    // ACTUALIZAR ESTADO MANUALMENTE
    @Transactional
    public PedidoDTO actualizarEstado(Long idPedido, EstadoPedido nuevoEstado) {
        log.info("[SERVICE] Actualizando estado del pedido {} a {}", idPedido, nuevoEstado);

        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Pedido ID {} no encontrado al actualizar estado", idPedido);
                    return new PedidoNotFoundException(idPedido);
                });

        EstadoPedido estadoActual = pedido.getEstado();
        validarTransicion(estadoActual, nuevoEstado);

        pedido.setEstado(nuevoEstado);
        Pedido actualizado = pedidoRepository.save(pedido);

        log.info("[SERVICE] Pedido {} actualizado: {} → {}", idPedido, estadoActual, nuevoEstado);

        // Notificar al usuario según el nuevo estado
        String mensaje = switch (nuevoEstado) {
            case EN_PREPARACION -> "Tu pedido #" + idPedido + " esta siendo preparado.";
            case ENVIADO        -> "Tu pedido #" + idPedido + " ha sido despachado.";
            case ENTREGADO      -> "Tu pedido #" + idPedido + " fue entregado. Gracias por tu compra.";
            case CANCELADO      -> "Tu pedido #" + idPedido + " ha sido cancelado.";
            default             -> null;
        };

        if (mensaje != null) {
            notificar(pedido.getIdUsuario(), mensaje);
        }

        return mapToDTO(actualizado);
    }

    // CONSULTAR POR ID
    public PedidoDTO consultarPorId(Long idPedido) {
        log.info("[SERVICE] Consultando pedido ID: {}", idPedido);

        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Pedido ID {} no encontrado", idPedido);
                    return new PedidoNotFoundException(idPedido);
                });

        log.info("[SERVICE] Pedido {} encontrado. Estado: {} | Total: {}",
                idPedido, pedido.getEstado(), pedido.getTotal());

        return mapToDTO(pedido);
    }

    // LISTAR POR USUARIO
    public List<PedidoDTO> listarPorUsuario(Long idUsuario) {
        log.info("[SERVICE] Listando pedidos del usuario ID: {}", idUsuario);

        List<Pedido> pedidos = pedidoRepository.findByIdUsuario(idUsuario);

        log.info("[SERVICE] Usuario {} tiene {} pedidos", idUsuario, pedidos.size());
        return pedidos.stream().map(this::mapToDTO).toList();
    }

    // LISTAR TODOS
    public List<PedidoDTO> listarTodos() {
        log.info("[SERVICE] Listando todos los pedidos");

        List<Pedido> pedidos = pedidoRepository.findAll();

        log.info("[SERVICE] Total de pedidos: {}", pedidos.size());
        return pedidos.stream().map(this::mapToDTO).toList();
    }

    // MAPPER
    private PedidoDTO mapToDTO(Pedido pedido) {
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