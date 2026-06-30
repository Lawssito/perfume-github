package com.ms_carrito.service;

import com.ms_carrito.client.CatalogoClient;
import com.ms_carrito.client.StockClient;
import com.ms_carrito.client.StockResponseDTO;
import com.ms_carrito.client.VarianteResponseDTO;
import com.ms_carrito.dto.*;
import com.ms_carrito.exception.*;
import com.ms_carrito.model.Carrito;
import com.ms_carrito.model.ItemCarrito;
import com.ms_carrito.repository.CarritoRepository;
import com.ms_carrito.repository.ItemCarritoRepository;
import com.ms_carrito.service.impl.CarritoServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarritoServiceImplTest {

    @Mock
    private CarritoRepository carritoRepository;
    @Mock
    private ItemCarritoRepository itemCarritoRepository;
    @Mock
    private StockClient stockClient;
    @Mock
    private CatalogoClient catalogoClient;

    private CarritoServiceImpl carritoService;

    private Carrito carrito;
    private ItemCarrito item;
    private StockResponseDTO stockResponse;
    private VarianteResponseDTO varianteResponse;

    @BeforeEach
    void setUp() {
        carritoService = new CarritoServiceImpl(carritoRepository, itemCarritoRepository, stockClient, catalogoClient);

        carrito = new Carrito();
        carrito.setIdCarrito(1L);
        carrito.setIdUsuario(1L);
        carrito.setCreadoEn(LocalDateTime.now());
        carrito.setItems(new ArrayList<>());

        item = new ItemCarrito();
        item.setIdItem(1L);
        item.setCarrito(carrito);
        item.setIdVariante(1L);
        item.setCantidad(2);
        item.setPrecioUnitario(new BigDecimal("49.990"));

        stockResponse = new StockResponseDTO();
        stockResponse.setIdInventario(1L);
        stockResponse.setIdVariante(1L);
        stockResponse.setCantidadDisponible(10);

        varianteResponse = new VarianteResponseDTO();
        varianteResponse.setIdVariante(1L);
        varianteResponse.setIdPerfume(1L);
        varianteResponse.setSku("SKU001");
        varianteResponse.setMl(100);
        varianteResponse.setPrecio(new BigDecimal("49.990"));
    }

    @Test
    void obtenerOCrearCarrito_CuandoExiste_RetornaCarrito() {
        when(carritoRepository.findByIdUsuario(1L)).thenReturn(Optional.of(carrito));

        CarritoDTO resultado = carritoService.obtenerOCrearCarrito(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getIdCarrito());
        assertEquals(1L, resultado.getIdUsuario());
    }

    @Test
    void obtenerOCrearCarrito_CuandoNoExiste_CreaNuevoCarrito() {
        Carrito nuevoCarrito = new Carrito();
        nuevoCarrito.setIdCarrito(2L);
        nuevoCarrito.setIdUsuario(1L);
        nuevoCarrito.setCreadoEn(LocalDateTime.now());
        nuevoCarrito.setItems(new ArrayList<>());

        when(carritoRepository.findByIdUsuario(1L)).thenReturn(Optional.empty());
        when(carritoRepository.save(any(Carrito.class))).thenReturn(nuevoCarrito);

        CarritoDTO resultado = carritoService.obtenerOCrearCarrito(1L);

        assertNotNull(resultado);
        assertEquals(2L, resultado.getIdCarrito());
        assertEquals(1L, resultado.getIdUsuario());
    }

    @Test
    void agregarItem_CuandoStockSuficienteYCarritoNuevo_AgregaItem() {
        AgregarItemDTO dto = new AgregarItemDTO();
        dto.setIdVariante(1L);
        dto.setCantidad(2);

        Carrito nuevoCarrito = new Carrito();
        nuevoCarrito.setIdCarrito(2L);
        nuevoCarrito.setIdUsuario(1L);
        nuevoCarrito.setCreadoEn(LocalDateTime.now());
        nuevoCarrito.setItems(new ArrayList<>());

        when(stockClient.consultarStock(1L)).thenReturn(stockResponse);
        when(catalogoClient.consultarVariante(1L)).thenReturn(varianteResponse);
        when(carritoRepository.findByIdUsuario(1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(nuevoCarrito));
        when(carritoRepository.save(any(Carrito.class))).thenReturn(nuevoCarrito);
        when(itemCarritoRepository.findByCarrito_IdCarritoAndIdVariante(2L, 1L)).thenReturn(Optional.empty());

        CarritoDTO resultado = carritoService.agregarItem(1L, dto);

        assertNotNull(resultado);
        assertEquals(2L, resultado.getIdCarrito());
        verify(itemCarritoRepository).save(any(ItemCarrito.class));
    }

    @Test
    void agregarItem_CuandoStockSuficienteYItemExistente_ActualizaCantidad() {
        AgregarItemDTO dto = new AgregarItemDTO();
        dto.setIdVariante(1L);
        dto.setCantidad(3);

        carrito.getItems().add(item);

        when(stockClient.consultarStock(1L)).thenReturn(stockResponse);
        when(catalogoClient.consultarVariante(1L)).thenReturn(varianteResponse);
        when(carritoRepository.findByIdUsuario(1L))
                .thenReturn(Optional.of(carrito))
                .thenReturn(Optional.of(carrito));
        when(itemCarritoRepository.findByCarrito_IdCarritoAndIdVariante(1L, 1L))
                .thenReturn(Optional.of(item));

        CarritoDTO resultado = carritoService.agregarItem(1L, dto);

        assertNotNull(resultado);
        verify(itemCarritoRepository).save(item);
    }

    @Test
    void agregarItem_CuandoStockInsuficiente_LanzaStockNoDisponibleException() {
        AgregarItemDTO dto = new AgregarItemDTO();
        dto.setIdVariante(1L);
        dto.setCantidad(20);

        stockResponse.setCantidadDisponible(5);
        when(stockClient.consultarStock(1L)).thenReturn(stockResponse);

        assertThrows(StockNoDisponibleException.class, () -> carritoService.agregarItem(1L, dto));
    }

    @Test
    void agregarItem_CuandoStockNoEncontrado_LanzaStockNoDisponibleException() {
        AgregarItemDTO dto = new AgregarItemDTO();
        dto.setIdVariante(1L);
        dto.setCantidad(2);

        FeignException.NotFound notFound = mock(FeignException.NotFound.class);
        when(stockClient.consultarStock(1L)).thenThrow(notFound);

        assertThrows(StockNoDisponibleException.class, () -> carritoService.agregarItem(1L, dto));
    }

    @Test
    void agregarItem_CuandoServicioStockNoDisponible_LanzaRuntimeException() {
        AgregarItemDTO dto = new AgregarItemDTO();
        dto.setIdVariante(1L);
        dto.setCantidad(2);

        FeignException feignError = mock(FeignException.class);
        when(stockClient.consultarStock(1L)).thenThrow(feignError);

        assertThrows(RuntimeException.class, () -> carritoService.agregarItem(1L, dto));
    }

    @Test
    void actualizarCantidad_CuandoItemExisteYStockSuficiente_Actualiza() {
        ActualizarCantidadDTO dto = new ActualizarCantidadDTO();
        dto.setCantidad(5);

        carrito.getItems().add(item);

        when(carritoRepository.findByIdUsuario(1L)).thenReturn(Optional.of(carrito));
        when(itemCarritoRepository.findById(1L)).thenReturn(Optional.of(item));
        when(stockClient.consultarStock(1L)).thenReturn(stockResponse);
        when(carritoRepository.findByIdUsuario(1L)).thenReturn(Optional.of(carrito));

        CarritoDTO resultado = carritoService.actualizarCantidad(1L, 1L, dto);

        assertNotNull(resultado);
        assertEquals(5, item.getCantidad());
    }

    @Test
    void actualizarCantidad_CuandoItemNoExiste_LanzaItemNotFoundException() {
        ActualizarCantidadDTO dto = new ActualizarCantidadDTO();
        dto.setCantidad(3);

        when(carritoRepository.findByIdUsuario(1L)).thenReturn(Optional.of(carrito));
        when(itemCarritoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ItemNotFoundException.class,
                () -> carritoService.actualizarCantidad(1L, 999L, dto));
    }

    @Test
    void actualizarCantidad_CuandoItemNoPerteneceAlCarrito_LanzaItemNotFoundException() {
        ActualizarCantidadDTO dto = new ActualizarCantidadDTO();
        dto.setCantidad(3);

        Carrito otroCarrito = new Carrito();
        otroCarrito.setIdCarrito(99L);
        otroCarrito.setIdUsuario(1L);

        item.setCarrito(otroCarrito);

        when(carritoRepository.findByIdUsuario(1L)).thenReturn(Optional.of(carrito));
        when(itemCarritoRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(ItemNotFoundException.class,
                () -> carritoService.actualizarCantidad(1L, 1L, dto));
    }

    @Test
    void eliminarItem_CuandoExiste_EliminaYRetornaCarrito() {
        carrito.getItems().add(item);

        when(carritoRepository.findByIdUsuario(1L))
                .thenReturn(Optional.of(carrito))
                .thenReturn(Optional.of(carrito));
        when(itemCarritoRepository.findById(1L)).thenReturn(Optional.of(item));

        CarritoDTO resultado = carritoService.eliminarItem(1L, 1L);

        assertNotNull(resultado);
        verify(itemCarritoRepository).deleteById(1L);
        verify(itemCarritoRepository).flush();
    }

    @Test
    void vaciarCarrito_CuandoExiste_LimpiaItemsYGuarda() {
        carrito.getItems().add(item);

        when(carritoRepository.findByIdUsuario(1L)).thenReturn(Optional.of(carrito));

        carritoService.vaciarCarrito(1L);

        assertTrue(carrito.getItems().isEmpty());
        verify(carritoRepository).save(carrito);
    }

    @Test
    void vaciarCarrito_CuandoNoExiste_LanzaCarritoNotFoundException() {
        when(carritoRepository.findByIdUsuario(1L)).thenReturn(Optional.empty());

        assertThrows(CarritoNotFoundException.class, () -> carritoService.vaciarCarrito(1L));
    }
}
