package com.ms_carrito.controller;

import com.ms_carrito.dto.*;
import com.ms_carrito.exception.GlobalExceptionHandler;
import com.ms_carrito.service.CarritoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CarritoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CarritoService carritoService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CarritoController(carritoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private CarritoDTO crearCarritoDTO() {
        ItemCarritoDTO item = new ItemCarritoDTO(1L, 1L, 2, new BigDecimal("49.990"), new BigDecimal("99.980"));
        List<ItemCarritoDTO> items = new ArrayList<>();
        items.add(item);
        return new CarritoDTO(1L, 1L, LocalDateTime.now(), items, new BigDecimal("99.980"));
    }

    @Test
    void obtenerMiCarrito_ConUsuarioAutenticado_DebeRetornar200() throws Exception {
        when(carritoService.obtenerOCrearCarrito(1L)).thenReturn(crearCarritoDTO());

        mockMvc.perform(get("/api/carrito/mi-carrito")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCarrito").value(1))
                .andExpect(jsonPath("$.idUsuario").value(1));
    }

    @Test
    void obtenerMiCarrito_SinHeaderXUserId_DebeRetornar403() throws Exception {
        mockMvc.perform(get("/api/carrito/mi-carrito"))
                .andExpect(status().isForbidden());
    }

    @Test
    void agregarItem_ConUsuarioAutenticado_DebeRetornar200() throws Exception {
        when(carritoService.agregarItem(eq(1L), any(AgregarItemDTO.class))).thenReturn(crearCarritoDTO());

        mockMvc.perform(post("/api/carrito/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVariante\":1,\"cantidad\":2}")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCarrito").value(1));
    }

    @Test
    void agregarItem_SinHeaderXUserId_DebeRetornar403() throws Exception {
        mockMvc.perform(post("/api/carrito/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVariante\":1,\"cantidad\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarCantidad_ConUsuarioAutenticado_DebeRetornar200() throws Exception {
        when(carritoService.actualizarCantidad(eq(1L), eq(1L), any(ActualizarCantidadDTO.class)))
                .thenReturn(crearCarritoDTO());

        mockMvc.perform(put("/api/carrito/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":3}")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCarrito").value(1));
    }

    @Test
    void eliminarItem_ConUsuarioAutenticado_DebeRetornar200() throws Exception {
        CarritoDTO carritoVacio = new CarritoDTO(1L, 1L, LocalDateTime.now(), new ArrayList<>(), BigDecimal.ZERO);
        when(carritoService.eliminarItem(1L, 1L)).thenReturn(carritoVacio);

        mockMvc.perform(delete("/api/carrito/items/1")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void vaciarCarrito_ConUsuarioAutenticado_DebeRetornar204() throws Exception {
        doNothing().when(carritoService).vaciarCarrito(1L);

        mockMvc.perform(delete("/api/carrito")
                        .header("X-User-Id", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void agregarItem_ConDatosInvalidos_DebeRetornar400() throws Exception {
        mockMvc.perform(post("/api/carrito/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVariante\":null,\"cantidad\":-1}")
                        .header("X-User-Id", "1"))
                .andExpect(status().isBadRequest());
    }
}
