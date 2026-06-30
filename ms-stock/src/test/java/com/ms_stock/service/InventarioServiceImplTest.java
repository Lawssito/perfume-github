package com.ms_stock.service;

import com.ms_stock.dto.*;
import com.ms_stock.exception.*;
import com.ms_stock.model.IdempotenciaKey;
import com.ms_stock.model.Inventario;
import com.ms_stock.repository.IdempotenciaKeyRepository;
import com.ms_stock.repository.InventarioRepository;
import com.ms_stock.service.impl.InventarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventarioServiceImplTest {

    @Mock
    private InventarioRepository inventarioRepository;
    @Mock
    private IdempotenciaKeyRepository idempotenciaKeyRepository;

    private InventarioServiceImpl inventarioService;

    private Inventario inventario;

    @BeforeEach
    void setUp() {
        inventarioService = new InventarioServiceImpl(inventarioRepository, idempotenciaKeyRepository);

        inventario = new Inventario();
        inventario.setIdInventario(1L);
        inventario.setIdVariante(1L);
        inventario.setCantidadDisponible(50);
        inventario.setCantidadReservada(10);
    }

    @Test
    void consultarPorVariante_CuandoExiste_RetornaInventarioDTO() {
        when(inventarioRepository.findByIdVariante(1L)).thenReturn(Optional.of(inventario));

        InventarioDTO resultado = inventarioService.consultarPorVariante(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getIdVariante());
        assertEquals(50, resultado.getCantidadDisponible());
        assertEquals(10, resultado.getCantidadReservada());
    }

    @Test
    void consultarPorVariante_CuandoNoExiste_LanzaVarianteNotFoundException() {
        when(inventarioRepository.findByIdVariante(999L)).thenReturn(Optional.empty());

        assertThrows(VarianteNotFoundException.class, () -> inventarioService.consultarPorVariante(999L));
    }

    @Test
    void listarTodo_RetornaLista() {
        when(inventarioRepository.findAll()).thenReturn(List.of(inventario));

        List<InventarioDTO> resultado = inventarioService.listarTodo();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getIdVariante());
    }

    @Test
    void crearInventario_CuandoNoExiste_CreaYRetorna() {
        when(inventarioRepository.findByIdVariante(2L)).thenReturn(Optional.empty());

        Inventario nuevo = new Inventario();
        nuevo.setIdInventario(2L);
        nuevo.setIdVariante(2L);
        nuevo.setCantidadDisponible(0);
        nuevo.setCantidadReservada(0);
        when(inventarioRepository.save(any(Inventario.class))).thenReturn(nuevo);

        InventarioDTO resultado = inventarioService.crearInventario(2L);

        assertNotNull(resultado);
        assertEquals(2L, resultado.getIdVariante());
        assertEquals(0, resultado.getCantidadDisponible());
        assertEquals(0, resultado.getCantidadReservada());
    }

    @Test
    void crearInventario_CuandoYaExiste_LanzaIllegalStateException() {
        when(inventarioRepository.findByIdVariante(1L)).thenReturn(Optional.of(inventario));

        assertThrows(IllegalStateException.class, () -> inventarioService.crearInventario(1L));
    }

    @Test
    void reponerStock_SumaCantidad() {
        ReponerStockDTO dto = new ReponerStockDTO();
        dto.setCantidad(20);

        when(inventarioRepository.findByIdVariante(1L)).thenReturn(Optional.of(inventario));

        Inventario actualizado = new Inventario();
        actualizado.setIdInventario(1L);
        actualizado.setIdVariante(1L);
        actualizado.setCantidadDisponible(70);
        actualizado.setCantidadReservada(10);
        when(inventarioRepository.save(any(Inventario.class))).thenReturn(actualizado);

        InventarioDTO resultado = inventarioService.reponerStock(1L, dto);

        assertNotNull(resultado);
        assertEquals(70, resultado.getCantidadDisponible());
        assertEquals(10, resultado.getCantidadReservada());
        verify(inventarioRepository).save(inventario);
    }

    @Test
    void reducirStock_CuandoValido_ReduceYRegistraIdempotencia() {
        ReducirStockDTO dto = new ReducirStockDTO();
        dto.setCantidad(10);
        dto.setIdempotencyKey("KEY_REDUCIR_001");

        when(idempotenciaKeyRepository.existsByIdempotencyKey("KEY_REDUCIR_001")).thenReturn(false);
        when(inventarioRepository.findByIdVariante(1L)).thenReturn(Optional.of(inventario));

        Inventario actualizado = new Inventario();
        actualizado.setIdInventario(1L);
        actualizado.setIdVariante(1L);
        actualizado.setCantidadDisponible(40);
        actualizado.setCantidadReservada(10);
        when(inventarioRepository.save(any(Inventario.class))).thenReturn(actualizado);

        InventarioDTO resultado = inventarioService.reducirStock(1L, dto);

        assertEquals(40, resultado.getCantidadDisponible());
        verify(idempotenciaKeyRepository).save(any(IdempotenciaKey.class));
    }

    @Test
    void reducirStock_CuandoIdempotente_RetornaEstadoActual() {
        ReducirStockDTO dto = new ReducirStockDTO();
        dto.setCantidad(10);
        dto.setIdempotencyKey("KEY_REDUCIR_001");

        when(idempotenciaKeyRepository.existsByIdempotencyKey("KEY_REDUCIR_001")).thenReturn(true);
        when(inventarioRepository.findByIdVariante(1L)).thenReturn(Optional.of(inventario));

        InventarioDTO resultado = inventarioService.reducirStock(1L, dto);

        assertEquals(50, resultado.getCantidadDisponible());
        verify(inventarioRepository, never()).save(any());
    }

    @Test
    void reducirStock_CuandoStockInsuficiente_LanzaStockInsuficienteException() {
        ReducirStockDTO dto = new ReducirStockDTO();
        dto.setCantidad(100);
        dto.setIdempotencyKey("KEY_REDUCIR_002");

        when(idempotenciaKeyRepository.existsByIdempotencyKey("KEY_REDUCIR_002")).thenReturn(false);
        when(inventarioRepository.findByIdVariante(1L)).thenReturn(Optional.of(inventario));

        assertThrows(StockInsuficienteException.class, () -> inventarioService.reducirStock(1L, dto));
    }

    @Test
    void reservarStock_CuandoValido_ReservaYReduceDisponible() {
        ReservarStockDTO dto = new ReservarStockDTO();
        dto.setCantidad(5);
        dto.setIdempotencyKey("KEY_RESERVA_001");

        when(idempotenciaKeyRepository.existsByIdempotencyKey("KEY_RESERVA_001")).thenReturn(false);
        when(inventarioRepository.findByIdVariante(1L)).thenReturn(Optional.of(inventario));

        Inventario actualizado = new Inventario();
        actualizado.setIdInventario(1L);
        actualizado.setIdVariante(1L);
        actualizado.setCantidadDisponible(45);
        actualizado.setCantidadReservada(15);
        when(inventarioRepository.save(any(Inventario.class))).thenReturn(actualizado);

        InventarioDTO resultado = inventarioService.reservarStock(1L, dto);

        assertEquals(45, resultado.getCantidadDisponible());
        assertEquals(15, resultado.getCantidadReservada());
        verify(idempotenciaKeyRepository).save(any(IdempotenciaKey.class));
    }

    @Test
    void reservarStock_CuandoStockInsuficiente_LanzaStockInsuficienteException() {
        ReservarStockDTO dto = new ReservarStockDTO();
        dto.setCantidad(100);
        dto.setIdempotencyKey("KEY_RESERVA_002");

        when(idempotenciaKeyRepository.existsByIdempotencyKey("KEY_RESERVA_002")).thenReturn(false);
        when(inventarioRepository.findByIdVariante(1L)).thenReturn(Optional.of(inventario));

        assertThrows(StockInsuficienteException.class, () -> inventarioService.reservarStock(1L, dto));
    }

    @Test
    void reservarStock_CuandoIdempotente_RetornaEstadoActual() {
        ReservarStockDTO dto = new ReservarStockDTO();
        dto.setCantidad(5);
        dto.setIdempotencyKey("KEY_RESERVA_001");

        when(idempotenciaKeyRepository.existsByIdempotencyKey("KEY_RESERVA_001")).thenReturn(true);
        when(inventarioRepository.findByIdVariante(1L)).thenReturn(Optional.of(inventario));

        InventarioDTO resultado = inventarioService.reservarStock(1L, dto);

        assertEquals(50, resultado.getCantidadDisponible());
        assertEquals(10, resultado.getCantidadReservada());
        verify(inventarioRepository, never()).save(any());
    }

    @Test
    void confirmarReserva_CuandoValido_ReduceReservada() {
        ConfirmarReservaDTO dto = new ConfirmarReservaDTO();
        dto.setCantidad(5);
        dto.setIdempotencyKey("KEY_CONFIRMAR_001");

        when(idempotenciaKeyRepository.existsByIdempotencyKey("KEY_CONFIRMAR_001")).thenReturn(false);
        when(inventarioRepository.findByIdVariante(1L)).thenReturn(Optional.of(inventario));

        Inventario actualizado = new Inventario();
        actualizado.setIdInventario(1L);
        actualizado.setIdVariante(1L);
        actualizado.setCantidadDisponible(50);
        actualizado.setCantidadReservada(5);
        when(inventarioRepository.save(any(Inventario.class))).thenReturn(actualizado);

        InventarioDTO resultado = inventarioService.confirmarReserva(1L, dto);

        assertEquals(5, resultado.getCantidadReservada());
        verify(idempotenciaKeyRepository).save(any(IdempotenciaKey.class));
    }

    @Test
    void confirmarReserva_CuandoReservadaInsuficiente_LanzaStockInsuficienteException() {
        ConfirmarReservaDTO dto = new ConfirmarReservaDTO();
        dto.setCantidad(100);
        dto.setIdempotencyKey("KEY_CONFIRMAR_002");

        when(idempotenciaKeyRepository.existsByIdempotencyKey("KEY_CONFIRMAR_002")).thenReturn(false);
        when(inventarioRepository.findByIdVariante(1L)).thenReturn(Optional.of(inventario));

        assertThrows(StockInsuficienteException.class, () -> inventarioService.confirmarReserva(1L, dto));
    }

    @Test
    void liberarReserva_ReduceReservadaYSumaDisponible() {
        LiberarReservaDTO dto = new LiberarReservaDTO();
        dto.setCantidad(5);
        dto.setIdempotencyKey("KEY_LIBERAR_001");

        when(idempotenciaKeyRepository.existsByIdempotencyKey("KEY_LIBERAR_001")).thenReturn(false);
        when(inventarioRepository.findByIdVariante(1L)).thenReturn(Optional.of(inventario));

        Inventario actualizado = new Inventario();
        actualizado.setIdInventario(1L);
        actualizado.setIdVariante(1L);
        actualizado.setCantidadDisponible(55);
        actualizado.setCantidadReservada(5);
        when(inventarioRepository.save(any(Inventario.class))).thenReturn(actualizado);

        InventarioDTO resultado = inventarioService.liberarReserva(1L, dto);

        assertEquals(55, resultado.getCantidadDisponible());
        assertEquals(5, resultado.getCantidadReservada());
        verify(idempotenciaKeyRepository).save(any(IdempotenciaKey.class));
    }

    @Test
    void eliminarInventario_CuandoExiste_Elimina() {
        when(inventarioRepository.findByIdVariante(1L)).thenReturn(Optional.of(inventario));

        inventarioService.eliminarInventario(1L);

        verify(inventarioRepository).delete(inventario);
    }

    @Test
    void eliminarInventario_CuandoNoExiste_LanzaVarianteNotFoundException() {
        when(inventarioRepository.findByIdVariante(999L)).thenReturn(Optional.empty());

        assertThrows(VarianteNotFoundException.class, () -> inventarioService.eliminarInventario(999L));
    }
}
