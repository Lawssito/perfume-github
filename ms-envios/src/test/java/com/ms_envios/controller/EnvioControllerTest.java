package com.ms_envios.controller;

import com.ms_envios.dto.*;
import com.ms_envios.exception.GlobalExceptionHandler;
import com.ms_envios.exception.EnvioNotFoundException;
import com.ms_envios.model.EstadoEnvio;
import com.ms_envios.service.EnvioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EnvioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EnvioService envioService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EnvioController(envioService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private EnvioDTO crearEnvioDTO() {
        return new EnvioDTO(1L, 1L, "Calle 123", EstadoEnvio.PENDIENTE, "TRK-ABC123", "DHL", LocalDateTime.now(), LocalDate.now().plusDays(5), null);
    }

    @Test
    void crearEnvio_DebeRetornar201() throws Exception {
        when(envioService.crearEnvio(any(CrearEnvioDTO.class))).thenReturn(crearEnvioDTO());

        mockMvc.perform(post("/api/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPedido\":1,\"direccionDestino\":\"Calle 123\",\"courier\":\"DHL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idEnvio").value(1))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void crearEnvio_ConDatosInvalidos_DebeRetornar400() throws Exception {
        mockMvc.perform(post("/api/envios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idPedido\":null,\"direccionDestino\":\"\",\"courier\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarTodos_ConRolAdmin_DebeRetornar200() throws Exception {
        when(envioService.listarTodos()).thenReturn(List.of(crearEnvioDTO()));

        mockMvc.perform(get("/api/envios")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idEnvio").value(1));
    }

    @Test
    void listarTodos_SinRolAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(get("/api/envios")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void consultarPorId_ConRolAdmin_DebeRetornar200() throws Exception {
        when(envioService.consultarPorId(1L)).thenReturn(crearEnvioDTO());

        mockMvc.perform(get("/api/envios/1")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEnvio").value(1));
    }

    @Test
    void consultarPorId_ConRolAdmin_NoEncontrado_DebeRetornar404() throws Exception {
        when(envioService.consultarPorId(999L)).thenThrow(new EnvioNotFoundException(999L));

        mockMvc.perform(get("/api/envios/999")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultarPorId_SinRolAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(get("/api/envios/1")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void consultarPorPedido_ConRolAdmin_DebeRetornar200() throws Exception {
        when(envioService.consultarPorPedido(1L)).thenReturn(crearEnvioDTO());

        mockMvc.perform(get("/api/envios/pedido/1")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPedido").value(1));
    }

    @Test
    void consultarPorPedido_ConRolAdmin_NoEncontrado_DebeRetornar404() throws Exception {
        when(envioService.consultarPorPedido(999L)).thenThrow(new EnvioNotFoundException(999L));

        mockMvc.perform(get("/api/envios/pedido/999")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarPorEstado_ConRolAdmin_DebeRetornar200() throws Exception {
        when(envioService.listarPorEstado(EstadoEnvio.PENDIENTE)).thenReturn(List.of(crearEnvioDTO()));

        mockMvc.perform(get("/api/envios/estado/PENDIENTE")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    @Test
    void avanzarEstado_ConRolAdmin_DebeRetornar200() throws Exception {
        EnvioDTO avanzado = crearEnvioDTO();
        avanzado.setEstado(EstadoEnvio.EN_PREPARACION);
        when(envioService.avanzarEstado(eq(1L), any(AvanzarEstadoDTO.class))).thenReturn(avanzado);

        mockMvc.perform(put("/api/envios/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PREPARACION\"}")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_PREPARACION"));
    }

    @Test
    void cancelarEnvio_ConRolAdmin_DebeRetornar200() throws Exception {
        EnvioDTO cancelado = crearEnvioDTO();
        cancelado.setEstado(EstadoEnvio.CANCELADO);
        when(envioService.cancelarEnvio(1L)).thenReturn(cancelado);

        mockMvc.perform(post("/api/envios/1/cancelar")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }
}
