package com.security_service.controller;

import com.security_service.dto.*;
import com.security_service.exception.GlobalExceptionHandler;
import com.security_service.service.SecurityService;
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
class SecurityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SecurityController(securityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private RolesUsuarioResponseDTO crearRolesResponse() {
        return new RolesUsuarioResponseDTO(1L, List.of("ROLE_USER"));
    }

    private RolResponseDTO crearRolResponse() {
        return new RolResponseDTO(1L, "ROLE_USER", List.of("ACCESS_BASIC"));
    }

    private PermisoResponseDTO crearPermisoResponse() {
        return new PermisoResponseDTO(1L, "ACCESS_BASIC");
    }

    @Test
    void asignarRol_ComoAdmin_DebeRetornar201() throws Exception {
        when(securityService.asignarRol(any(AsignarRolRequestDTO.class))).thenReturn(crearRolesResponse());

        mockMvc.perform(post("/api/usuario-roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":1,\"rolNombre\":\"ROLE_USER\"}")
                        .header("X-Internal-Api-Key", "api-key"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void asignarRol_SinAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(post("/api/usuario-roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":1,\"rolNombre\":\"ROLE_USER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenerRoles_ComoAdmin_DebeRetornar200() throws Exception {
        when(securityService.obtenerRoles(1L)).thenReturn(crearRolesResponse());

        mockMvc.perform(get("/api/usuario-roles/1")
                        .header("X-Internal-Api-Key", "api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(1))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void revocarRol_ComoAdmin_DebeRetornar204() throws Exception {
        doNothing().when(securityService).revocarRol(1L, "ROLE_USER");

        mockMvc.perform(delete("/api/usuario-roles/1/ROLE_USER")
                        .header("X-Internal-Api-Key", "api-key"))
                .andExpect(status().isNoContent());
    }

    @Test
    void validarPermiso_CuandoValido_DebeRetornar200() throws Exception {
        ValidacionResponseDTO validacion = new ValidacionResponseDTO();
        validacion.setValido(true);
        validacion.setMensaje("Acceso permitido");
        validacion.setIdUsuario(1L);
        when(securityService.validarPermiso(any(ValidarAccesoRequestDTO.class))).thenReturn(validacion);

        mockMvc.perform(post("/api/security/validar-permiso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":1,\"permisoRequerido\":\"ACCESS_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true))
                .andExpect(jsonPath("$.mensaje").value("Acceso permitido"))
                .andExpect(jsonPath("$.idUsuario").value(1));
    }

    @Test
    void validarPermiso_CuandoNoValido_DebeRetornar403() throws Exception {
        ValidacionResponseDTO validacion = new ValidacionResponseDTO();
        validacion.setValido(false);
        validacion.setMensaje("Acceso denegado");
        validacion.setIdUsuario(1L);
        when(securityService.validarPermiso(any(ValidarAccesoRequestDTO.class))).thenReturn(validacion);

        mockMvc.perform(post("/api/security/validar-permiso")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":1,\"permisoRequerido\":\"ACCESS_ADMIN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.valido").value(false))
                .andExpect(jsonPath("$.mensaje").value("Acceso denegado"));
    }

    @Test
    void listarRoles_ComoAdmin_DebeRetornar200() throws Exception {
        when(securityService.listarRoles()).thenReturn(List.of(crearRolResponse()));

        mockMvc.perform(get("/api/security/roles")
                        .header("X-Internal-Api-Key", "api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idRol").value(1))
                .andExpect(jsonPath("$[0].nombre").value("ROLE_USER"))
                .andExpect(jsonPath("$[0].permisos[0]").value("ACCESS_BASIC"));
    }

    @Test
    void listarRoles_SinAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(get("/api/security/roles"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearRol_ComoAdmin_DebeRetornar201() throws Exception {
        when(securityService.crearRol(any(RolRequestDTO.class))).thenReturn(crearRolResponse());

        mockMvc.perform(post("/api/security/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"ROLE_USER\"}")
                        .header("X-Internal-Api-Key", "api-key"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idRol").value(1))
                .andExpect(jsonPath("$.nombre").value("ROLE_USER"));
    }

    @Test
    void obtenerRol_ComoAdmin_CuandoExiste_DebeRetornar200() throws Exception {
        when(securityService.obtenerRol(1L)).thenReturn(crearRolResponse());

        mockMvc.perform(get("/api/security/roles/1")
                        .header("X-Internal-Api-Key", "api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idRol").value(1))
                .andExpect(jsonPath("$.nombre").value("ROLE_USER"));
    }

    @Test
    void obtenerRol_ComoAdmin_CuandoNoExiste_DebeRetornar404() throws Exception {
        when(securityService.obtenerRol(999L)).thenThrow(new IllegalArgumentException("Rol no encontrado"));

        mockMvc.perform(get("/api/security/roles/999")
                        .header("X-Internal-Api-Key", "api-key"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarRol_ComoAdmin_DebeRetornar200() throws Exception {
        RolResponseDTO actualizado = new RolResponseDTO(1L, "ROLE_ADMIN", List.of("ACCESS_ADMIN"));
        when(securityService.actualizarRol(eq(1L), any(RolRequestDTO.class))).thenReturn(actualizado);

        mockMvc.perform(put("/api/security/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"ROLE_ADMIN\"}")
                        .header("X-Internal-Api-Key", "api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idRol").value(1))
                .andExpect(jsonPath("$.nombre").value("ROLE_ADMIN"));
    }

    @Test
    void eliminarRol_ComoAdmin_DebeRetornar204() throws Exception {
        doNothing().when(securityService).eliminarRol(1L);

        mockMvc.perform(delete("/api/security/roles/1")
                        .header("X-Internal-Api-Key", "api-key"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listarPermisos_ComoAdmin_DebeRetornar200() throws Exception {
        when(securityService.listarPermisos()).thenReturn(List.of(crearPermisoResponse()));

        mockMvc.perform(get("/api/security/permisos")
                        .header("X-Internal-Api-Key", "api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPermiso").value(1))
                .andExpect(jsonPath("$[0].nombre").value("ACCESS_BASIC"));
    }
}
