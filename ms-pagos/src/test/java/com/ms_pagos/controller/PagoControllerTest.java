package com.ms_pagos.controller;

import com.ms_pagos.dto.*;
import com.ms_pagos.exception.GlobalExceptionHandler;
import com.ms_pagos.exception.PagoNotFoundException;
import com.ms_pagos.model.EstadoPago;
import com.ms_pagos.model.MetodoPago;
import com.ms_pagos.service.PagoService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PagoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PagoService pagoService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PagoController(pagoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PagoDTO crearPagoDTO() {
        return new PagoDTO(1L, 1L, new BigDecimal("99.980"), MetodoPago.TARJETA,
                EstadoPago.PENDIENTE, "REF-001", LocalDateTime.now(), null);
    }

    @Test
    void crearPago_DebeRetornar201() throws Exception {
        when(pagoService.crearPago(any(CrearPagoDTO.class))).thenReturn(crearPagoDTO());

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPedido\":1,\"montoTotal\":99.98,\"metodoPago\":\"TARJETA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idTransaccion").value(1))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void consultarPorId_CuandoExiste_DebeRetornar200() throws Exception {
        when(pagoService.consultarPorId(1L)).thenReturn(crearPagoDTO());

        mockMvc.perform(get("/api/pagos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idTransaccion").value(1));
    }

    @Test
    void consultarPorId_CuandoNoExiste_DebeRetornar404() throws Exception {
        when(pagoService.consultarPorId(999L)).thenThrow(new PagoNotFoundException(999L));

        mockMvc.perform(get("/api/pagos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultarPorPedido_CuandoExiste_DebeRetornar200() throws Exception {
        when(pagoService.consultarPorPedido(1L)).thenReturn(crearPagoDTO());

        mockMvc.perform(get("/api/pagos/pedido/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPedido").value(1));
    }

    @Test
    void listarTodos_SinRolAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(get("/api/pagos")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarTodos_ConRolAdmin_DebeRetornar200() throws Exception {
        when(pagoService.listarTodos()).thenReturn(List.of(crearPagoDTO()));

        mockMvc.perform(get("/api/pagos")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idTransaccion").value(1));
    }

    @Test
    void procesarPago_ConRolAdmin_DebeRetornar200() throws Exception {
        PagoDTO procesado = crearPagoDTO();
        procesado.setEstado(EstadoPago.COMPLETADO);
        procesado.setProcesadoEn(LocalDateTime.now());
        when(pagoService.procesarPago(1L)).thenReturn(procesado);

        mockMvc.perform(post("/api/pagos/1/procesar")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));
    }

    @Test
    void procesarPago_SinRolAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(post("/api/pagos/1/procesar")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anularPago_ConRolAdmin_DebeRetornar200() throws Exception {
        PagoDTO anulado = crearPagoDTO();
        anulado.setEstado(EstadoPago.ANULADO);
        when(pagoService.anularPago(1L)).thenReturn(anulado);

        mockMvc.perform(post("/api/pagos/1/anular")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ANULADO"));
    }

    @Test
    void reintentarPago_ConRolAdmin_DebeRetornar200() throws Exception {
        PagoDTO reintentado = crearPagoDTO();
        reintentado.setEstado(EstadoPago.PENDIENTE);
        when(pagoService.reintentarPago(1L)).thenReturn(reintentado);

        mockMvc.perform(post("/api/pagos/1/reintentar")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void crearPago_ConDatosInvalidos_DebeRetornar400() throws Exception {
        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPedido\":null,\"montoTotal\":-1,\"metodoPago\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
