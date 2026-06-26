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
    private final CatalogoClient     catalogoClient;
    private final StockClient        stockClient;
    private final PagoClient         pagoClient;
    private final EnvioClient        envioClient;
    private final NotificacionClient notificacionClient;

    @Override
    @Transactional
    public PedidoDTO crearPedido(CrearPedidoDTO dto, Long idUsuario) {
        log.info("[AUDIT idUsuario={}] Iniciando creacion de pedido", idUsuario);
        CarritoDTO carrito = obtenerCarrito(idUsuario);
        validarCarritoNoVacio(carrito, idUsuario);
        validarMontoTotalPorPagar(carrito);

        Pedido pedidoGuardado = pedidoRepository.save(
            buildPedido(idUsuario, carrito));
        log.info("[AUDIT idUsuario={}] Pedido {} creado en estado CREADO. Total: {}",
                idUsuario, pedidoGuardado.getIdPedido(), pedidoGuardado.getTotal());

        // Guardamos dirección y courier en el pedido para usarlos después
        pedidoGuardado.setDireccionEntrega(dto.getDireccionEntrega());
        pedidoGuardado.setCourier(dto.getCourier());
        pedidoRepository.save(pedidoGuardado);

        // RESERVAR stock con claves determinísticas (Fix #1)
        reservarStockPedido(pedidoGuardado);
        log.info("[AUDIT pedidoId={}] Stock reservado exitosamente", pedidoGuardado.getIdPedido());

        return mapToResponse(pedidoGuardado);
    }

    @Override
    @Transactional
    public PedidoDTO pagarPedido(Long idPedido) {
        log.info("[AUDIT pedidoId={}] Iniciando pago", idPedido);
        Pedido pedido = obtenerPorId(idPedido);

        if (pedido.getEstado() != EstadoPedido.CREADO) {
            log.warn("[AUDIT pedidoId={}] Estado invalido para pago: {}", idPedido, pedido.getEstado());
            throw new IllegalStateException(
                "El pedido debe estar en estado CREADO para pagarlo. Estado actual: " + pedido.getEstado());
        }

        PagoDTO pagoProcesado = crearYProcesarPago(pedido);

        if ("COMPLETADO".equals(pagoProcesado.getEstado())) {
            // Pago exitoso → ejecutar post-pago completo (stock, envio, carrito, notificacion)
            ejecutarPostPago(pedido);
            log.info("[AUDIT pedidoId={}] Pago exitoso y pedido confirmado → EN_PREPARACION", idPedido);
        } else {
            log.warn("[AUDIT pedidoId={}] Pago rechazado: {}. Pedido queda en CREADO para reintentar.",
                    idPedido, pagoProcesado.getEstado());
            throw new IllegalStateException(
                "El pago fue rechazado. Puedes reintentar el pago con POST /api/pedidos/" + idPedido + "/pagar");
        }

        return mapToResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoDTO confirmarPedido(Long idPedido) {
        log.info("[AUDIT pedidoId={}] Confirmando pedido post-pago", idPedido);
        Pedido pedido = obtenerPorId(idPedido);

        if (pedido.getEstado() != EstadoPedido.PAGADO) {
            log.warn("[AUDIT pedidoId={}] Estado invalido para confirmar: {}", idPedido, pedido.getEstado());
            throw new IllegalStateException(
                "El pedido debe estar en estado PAGADO para confirmarlo. Estado actual: " + pedido.getEstado());
        }

        // Para pedidos legacy en PAGADO, ejecutar post-pago
        ejecutarPostPago(pedido);
        log.info("[AUDIT pedidoId={}] Pedido confirmado (legacy PAGADO) → EN_PREPARACION", idPedido);

        return mapToResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoDTO actualizarEstado(Long idPedido, EstadoPedido nuevoEstado) {
        log.info("[AUDIT pedidoId={}] Cambio de estado a {}", idPedido, nuevoEstado);
        Pedido pedido       = obtenerPorId(idPedido);
        EstadoPedido actual = pedido.getEstado();
        validarTransicion(actual, nuevoEstado);

        if (actual == EstadoPedido.EN_PREPARACION && nuevoEstado == EstadoPedido.CANCELADO) {
            log.info("[AUDIT pedidoId={}] Cancelando pedido EN_PREPARACION — restaurando stock", idPedido);
            restaurarStockPedido(pedido);
        } else if ((actual == EstadoPedido.CREADO || actual == EstadoPedido.PAGADO)
                && nuevoEstado == EstadoPedido.CANCELADO) {
            log.info("[AUDIT pedidoId={}] Cancelando pedido {} — liberando reserva", idPedido, actual);
            liberarReservaPedido(pedido);
        }

        pedido.setEstado(nuevoEstado);
        Pedido actualizado = pedidoRepository.save(pedido);
        log.info("[AUDIT pedidoId={}] Estado: {} → {}", idPedido, actual, nuevoEstado);
        notificarCambioEstado(pedido.getIdUsuario(), idPedido, nuevoEstado);
        return mapToResponse(actualizado);
    }

    @Override
    public PedidoDTO consultarPorId(Long idPedido) {
        log.info("[AUDIT pedidoId={}] Consultando pedido", idPedido);
        Pedido pedido = obtenerPorId(idPedido);
        log.info("[AUDIT pedidoId={}] Estado: {} | Total: {}", idPedido, pedido.getEstado(), pedido.getTotal());
        return mapToResponse(pedido);
    }

    @Override
    public List<PedidoDTO> listarPorUsuario(Long idUsuario) {
        log.info("[AUDIT idUsuario={}] Listando pedidos", idUsuario);
        List<Pedido> pedidos = pedidoRepository.findByIdUsuario(idUsuario);
        log.info("[AUDIT idUsuario={}] Tiene {} pedidos", idUsuario, pedidos.size());
        return pedidos.stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<PedidoDTO> listarTodos() {
        log.info("[AUDIT] Listando todos los pedidos");
        List<Pedido> pedidos = pedidoRepository.findAll();
        log.info("[AUDIT] Total pedidos: {}", pedidos.size());
        return pedidos.stream().map(this::mapToResponse).toList();
    }

    // ─────────────────────────────────────────────────────────
    // MÉTODOS AUXILIARES PRIVADOS — Feign
    // ─────────────────────────────────────────────────────────

    private CarritoDTO obtenerCarrito(Long idUsuario) {
        try {
            CarritoDTO carrito = carritoClient.obtenerCarrito();
            log.info("[AUDIT idUsuario={}] Carrito obtenido. Items: {}", idUsuario, carrito.getItems().size());
            return carrito;
        } catch (feign.FeignException.NotFound e) {
            log.warn("[AUDIT idUsuario={}] No existe carrito", idUsuario);
            throw new IllegalStateException("No existe carrito para el usuario " + idUsuario);
        } catch (feign.FeignException e) {
            log.warn("[AUDIT idUsuario={}] ms-carrito no disponible: {}", idUsuario, e.getMessage());
            throw new IllegalStateException("El servicio de carrito no esta disponible. Intenta nuevamente.");
        }
    }

    private void restaurarStockPedido(Pedido pedido) {
        for (DetallePedido detalle : pedido.getDetalles()) {
            try {
                stockClient.reponerStock(
                    detalle.getIdVariante(),
                    new ReponerStockClientDTO(detalle.getCantidad())
                );
                log.info("[AUDIT pedidoId={}] Stock repuesto variante {}: +{}", pedido.getIdPedido(), detalle.getIdVariante(), detalle.getCantidad());
            } catch (feign.FeignException e) {
                log.error("[AUDIT pedidoId={}] Error reponiendo stock variante {}: status={}", pedido.getIdPedido(), detalle.getIdVariante(), e.status());
            }
        }
    }

    private PagoDTO crearYProcesarPago(Pedido pedido) {
        Long idPedido = pedido.getIdPedido();

        // 1) Buscar si ya existe un pago para este pedido (ej. RECHAZADO previamente)
        try {
            PagoDTO existente = pagoClient.consultarPorPedido(idPedido);
            if (existente != null && existente.getIdTransaccion() != null) {
                log.info("[AUDIT pedidoId={}] Pago existente: ID={} estado={}",
                        idPedido, existente.getIdTransaccion(), existente.getEstado());

                if ("COMPLETADO".equals(existente.getEstado())) {
                    return existente; // Ya fue completado, reusamos
                }

                if ("RECHAZADO".equals(existente.getEstado())) {
                    log.info("[AUDIT pedidoId={}] Reintentando pago {}", idPedido, existente.getIdTransaccion());
                    PagoDTO reintentado = pagoClient.reintentarPago(existente.getIdTransaccion());
                    log.info("[AUDIT pedidoId={}] Reintento resultado: {}", idPedido, reintentado.getEstado());
                    return reintentado;
                }
            }
        } catch (feign.FeignException.NotFound e) {
            log.info("[AUDIT pedidoId={}] No hay pago previo. Creando uno nuevo...", idPedido);
        } catch (feign.FeignException e) {
            log.warn("[AUDIT pedidoId={}] No se pudo consultar pago existente, creando nuevo: {}",
                    idPedido, e.getMessage());
        }

        // 2) No existe pago previo — creamos y procesamos
        try {
            log.info("[AUDIT pedidoId={}] Creando pago", idPedido);
            PagoDTO pago = pagoClient.crearPago(
                new CrearPagoClientDTO(idPedido, pedido.getTotal(), "TARJETA")
            );
            log.info("[AUDIT pedidoId={}] Pago {} creado. Procesando...", idPedido, pago.getIdTransaccion());
            PagoDTO procesado = pagoClient.procesarPago(pago.getIdTransaccion());
            log.info("[AUDIT pedidoId={}] Resultado pago: {}", idPedido, procesado.getEstado());
            return procesado;
        } catch (feign.FeignException e) {
            log.error("[AUDIT pedidoId={}] ms-pagos no disponible. Cancelando pedido: status={} msg={}",
                    idPedido, e.status(), e.getMessage());
            pedido.setEstado(EstadoPedido.CANCELADO);
            pedidoRepository.save(pedido);
            notificar(pedido.getIdUsuario(),
                "No se pudo procesar tu pago. El pedido #" + idPedido + " fue cancelado.");
            throw new IllegalStateException(
                "El servicio de pagos no esta disponible (ms-pagos:8087). "
                + "Enciende ms-pagos y verifica: POST http://localhost:8087/api/pagos");
        }
    }

    private void ejecutarPostPago(Pedido pedido) {
        Long idPedido = pedido.getIdPedido();

        // CONFIRMAR reserva (stock sale del sistema definitivamente)
        confirmarReservaPedido(pedido);
        log.info("[AUDIT pedidoId={}] Reserva confirmada. Creando envio...", idPedido);

        try {
            // Crear envío
            crearEnvio(idPedido, pedido.getDireccionEntrega(), pedido.getCourier());
            // Vaciar carrito
            vaciarCarritoUsuario(pedido.getIdUsuario());
        } catch (Exception e) {
            // COMPENSACIÓN: si falla envio o carrito, reponemos stock
            log.error("[AUDIT pedidoId={}] Fallo posterior a confirmar reserva. Reponiendo stock: {}",
                    idPedido, e.getMessage());
            restaurarStockPedido(pedido);
            pedido.setEstado(EstadoPedido.PAGADO);
            pedidoRepository.save(pedido);
            throw e;
        }

        // Avanzar estado a EN_PREPARACION
        pedido.setEstado(EstadoPedido.EN_PREPARACION);
        pedidoRepository.save(pedido);
        log.info("[AUDIT pedidoId={}] Pedido confirmado → EN_PREPARACION", idPedido);

        // Notificar
        notificar(pedido.getIdUsuario(),
            "Tu pedido #" + idPedido + " fue confirmado y esta siendo procesado.");
    }

    private void crearEnvio(Long idPedido, String direccion, String courier) {
        try {
            EnvioDTO envio = envioClient.crearEnvio(
                new CrearEnvioClientDTO(idPedido, direccion, courier)
            );
            log.info("[AUDIT pedidoId={}] Envio creado. Tracking: {}", idPedido, envio.getNumeroTracking());
        } catch (feign.FeignException e) {
            log.warn("[AUDIT pedidoId={}] No se pudo crear envio: {}. Gestion manual.", idPedido, e.getMessage());
        }
    }

    private void vaciarCarritoUsuario(Long idUsuario) {
        try {
            carritoClient.vaciarCarrito();
            log.info("[AUDIT idUsuario={}] Carrito vaciado", idUsuario);
        } catch (feign.FeignException.NotFound e) {
            log.warn("[AUDIT idUsuario={}] No existe carrito para vaciar", idUsuario);
        } catch (feign.FeignException e) {
            log.error("[AUDIT idUsuario={}] No se pudo vaciar carrito: status={}", idUsuario, e.status());
            throw new IllegalStateException(
                "El pedido fue confirmado pero no se pudo vaciar el carrito. "
                + "Verifica ms-carrito (puerto 8086) o vacia manualmente mediante "
                + "DELETE /api/carrito");
        }
    }

    private void notificar(Long idUsuario, String mensaje) {
        try {
            notificacionClient.enviarNotificacion(
                new CrearNotificacionDTO(idUsuario, mensaje)
            );
            log.info("[AUDIT idUsuario={}] Notificacion enviada: {}", idUsuario, mensaje);
        } catch (feign.FeignException e) {
            log.warn("[AUDIT idUsuario={}] No se pudo notificar: {}", idUsuario, e.getMessage());
        }
    }

    private Pedido obtenerPorId(Long idPedido) {
        return pedidoRepository.findById(idPedido)
                .orElseThrow(() -> {
                    log.warn("[AUDIT pedidoId={}] No encontrado", idPedido);
                    return new PedidoNotFoundException(idPedido);
                });
    }

    private void validarCarritoNoVacio(CarritoDTO carrito, Long idUsuario) {
        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            log.warn("[AUDIT idUsuario={}] Carrito vacio", idUsuario);
            throw new IllegalStateException("El carrito esta vacio. Agrega productos antes de confirmar.");
        }
    }

    private void validarMontoTotalPorPagar(CarritoDTO carrito) {
        if (carrito.getTotal() == null || carrito.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[AUDIT] Carrito sin monto. idCarrito={} total={}", carrito.getIdCarrito(), carrito.getTotal());
            throw new IllegalStateException(
                "No se puede crear el pedido porque no hay nada que pagar. "
                + "Agrega productos con precio mayor a 0 antes de confirmar.");
        }
    }

    private void validarTransicion(EstadoPedido actual, EstadoPedido solicitado) {
        boolean valida = switch (actual) {
            case CREADO         -> solicitado == EstadoPedido.PAGADO
                                || solicitado == EstadoPedido.EN_PREPARACION
                                || solicitado == EstadoPedido.CANCELADO;
            case PAGADO         -> solicitado == EstadoPedido.EN_PREPARACION
                                || solicitado == EstadoPedido.CANCELADO;
            case EN_PREPARACION -> solicitado == EstadoPedido.ENVIADO
                                || solicitado == EstadoPedido.CANCELADO;
            case ENVIADO        -> solicitado == EstadoPedido.ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };
        if (!valida) {
            log.warn("[AUDIT] Transicion invalida: {} → {}", actual, solicitado);
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
        VarianteResponseDTO variante = consultarVarianteCatalogo(item.getIdVariante());
        detalle.setNombreProducto(
                variante.getSku() != null && !variante.getSku().isBlank()
                        ? variante.getSku()
                        : "Variante " + item.getIdVariante());
        detalle.setMl(variante.getMl());
        detalle.setCantidad(item.getCantidad());
        // Usar precio ACTUAL del catálogo, no el snapshot del carrito (Fix #5)
        detalle.setPrecioUnitario(variante.getPrecio());
        return detalle;
    }

    private VarianteResponseDTO consultarVarianteCatalogo(Long idVariante) {
        try {
            VarianteResponseDTO variante = catalogoClient.consultarVariante(idVariante);
            if (variante.getMl() == null || variante.getMl() <= 0) {
                throw new IllegalStateException(
                        "La variante " + idVariante + " no tiene ml valido en el catalogo.");
            }
            return variante;
        } catch (IllegalStateException e) {
            throw e;
        } catch (feign.FeignException.NotFound e) {
            throw new IllegalStateException("La variante " + idVariante + " no existe en el catalogo.");
        } catch (feign.FeignException e) {
            throw new IllegalStateException(
                    "El catalogo no esta disponible (ms-catalogo:8084). "
                    + "Verifica GET http://localhost:8084/api/catalogo/variantes/" + idVariante);
        }
    }

    private void reservarStockPedido(Pedido pedido) {
        for (DetallePedido detalle : pedido.getDetalles()) {
            try {
                String key = "RESERVA_PEDIDO_" + pedido.getIdPedido() + "_VARIANTE_" + detalle.getIdVariante();
                stockClient.reservarStock(
                    detalle.getIdVariante(),
                    new ReservarStockClientDTO(detalle.getCantidad(), key)
                );
                log.info("[AUDIT pedidoId={}] Stock reservado variante {}: -{}",
                        pedido.getIdPedido(), detalle.getIdVariante(), detalle.getCantidad());
            } catch (feign.FeignException e) {
                log.error("[AUDIT pedidoId={}] Error reservando stock variante {}: status={}",
                        pedido.getIdPedido(), detalle.getIdVariante(), e.status());
                // Liberar reservas ya hechas (compensación parcial)
                liberarReservaPedido(pedido);
                notificar(pedido.getIdUsuario(),
                    "No se pudo completar tu pedido #" + pedido.getIdPedido()
                    + " por falta de stock. Se ha liberado la reserva.");
                throw new IllegalStateException(
                    "No se pudo reservar stock para la variante " + detalle.getIdVariante()
                    + ". Stock insuficiente o ms-stock no disponible.");
            }
        }
    }

    private void confirmarReservaPedido(Pedido pedido) {
        for (DetallePedido detalle : pedido.getDetalles()) {
            try {
                String key = "CONFIRMAR_PEDIDO_" + pedido.getIdPedido() + "_VARIANTE_" + detalle.getIdVariante();
                stockClient.confirmarReserva(
                    detalle.getIdVariante(),
                    new ConfirmarReservaClientDTO(detalle.getCantidad(), key)
                );
                log.info("[AUDIT pedidoId={}] Reserva confirmada variante {}: -{}",
                        pedido.getIdPedido(), detalle.getIdVariante(), detalle.getCantidad());
            } catch (feign.FeignException e) {
                log.error("[AUDIT pedidoId={}] Error confirmando reserva variante {}: status={}",
                        pedido.getIdPedido(), detalle.getIdVariante(), e.status());
                throw new IllegalStateException(
                    "No se pudo confirmar la reserva de stock para la variante " + detalle.getIdVariante()
                    + ". El pedido esta PAGADO; revisa ms-stock.");
            }
        }
    }

    private void liberarReservaPedido(Pedido pedido) {
        for (DetallePedido detalle : pedido.getDetalles()) {
            try {
                String key = "LIBERAR_PEDIDO_" + pedido.getIdPedido() + "_VARIANTE_" + detalle.getIdVariante();
                stockClient.liberarReserva(
                    detalle.getIdVariante(),
                    new LiberarReservaClientDTO(detalle.getCantidad(), key)
                );
                log.info("[AUDIT pedidoId={}] Reserva liberada variante {}: +{}",
                        pedido.getIdPedido(), detalle.getIdVariante(), detalle.getCantidad());
            } catch (feign.FeignException e) {
                log.error("[AUDIT pedidoId={}] Error liberando reserva variante {}: status={}",
                        pedido.getIdPedido(), detalle.getIdVariante(), e.status());
            }
        }
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
            pedido.getDireccionEntrega(),
            pedido.getCourier(),
            detallesDTO
        );
    }
}