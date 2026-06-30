package com.user_service.controller;

import com.user_service.dto.*;
import com.user_service.exception.ForbiddenException;
import com.user_service.exception.GlobalExceptionHandler;
import com.user_service.service.UsuarioService;
import com.user_service.service.security.AutorizacionUsuarioService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private AutorizacionUsuarioService autorizacionUsuarioService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UsuarioController(usuarioService, autorizacionUsuarioService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UsuarioResponseDTO crearUsuarioResponse() {
        return new UsuarioResponseDTO(1L, "test@email.com", "Test", "123456789", "ACTIVO");
    }

    private UsuarioAutenticadoDTO crearAdminAutenticado() {
        return new UsuarioAutenticadoDTO(1L, "admin@email.com", List.of("ROLE_ADMIN"));
    }

    @Test
    void registrar_DebeRetornar201() throws Exception {
        when(usuarioService.registrarUsuario(any(RegistroUsuarioRequestDTO.class))).thenReturn(crearUsuarioResponse());

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@email.com\",\"nombre\":\"Test\",\"password\":\"Pass1234\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.nombre").value("Test"));
    }

    @Test
    void registrar_CuandoDatosInvalidos_DebeRetornar400() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalido\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarTodos_ConAuthValida_DebeRetornar200() throws Exception {
        UsuarioAutenticadoDTO admin = crearAdminAutenticado();
        when(autorizacionUsuarioService.validarSesion(any())).thenReturn(admin);
        doNothing().when(autorizacionUsuarioService).exigirAdmin(admin);
        when(usuarioService.listarTodos()).thenReturn(List.of(crearUsuarioResponse()));

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idUsuario").value(1))
                .andExpect(jsonPath("$[0].email").value("test@email.com"));
    }

    @Test
    void listarTodos_CuandoForbidden_DebeRetornar403() throws Exception {
        UsuarioAutenticadoDTO user = new UsuarioAutenticadoDTO(2L, "user@email.com", List.of("ROLE_USER"));
        when(autorizacionUsuarioService.validarSesion(any())).thenReturn(user);
        doThrow(new ForbiddenException("No tienes permisos de administrador")).when(autorizacionUsuarioService).exigirAdmin(user);

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenerPorId_CuandoExiste_DebeRetornar200() throws Exception {
        UsuarioAutenticadoDTO admin = crearAdminAutenticado();
        when(autorizacionUsuarioService.validarSesion(any())).thenReturn(admin);
        doNothing().when(autorizacionUsuarioService).exigirMismoUsuarioOAdmin(admin, 1L);
        when(usuarioService.obtenerPorId(1L)).thenReturn(crearUsuarioResponse());

        mockMvc.perform(get("/api/usuarios/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.email").value("test@email.com"));
    }

    @Test
    void obtenerPorId_CuandoNoExiste_DebeRetornar404() throws Exception {
        UsuarioAutenticadoDTO admin = crearAdminAutenticado();
        when(autorizacionUsuarioService.validarSesion(any())).thenReturn(admin);
        doNothing().when(autorizacionUsuarioService).exigirMismoUsuarioOAdmin(admin, 999L);
        when(usuarioService.obtenerPorId(999L)).thenThrow(new IllegalArgumentException("Usuario no encontrado"));

        mockMvc.perform(get("/api/usuarios/999")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_DebeRetornar200() throws Exception {
        UsuarioAutenticadoDTO admin = crearAdminAutenticado();
        when(autorizacionUsuarioService.validarSesion(any())).thenReturn(admin);
        doNothing().when(autorizacionUsuarioService).exigirMismoUsuarioOAdmin(admin, 1L);
        when(usuarioService.actualizar(eq(1L), any(ActualizarUsuarioDTO.class))).thenReturn(crearUsuarioResponse());

        mockMvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Test\"}")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(1));
    }

    @Test
    void eliminar_DebeRetornar204() throws Exception {
        UsuarioAutenticadoDTO admin = crearAdminAutenticado();
        when(autorizacionUsuarioService.validarSesion(any())).thenReturn(admin);
        doNothing().when(autorizacionUsuarioService).exigirAdmin(admin);
        doNothing().when(usuarioService).eliminar(1L);

        mockMvc.perform(delete("/api/usuarios/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_CuandoForbidden_DebeRetornar403() throws Exception {
        UsuarioAutenticadoDTO user = new UsuarioAutenticadoDTO(2L, "user@email.com", List.of("ROLE_USER"));
        when(autorizacionUsuarioService.validarSesion(any())).thenReturn(user);
        doThrow(new ForbiddenException("No tienes permisos de administrador")).when(autorizacionUsuarioService).exigirAdmin(user);

        mockMvc.perform(delete("/api/usuarios/1")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void agregarDireccion_DebeRetornar201() throws Exception {
        UsuarioAutenticadoDTO admin = crearAdminAutenticado();
        when(autorizacionUsuarioService.validarSesion(any())).thenReturn(admin);
        doNothing().when(autorizacionUsuarioService).exigirMismoUsuarioOAdmin(admin, 1L);
        DireccionResponseDTO direccion = new DireccionResponseDTO(1L, "Calle", "123", "Comuna", "Ciudad", "Casa");
        when(usuarioService.agregarDireccion(eq(1L), any(DireccionDTO.class))).thenReturn(direccion);

        mockMvc.perform(post("/api/usuarios/1/direcciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calle\":\"Calle\",\"numero\":\"123\",\"comuna\":\"Comuna\",\"ciudad\":\"Ciudad\",\"tipoAlias\":\"Casa\"}")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idDireccion").value(1))
                .andExpect(jsonPath("$.calle").value("Calle"))
                .andExpect(jsonPath("$.tipoAlias").value("Casa"));
    }

    @Test
    void listarDirecciones_DebeRetornar200() throws Exception {
        UsuarioAutenticadoDTO admin = crearAdminAutenticado();
        when(autorizacionUsuarioService.validarSesion(any())).thenReturn(admin);
        doNothing().when(autorizacionUsuarioService).exigirMismoUsuarioOAdmin(admin, 1L);
        DireccionResponseDTO direccion = new DireccionResponseDTO(1L, "Calle", "123", "Comuna", "Ciudad", "Casa");
        when(usuarioService.listarDirecciones(1L)).thenReturn(List.of(direccion));

        mockMvc.perform(get("/api/usuarios/1/direcciones")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idDireccion").value(1))
                .andExpect(jsonPath("$[0].calle").value("Calle"));
    }

    @Test
    void eliminarDireccion_DebeRetornar204() throws Exception {
        UsuarioAutenticadoDTO admin = crearAdminAutenticado();
        when(autorizacionUsuarioService.validarSesion(any())).thenReturn(admin);
        doNothing().when(autorizacionUsuarioService).exigirMismoUsuarioOAdmin(admin, 1L);
        doNothing().when(usuarioService).eliminarDireccion(1L, 1L);

        mockMvc.perform(delete("/api/usuarios/1/direcciones/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }
}
