package com.ms_pedidos.service;

import com.ms_pedidos.client.*;
import com.ms_pedidos.client.dto.*;
import com.ms_pedidos.dto.*;
import com.ms_pedidos.exception.*;
import com.ms_pedidos.model.*;
import com.ms_pedidos.repository.PedidoRepository;
import com.ms_pedidos.service.impl.PedidoServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private CarritoClient carritoClient;
    @Mock
    private CatalogoClient catalogoClient;
    @Mock
    private StockClient stockClient;
    @Mock
    private PagoClient pagoClient;
    @Mock
    private EnvioClient envioClient;
    @Mock
    private NotificacionClient notificacionClient;

    private PedidoServiceImpl pedidoService;

    private CarritoDTO carritoDTO;
    private ItemCarritoDTO itemCarrito;
    private VarianteResponseDTO varianteResponse;

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoServiceImpl(pedidoRepository, carritoClient, catalogoClient, stockClient, pagoClient, envioClient, notificacionClient);

        itemCarrito = new ItemCarritoDTO();
        itemCarrito.setIdItem(1L);
        itemCarrito.setIdVariante(1L);
        itemCarrito.setCantidad(2);
        itemCarrito.setPrecioUnitario(new BigDecimal("49.990"));
        itemCarrito.setSubtotal(new BigDecimal("99.980"));

        carritoDTO = new CarritoDTO();
        carritoDTO.setIdCarrito(1L);
        carritoDTO.setIdUsuario(1L);
        carritoDTO.setItems(List.of(itemCarrito));
        carritoDTO.setTotal(new BigDecimal("99.980"));

        varianteResponse = new VarianteResponseDTO();
        varianteResponse.setIdVariante(1L);
        varianteResponse.setIdPerfume(1L);
        varianteResponse.setSku("SKU001");
        varianteResponse.setMl(100);
        varianteResponse.setPrecio(new BigDecimal("49.990"));
    }

    @Test
    void crearPedido_CuandoValido_RetornaPedidoDTO() {
        CrearPedidoDTO dto = new CrearPedidoDTO();
        dto.setDireccionEntrega("Av. Providencia 123");
        dto.setCourier("Starken");

        when(carritoClient.obtenerCarrito()).thenReturn(carritoDTO);
        when(catalogoClient.consultarVariante(1L)).thenReturn(varianteResponse);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setIdPedido(1L);
            p.setEstado(EstadoPedido.CREADO);
            p.setFechaCreacion(LocalDateTime.now());
            for (DetallePedido d : p.getDetalles()) {
                d.setIdDetalle(1L);
            }
            return p;
        });

        PedidoDTO resultado = pedidoService.crearPedido(dto, 1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getIdPedido());
        assertEquals(1L, resultado.getIdUsuario());
        assertEquals(EstadoPedido.CREADO, resultado.getEstado());
        assertEquals(new BigDecimal("99.980"), resultado.getTotal());
        assertEquals("Av. Providencia 123", resultado.getDireccionEntrega());
        assertEquals("Starken", resultado.getCourier());
        assertEquals(1, resultado.getDetalles().size());
        verify(pedidoRepository, times(2)).save(any(Pedido.class));
        verify(stockClient).reservarStock(eq(1L), any(ReservarStockClientDTO.class));
    }

    @Test
    void crearPedido_CuandoCarritoVacio_LanzaIllegalStateException() {
        CrearPedidoDTO dto = new CrearPedidoDTO();
        dto.setDireccionEntrega("Av. Providencia 123");
        dto.setCourier("Starken");
        carritoDTO.setItems(new ArrayList<>());
        carritoDTO.setTotal(BigDecimal.ZERO);

        when(carritoClient.obtenerCarrito()).thenReturn(carritoDTO);

        assertThrows(IllegalStateException.class, () -> pedidoService.crearPedido(dto, 1L));
    }

    @Test
    void crearPedido_CuandoMontoCero_LanzaIllegalStateException() {
        CrearPedidoDTO dto = new CrearPedidoDTO();
        dto.setDireccionEntrega("Av. Providencia 123");
        dto.setCourier("Starken");
        carritoDTO.setTotal(BigDecimal.ZERO);

        when(carritoClient.obtenerCarrito()).thenReturn(carritoDTO);

        assertThrows(IllegalStateException.class, () -> pedidoService.crearPedido(dto, 1L));
    }

    @Test
    void pagarPedido_CuandoValidoYExitoso_RetornaPedidoDTO() {
        Pedido pedido = buildPedidoConDetalle();
        pedido.setEstado(EstadoPedido.CREADO);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagoClient.consultarPorPedido(1L)).thenThrow(mock(FeignException.NotFound.class));

        PagoDTO pagoCreado = new PagoDTO();
        pagoCreado.setIdTransaccion(100L);
        pagoCreado.setIdPedido(1L);
        pagoCreado.setEstado("PENDIENTE");
        when(pagoClient.crearPago(any(CrearPagoClientDTO.class))).thenReturn(pagoCreado);

        PagoDTO pagoProcesado = new PagoDTO();
        pagoProcesado.setIdTransaccion(100L);
        pagoProcesado.setIdPedido(1L);
        pagoProcesado.setEstado("COMPLETADO");
        when(pagoClient.procesarPago(100L)).thenReturn(pagoProcesado);

        EnvioDTO envioDTO = new EnvioDTO();
        envioDTO.setIdEnvio(10L);
        envioDTO.setNumeroTracking("TRK001");
        when(envioClient.crearEnvio(any(CrearEnvioClientDTO.class))).thenReturn(envioDTO);

        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        PedidoDTO resultado = pedidoService.pagarPedido(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getIdPedido());
        verify(stockClient).confirmarReserva(eq(1L), any(ConfirmarReservaClientDTO.class));
        verify(envioClient).crearEnvio(any(CrearEnvioClientDTO.class));
        verify(carritoClient).vaciarCarrito();
        verify(notificacionClient).enviarNotificacion(any(CrearNotificacionDTO.class));
    }

    @Test
    void pagarPedido_CuandoEstadoNoEsCREADO_LanzaIllegalStateException() {
        Pedido pedido = buildPedidoConDetalle();
        pedido.setEstado(EstadoPedido.EN_PREPARACION);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThrows(IllegalStateException.class, () -> pedidoService.pagarPedido(1L));
    }

    @Test
    void pagarPedido_CuandoPagoRechazado_LanzaIllegalStateException() {
        Pedido pedido = buildPedidoConDetalle();
        pedido.setEstado(EstadoPedido.CREADO);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagoClient.consultarPorPedido(1L)).thenThrow(mock(FeignException.NotFound.class));

        PagoDTO pagoCreado = new PagoDTO();
        pagoCreado.setIdTransaccion(100L);
        pagoCreado.setIdPedido(1L);
        pagoCreado.setEstado("PENDIENTE");
        when(pagoClient.crearPago(any(CrearPagoClientDTO.class))).thenReturn(pagoCreado);

        PagoDTO pagoRechazado = new PagoDTO();
        pagoRechazado.setIdTransaccion(100L);
        pagoRechazado.setIdPedido(1L);
        pagoRechazado.setEstado("RECHAZADO");
        when(pagoClient.procesarPago(100L)).thenReturn(pagoRechazado);

        assertThrows(IllegalStateException.class, () -> pedidoService.pagarPedido(1L));
    }

    @Test
    void consultarPorId_CuandoExiste_RetornaPedidoDTO() {
        Pedido pedido = buildPedidoConDetalle();

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        PedidoDTO resultado = pedidoService.consultarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getIdPedido());
        assertEquals(EstadoPedido.CREADO, resultado.getEstado());
    }

    @Test
    void consultarPorId_CuandoNoExiste_LanzaPedidoNotFoundException() {
        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PedidoNotFoundException.class, () -> pedidoService.consultarPorId(999L));
    }

    @Test
    void listarPorUsuario_RetornaLista() {
        Pedido pedido = buildPedidoConDetalle();

        when(pedidoRepository.findByIdUsuario(1L)).thenReturn(List.of(pedido));

        List<PedidoDTO> resultado = pedidoService.listarPorUsuario(1L);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getIdUsuario());
    }

    @Test
    void listarTodos_RetornaLista() {
        Pedido pedido = buildPedidoConDetalle();

        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));

        List<PedidoDTO> resultado = pedidoService.listarTodos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getIdPedido());
    }

    @Test
    void actualizarEstado_TransicionValida_CREADOaEN_PREPARACION() {
        Pedido pedido = buildPedidoConDetalle();
        pedido.setEstado(EstadoPedido.CREADO);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        PedidoDTO resultado = pedidoService.actualizarEstado(1L, EstadoPedido.EN_PREPARACION);

        assertNotNull(resultado);
        assertEquals(EstadoPedido.EN_PREPARACION, resultado.getEstado());
        verify(pedidoRepository).save(pedido);
        verify(notificacionClient).enviarNotificacion(any(CrearNotificacionDTO.class));
    }

    @Test
    void actualizarEstado_TransicionInvalida_LanzaTransicionEstadoInvalidaException() {
        Pedido pedido = buildPedidoConDetalle();
        pedido.setEstado(EstadoPedido.ENTREGADO);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> pedidoService.actualizarEstado(1L, EstadoPedido.CANCELADO));
    }

    @Test
    void actualizarEstado_CancelDesdeCREADO_LiberaReserva() {
        Pedido pedido = buildPedidoConDetalle();
        pedido.setEstado(EstadoPedido.CREADO);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        PedidoDTO resultado = pedidoService.actualizarEstado(1L, EstadoPedido.CANCELADO);

        assertEquals(EstadoPedido.CANCELADO, resultado.getEstado());
        verify(stockClient).liberarReserva(eq(1L), any(LiberarReservaClientDTO.class));
        verify(notificacionClient).enviarNotificacion(any(CrearNotificacionDTO.class));
    }

    @Test
    void actualizarEstado_CancelDesdeEN_PREPARACION_RestauraStock() {
        Pedido pedido = buildPedidoConDetalle();
        pedido.setEstado(EstadoPedido.EN_PREPARACION);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        PedidoDTO resultado = pedidoService.actualizarEstado(1L, EstadoPedido.CANCELADO);

        assertEquals(EstadoPedido.CANCELADO, resultado.getEstado());
        verify(stockClient).reponerStock(eq(1L), any(ReponerStockClientDTO.class));
        verify(notificacionClient).enviarNotificacion(any(CrearNotificacionDTO.class));
    }

    private Pedido buildPedidoConDetalle() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(1L);
        pedido.setIdUsuario(1L);
        pedido.setEstado(EstadoPedido.CREADO);
        pedido.setTotal(new BigDecimal("99.980"));
        pedido.setFechaCreacion(LocalDateTime.now());
        pedido.setDireccionEntrega("Av. Providencia 123");
        pedido.setCourier("Starken");

        DetallePedido detalle = new DetallePedido();
        detalle.setIdDetalle(1L);
        detalle.setPedido(pedido);
        detalle.setIdVariante(1L);
        detalle.setNombreProducto("SKU001");
        detalle.setMl(100);
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(new BigDecimal("49.990"));

        pedido.setDetalles(List.of(detalle));
        return pedido;
    }
}
