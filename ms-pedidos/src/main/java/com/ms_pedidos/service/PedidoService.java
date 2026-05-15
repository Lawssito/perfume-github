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

    private final PedidoRepository    pedidoRepository;
    private final CarritoClient       carritoClient;
    private final StockClient         stockClient;
    private final PagoClient          pagoClient;
    private final EnvioClient         envioClient;
    private final NotificacionClient  notificacionClient;

    // HELPER — validar transiciones de estado

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

    // HELPER — notificar sin romper el flujo principal

    /**
     * Las notificaciones son informativas — si ms-notificaciones está caído
     * no debe cancelar toda la operación. Por eso el try/catch absorbe
     * el error y solo deja trazabilidad en el log.
     */
    private void notificar(Long idUsuario, String mensaje) {
        try {
            notificacionClient.enviarNotificacion(
                new CrearNotificacionDTO(idUsuario, mensaje)
            );
            log.info("[SERVICE] Notificacion enviada al usuario {}: {}", idUsuario, mensaje);
        } catch (Exception e) {
            log.warn("[SERVICE] No se pudo notificar al usuario {}: {}", idUsuario, e.getMessage());
        }
    }

    // CREAR PEDIDO — orquesta todo el flujo de compra

    /**
     * Flujo completo:
     * 1. Obtiene el carrito del usuario desde ms-carrito
     * 2. Verifica stock de cada item en ms-stock
     * 3. Crea el pedido con los detalles (snapshot)
     * 4. Reduce el stock en ms-stock
     * 5. Crea y procesa el pago en ms-pagos
     * 6. Si pago COMPLETADO → crea envío en ms-envios + notifica
     * 7. Si pago RECHAZADO  → cancela pedido + notifica
     * 8. Vacía el carrito en ms-carrito
     */
    @Transactional
    public PedidoDTO crearPedido(CrearPedidoDTO dto) {
        log.info("[SERVICE] Iniciando creacion de pedido para usuario ID: {}", dto.getIdUsuario());

        try {
            // ── PASO 1: Obtener carrito ──────────────────────
            log.info("[SERVICE] Consultando carrito del usuario {}", dto.getIdUsuario());
            CarritoDTO carrito = carritoClient.obtenerCarrito(dto.getIdUsuario());

            if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
                log.warn("[SERVICE] El carrito del usuario {} esta vacio", dto.getIdUsuario());
                throw new IllegalStateException("El carrito esta vacio. Agrega productos antes de confirmar.");
            }

            log.info("[SERVICE] Carrito obtenido con {} items. Total: {}",
                    carrito.getItems().size(), carrito.getTotal());

            // ── PASO 2: Verificar stock de cada item ─────────
            log.info("[SERVICE] Verificando stock para {} items", carrito.getItems().size());
            for (ItemCarritoDTO item : carrito.getItems()) {
                try {
                    StockDTO stock = stockClient.consultarStock(item.getIdVariante());
                    if (stock.getCantidadDisponible() < item.getCantidad()) {
                        log.warn("[SERVICE] Stock insuficiente para variante {}. Disponible: {}, Requerido: {}",
                                item.getIdVariante(), stock.getCantidadDisponible(), item.getCantidad());
                        throw new IllegalStateException(
                            "Stock insuficiente para la variante " + item.getIdVariante()
                        );
                    }
                    log.info("[SERVICE] Stock OK para variante {}. Disponible: {}",
                            item.getIdVariante(), stock.getCantidadDisponible());
                } catch (IllegalStateException e) {
                    throw e;
                } catch (Exception e) {
                    // ms-stock caído — asumimos disponible y continuamos
                    log.warn("[SERVICE] No se pudo verificar stock para variante {}: {}. Continuando.",
                            item.getIdVariante(), e.getMessage());
                }
            }

            // ── PASO 3: Crear pedido con snapshot ────────────
            Pedido pedido = new Pedido();
            pedido.setIdUsuario(dto.getIdUsuario());
            pedido.setTotal(carrito.getTotal());

            List<DetallePedido> detalles = carrito.getItems().stream()
                    .map(item -> {
                        DetallePedido detalle = new DetallePedido();
                        detalle.setPedido(pedido);
                        detalle.setIdVariante(item.getIdVariante());
                        // Snapshot — nombre y ml se consultan a ms-catalogo en una
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

            // ── PASO 4: Reducir stock ────────────────────────
            for (ItemCarritoDTO item : carrito.getItems()) {
                try {
                    stockClient.reducirStock(
                        item.getIdVariante(),
                        new ReducirStockClientDTO(item.getCantidad())
                    );
                    log.info("[SERVICE] Stock reducido para variante {}. Cantidad: {}",
                            item.getIdVariante(), item.getCantidad());
                } catch (Exception e) {
                    log.warn("[SERVICE] No se pudo reducir stock para variante {}: {}",
                            item.getIdVariante(), e.getMessage());
                }
            }

            // ── PASO 5: Crear y procesar pago ────────────────
            log.info("[SERVICE] Creando pago para pedido {}", pedidoGuardado.getIdPedido());
            PagoDTO pago = pagoClient.crearPago(
                new CrearPagoClientDTO(
                    pedidoGuardado.getIdPedido(),
                    pedidoGuardado.getTotal(),
                    "TARJETA"
                )
            );
            log.info("[SERVICE] Pago creado con ID: {}. Procesando...", pago.getIdTransaccion());

            PagoDTO pagoProcessado = pagoClient.procesarPago(pago.getIdTransaccion());
            log.info("[SERVICE] Resultado del pago: {}", pagoProcessado.getEstado());

            // ── PASO 6 o 7: Según resultado del pago ─────────
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
                } catch (Exception e) {
                    log.warn("[SERVICE] No se pudo crear envio para pedido {}: {}",
                            pedidoGuardado.getIdPedido(), e.getMessage());
                }

                notificar(dto.getIdUsuario(),
                    "Tu pedido #" + pedidoGuardado.getIdPedido()
                    + " fue confirmado y esta siendo procesado.");

            } else {

                // Pago rechazado → CANCELADO → notificar
                pedidoGuardado.setEstado(EstadoPedido.CANCELADO);
                pedidoRepository.save(pedidoGuardado);
                log.warn("[SERVICE] Pago rechazado. Pedido {} cancelado.", pedidoGuardado.getIdPedido());

                notificar(dto.getIdUsuario(),
                    "Tu pago fue rechazado. El pedido #"
                    + pedidoGuardado.getIdPedido() + " fue cancelado.");
            }

            // ── PASO 8: Vaciar carrito ───────────────────────
            try {
                carritoClient.vaciarCarrito(dto.getIdUsuario());
                log.info("[SERVICE] Carrito del usuario {} vaciado exitosamente", dto.getIdUsuario());
            } catch (Exception e) {
                log.warn("[SERVICE] No se pudo vaciar el carrito del usuario {}: {}",
                        dto.getIdUsuario(), e.getMessage());
            }

            return mapToDTO(pedidoGuardado);

        } catch (IllegalStateException e) {
            log.warn("[SERVICE] Error de negocio al crear pedido para usuario {}: {}",
                    dto.getIdUsuario(), e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al crear pedido para usuario {}: {}",
                    dto.getIdUsuario(), e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────
    // ACTUALIZAR ESTADO MANUALMENTE
    // ─────────────────────────────────────────────────────────

    @Transactional
    public PedidoDTO actualizarEstado(Long idPedido, EstadoPedido nuevoEstado) {
        log.info("[SERVICE] Actualizando estado del pedido {} a {}", idPedido, nuevoEstado);

        try {
            Pedido pedido = pedidoRepository.findById(idPedido)
                    .orElseThrow(() -> new PedidoNotFoundException(idPedido));

            EstadoPedido estadoActual = pedido.getEstado();
            validarTransicion(estadoActual, nuevoEstado);

            pedido.setEstado(nuevoEstado);
            Pedido actualizado = pedidoRepository.save(pedido);

            log.info("[SERVICE] Pedido {} actualizado: {} → {}",
                    idPedido, estadoActual, nuevoEstado);

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

        } catch (PedidoNotFoundException | TransicionEstadoInvalidaException e) {
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al actualizar estado del pedido {}: {}",
                    idPedido, e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────
    // CONSULTAR POR ID
    // ─────────────────────────────────────────────────────────

    public PedidoDTO consultarPorId(Long idPedido) {
        log.info("[SERVICE] Consultando pedido ID: {}", idPedido);

        try {
            Pedido pedido = pedidoRepository.findById(idPedido)
                    .orElseThrow(() -> new PedidoNotFoundException(idPedido));

            log.info("[SERVICE] Pedido {} encontrado. Estado: {} | Total: {}",
                    idPedido, pedido.getEstado(), pedido.getTotal());

            return mapToDTO(pedido);

        } catch (PedidoNotFoundException e) {
            log.warn("[SERVICE] Pedido ID {} no encontrado", idPedido);
            throw e;

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al consultar pedido {}: {}",
                    idPedido, e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────
    // LISTAR POR USUARIO
    // ─────────────────────────────────────────────────────────

    public List<PedidoDTO> listarPorUsuario(Long idUsuario) {
        log.info("[SERVICE] Listando pedidos del usuario ID: {}", idUsuario);

        try {
            List<Pedido> pedidos = pedidoRepository.findByIdUsuario(idUsuario);
            log.info("[SERVICE] Usuario {} tiene {} pedidos", idUsuario, pedidos.size());
            return pedidos.stream().map(this::mapToDTO).toList();

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al listar pedidos del usuario {}: {}",
                    idUsuario, e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────
    // LISTAR TODOS
    // ─────────────────────────────────────────────────────────

    public List<PedidoDTO> listarTodos() {
        log.info("[SERVICE] Listando todos los pedidos");

        try {
            List<Pedido> pedidos = pedidoRepository.findAll();
            log.info("[SERVICE] Total de pedidos: {}", pedidos.size());
            return pedidos.stream().map(this::mapToDTO).toList();

        } catch (Exception e) {
            log.error("[SERVICE] Error inesperado al listar pedidos: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────
    // MAPPER
    // ─────────────────────────────────────────────────────────

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