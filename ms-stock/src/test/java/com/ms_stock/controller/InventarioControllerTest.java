package com.ms_stock.controller;

import com.ms_stock.dto.*;
import com.ms_stock.exception.GlobalExceptionHandler;
import com.ms_stock.exception.StockInsuficienteException;
import com.ms_stock.exception.VarianteNotFoundException;
import com.ms_stock.service.InventarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InventarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InventarioService inventarioService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InventarioController(inventarioService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private InventarioDTO crearInventarioDTO() {
        return new InventarioDTO(1L, 1L, 50, 0);
    }

    @Test
    void listarTodo_DebeRetornar200() throws Exception {
        when(inventarioService.listarTodo()).thenReturn(List.of(crearInventarioDTO()));

        mockMvc.perform(get("/api/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idVariante").value(1))
                .andExpect(jsonPath("$[0].cantidadDisponible").value(50));
    }

    @Test
    void consultarPorVariante_CuandoExiste_DebeRetornar200() throws Exception {
        when(inventarioService.consultarPorVariante(1L)).thenReturn(crearInventarioDTO());

        mockMvc.perform(get("/api/stock/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDisponible").value(50));
    }

    @Test
    void consultarPorVariante_CuandoNoExiste_DebeRetornar404() throws Exception {
        when(inventarioService.consultarPorVariante(999L))
                .thenThrow(new VarianteNotFoundException(999L));

        mockMvc.perform(get("/api/stock/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearInventario_ConRolAdmin_DebeRetornar201() throws Exception {
        when(inventarioService.crearInventario(1L)).thenReturn(crearInventarioDTO());

        mockMvc.perform(post("/api/stock/1")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idVariante").value(1));
    }

    @Test
    void crearInventario_SinRolAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(post("/api/stock/1")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reponerStock_ConRolAdmin_DebeRetornar200() throws Exception {
        InventarioDTO repuesto = new InventarioDTO(1L, 1L, 100, 0);
        when(inventarioService.reponerStock(eq(1L), any(ReponerStockDTO.class))).thenReturn(repuesto);

        mockMvc.perform(put("/api/stock/1/reponer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":50}")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDisponible").value(100));
    }

    @Test
    void reducirStock_ConRolAdmin_DebeRetornar200() throws Exception {
        InventarioDTO reducido = new InventarioDTO(1L, 1L, 45, 0);
        when(inventarioService.reducirStock(eq(1L), any(ReducirStockDTO.class))).thenReturn(reducido);

        mockMvc.perform(put("/api/stock/1/reducir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":5,\"idempotencyKey\":\"key-001\"}")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDisponible").value(45));
    }

    @Test
    void reducirStock_StockInsuficiente_DebeRetornar409() throws Exception {
        when(inventarioService.reducirStock(eq(1L), any(ReducirStockDTO.class)))
                .thenThrow(new StockInsuficienteException(1L, 999, 50));

        mockMvc.perform(put("/api/stock/1/reducir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":999,\"idempotencyKey\":\"key-002\"}")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminarInventario_ConRolAdmin_DebeRetornar204() throws Exception {
        doNothing().when(inventarioService).eliminarInventario(1L);

        mockMvc.perform(delete("/api/stock/1")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    void reponerStock_SinRolAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(put("/api/stock/1/reponer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":10}")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearInventario_SinHeaderRol_DebeRetornar201_PorSerFeign() throws Exception {
        when(inventarioService.crearInventario(1L)).thenReturn(crearInventarioDTO());

        mockMvc.perform(post("/api/stock/1"))
                .andExpect(status().isCreated());
    }
}
