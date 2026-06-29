package com.auth_service.controller;

import com.auth_service.dto.*;
import com.auth_service.exception.GlobalExceptionHandler;
import com.auth_service.service.AuthService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private CredencialResponseDTO crearCredencialResponse() {
        return new CredencialResponseDTO(1L, 1L, "test@email.com", "ACTIVO", LocalDateTime.now());
    }

    @Test
    void crearCredencial_DebeRetornar201() throws Exception {
        doNothing().when(authService).crearCredencial(any(CrearCredencialRequestDTO.class));

        mockMvc.perform(post("/api/v1/auth/credenciales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":1,\"email\":\"test@email.com\",\"password\":\"Pass1234\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void crearCredencial_CuandoDatosInvalidos_DebeRetornar400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/credenciales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalido\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarCredenciales_DebeRetornar200() throws Exception {
        when(authService.listarCredenciales()).thenReturn(List.of(crearCredencialResponse()));

        mockMvc.perform(get("/api/v1/auth/credenciales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idCredencial").value(1))
                .andExpect(jsonPath("$[0].email").value("test@email.com"))
                .andExpect(jsonPath("$[0].estadoCuenta").value("ACTIVO"));
    }

    @Test
    void obtenerPorIdUsuario_CuandoExiste_DebeRetornar200() throws Exception {
        when(authService.obtenerPorIdUsuario(1L)).thenReturn(crearCredencialResponse());

        mockMvc.perform(get("/api/v1/auth/credenciales/usuario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCredencial").value(1))
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.estadoCuenta").value("ACTIVO"));
    }

    @Test
    void obtenerPorIdUsuario_CuandoNoExiste_DebeRetornar404() throws Exception {
        when(authService.obtenerPorIdUsuario(999L)).thenThrow(new IllegalArgumentException("Credencial no encontrada para el usuario"));

        mockMvc.perform(get("/api/v1/auth/credenciales/usuario/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarEstado_DebeRetornar200() throws Exception {
        CredencialResponseDTO actualizada = new CredencialResponseDTO(1L, 1L, "test@email.com", "INACTIVO", LocalDateTime.now());
        when(authService.actualizarEstadoCuenta(eq(1L), any(ActualizarEstadoCuentaDTO.class))).thenReturn(actualizada);

        mockMvc.perform(put("/api/v1/auth/credenciales/usuario/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estadoCuenta\":\"INACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCuenta").value("INACTIVO"));
    }

    @Test
    void eliminarCredencial_DebeRetornar204() throws Exception {
        doNothing().when(authService).eliminarCredencial(1L);

        mockMvc.perform(delete("/api/v1/auth/credenciales/usuario/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void login_ConCredencialesValidas_DebeRetornar200() throws Exception {
        AuthResponseDTO authResponse = new AuthResponseDTO();
        authResponse.setToken("jwt-token");
        authResponse.setRefreshToken("refresh-token");
        authResponse.setIdUsuario(1L);
        authResponse.setEmail("test@email.com");
        authResponse.setRoles(List.of("ROLE_USER"));
        authResponse.setMensaje("Autenticacion exitosa");

        when(authService.autenticarUsuario(any(LoginRequestDTO.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@email.com\",\"password\":\"Pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.mensaje").value("Autenticacion exitosa"));
    }

    @Test
    void validate_CuandoTokenValido_DebeRetornar200() throws Exception {
        TokenClaimsResponseDTO claims = new TokenClaimsResponseDTO(true, "test@email.com", 1L, List.of("ROLE_USER"), "Token valido");
        when(authService.validarToken(any(ValidateTokenRequestDTO.class))).thenReturn(claims);

        mockMvc.perform(post("/api/v1/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"valid-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void validate_CuandoTokenInvalido_DebeRetornar401() throws Exception {
        TokenClaimsResponseDTO claims = new TokenClaimsResponseDTO(false, null, null, null, "Token invalido o expirado");
        when(authService.validarToken(any(ValidateTokenRequestDTO.class))).thenReturn(claims);

        mockMvc.perform(post("/api/v1/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"invalid-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valido").value(false))
                .andExpect(jsonPath("$.mensaje").value("Token invalido o expirado"));
    }

    @Test
    void refresh_DebeRetornar200() throws Exception {
        AuthResponseDTO authResponse = new AuthResponseDTO();
        authResponse.setToken("new-jwt-token");
        authResponse.setRefreshToken("new-refresh-token");
        authResponse.setIdUsuario(1L);
        authResponse.setEmail("test@email.com");
        authResponse.setRoles(List.of("ROLE_USER"));
        authResponse.setMensaje("Token renovado exitosamente");

        when(authService.refreshToken("valid-refresh-token")).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"valid-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.mensaje").value("Token renovado exitosamente"));
    }
}
