package com.ms_notificaciones.controller;

import com.ms_notificaciones.dto.*;
import com.ms_notificaciones.exception.GlobalExceptionHandler;
import com.ms_notificaciones.service.NotificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificacionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificacionService notificacionService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificacionController(notificacionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private NotificacionResponseDTO crearNotificacionDTO() {
        return new NotificacionResponseDTO(1L, 1L, "Su pedido #123 ha sido confirmado", "ENVIADO", LocalDateTime.now());
    }

    @Test
    void enviarNotificacion_DebeRetornar201() throws Exception {
        when(notificacionService.procesarEventoYEnviar(any(EventoNotificacionDTO.class))).thenReturn(crearNotificacionDTO());

        mockMvc.perform(post("/api/notificaciones/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":1,\"tipoEvento\":\"PEDIDO_CREADO\",\"mensaje\":\"Su pedido #123 ha sido confirmado\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idNotificacion").value(1))
                .andExpect(jsonPath("$.mensaje").value("Su pedido #123 ha sido confirmado"));
    }

    @Test
    void enviarNotificacion_ConDatosInvalidos_DebeRetornar400() throws Exception {
        mockMvc.perform(post("/api/notificaciones/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":null,\"tipoEvento\":\"\",\"mensaje\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarTodas_ConRolAdmin_DebeRetornar200() throws Exception {
        when(notificacionService.listarTodas()).thenReturn(List.of(crearNotificacionDTO()));

        mockMvc.perform(get("/api/notificaciones")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idNotificacion").value(1));
    }

    @Test
    void listarTodas_SinRolAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(get("/api/notificaciones")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenerPorId_ConRolAdmin_DebeRetornar200() throws Exception {
        when(notificacionService.obtenerPorId(1L)).thenReturn(crearNotificacionDTO());

        mockMvc.perform(get("/api/notificaciones/1")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idNotificacion").value(1));
    }

    @Test
    void obtenerPorId_ConRolAdmin_NoEncontrado_DebeRetornar404() throws Exception {
        when(notificacionService.obtenerPorId(999L)).thenThrow(new IllegalArgumentException("No existe notificacion con ID: 999"));

        mockMvc.perform(get("/api/notificaciones/999")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarMisNotificaciones_ConUsuarioAutenticado_DebeRetornar200() throws Exception {
        when(notificacionService.listarPorUsuario(1L)).thenReturn(List.of(crearNotificacionDTO()));

        mockMvc.perform(get("/api/notificaciones/mis-notificaciones")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idNotificacion").value(1));
    }

    @Test
    void listarMisNotificaciones_SinUsuarioAutenticado_DebeRetornar403() throws Exception {
        mockMvc.perform(get("/api/notificaciones/mis-notificaciones"))
                .andExpect(status().isForbidden());
    }

    @Test
    void eliminar_ConRolAdmin_DebeRetornar204() throws Exception {
        mockMvc.perform(delete("/api/notificaciones/1")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_SinRolAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(delete("/api/notificaciones/1")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }
}
