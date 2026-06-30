package com.security_service.service;

import com.security_service.dto.*;
import com.security_service.model.Permiso;
import com.security_service.model.Rol;
import com.security_service.model.UsuarioRol;
import com.security_service.repository.PermisoRepository;
import com.security_service.repository.RolRepository;
import com.security_service.repository.UsuarioRolRepository;
import com.security_service.service.impl.SecurityServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceImplTest {

    @Mock
    private UsuarioRolRepository usuarioRolRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PermisoRepository permisoRepository;

    @InjectMocks
    private SecurityServiceImpl securityService;

    @Test
    void asignarRol_conNuevoRol_creaRolYAsigna() {
        AsignarRolRequestDTO request = new AsignarRolRequestDTO();
        request.setIdUsuario(1L);
        request.setRolNombre("ADMIN");

        Rol nuevoRol = Rol.builder().idRol(1L).nombre("ADMIN").permisos(new HashSet<>()).build();
        UsuarioRol usuarioRol = UsuarioRol.builder().id(1L).idUsuario(1L).rol(nuevoRol).build();

        when(rolRepository.findByNombre("ADMIN")).thenReturn(Optional.empty());
        when(rolRepository.save(any(Rol.class))).thenReturn(nuevoRol);
        when(usuarioRolRepository.findByIdUsuario(1L))
                .thenReturn(List.of())
                .thenReturn(List.of(usuarioRol));

        RolesUsuarioResponseDTO result = securityService.asignarRol(request);

        assertEquals(1L, result.getIdUsuario());
        assertEquals(List.of("ADMIN"), result.getRoles());
        verify(rolRepository).save(any(Rol.class));
        verify(usuarioRolRepository).save(any(UsuarioRol.class));
    }

    @Test
    void asignarRol_rolYaAsignado_noDuplica() {
        AsignarRolRequestDTO request = new AsignarRolRequestDTO();
        request.setIdUsuario(1L);
        request.setRolNombre("ADMIN");

        Rol rol = Rol.builder().idRol(1L).nombre("ADMIN").build();
        UsuarioRol usuarioRol = UsuarioRol.builder().id(1L).idUsuario(1L).rol(rol).build();

        when(rolRepository.findByNombre("ADMIN")).thenReturn(Optional.of(rol));
        when(usuarioRolRepository.findByIdUsuario(1L))
                .thenReturn(List.of(usuarioRol));

        RolesUsuarioResponseDTO result = securityService.asignarRol(request);

        assertEquals(List.of("ADMIN"), result.getRoles());
        verify(rolRepository, never()).save(any(Rol.class));
        verify(usuarioRolRepository, never()).save(any(UsuarioRol.class));
    }

    @Test
    void obtenerRoles_retornaRolesDelUsuario() {
        Rol rol1 = Rol.builder().idRol(1L).nombre("ADMIN").build();
        Rol rol2 = Rol.builder().idRol(2L).nombre("CLIENTE").build();
        UsuarioRol ur1 = UsuarioRol.builder().id(1L).idUsuario(1L).rol(rol1).build();
        UsuarioRol ur2 = UsuarioRol.builder().id(2L).idUsuario(1L).rol(rol2).build();

        when(usuarioRolRepository.findByIdUsuario(1L)).thenReturn(List.of(ur1, ur2));

        RolesUsuarioResponseDTO result = securityService.obtenerRoles(1L);

        assertEquals(1L, result.getIdUsuario());
        assertTrue(result.getRoles().containsAll(List.of("ADMIN", "CLIENTE")));
    }

    @Test
    void revocarRol_exitoso() {
        Rol rol = Rol.builder().idRol(1L).nombre("ADMIN").build();
        UsuarioRol usuarioRol = UsuarioRol.builder().id(1L).idUsuario(1L).rol(rol).build();

        when(usuarioRolRepository.findByIdUsuario(1L)).thenReturn(List.of(usuarioRol));

        securityService.revocarRol(1L, "ADMIN");

        verify(usuarioRolRepository).delete(usuarioRol);
    }

    @Test
    void revocarRol_noAsignado_lanzaIAE() {
        Rol rol = Rol.builder().idRol(1L).nombre("CLIENTE").build();
        UsuarioRol usuarioRol = UsuarioRol.builder().id(1L).idUsuario(1L).rol(rol).build();

        when(usuarioRolRepository.findByIdUsuario(1L)).thenReturn(List.of(usuarioRol));

        assertThrows(IllegalArgumentException.class,
                () -> securityService.revocarRol(1L, "ADMIN"));
        verify(usuarioRolRepository, never()).delete(any());
    }

    @Test
    void validarPermiso_tienePermiso_retornaTrue() {
        Permiso permiso = Permiso.builder().idPermiso(1L).nombre("ACCESS_ADMIN").build();
        Rol rol = Rol.builder().idRol(1L).nombre("ADMIN").permisos(Set.of(permiso)).build();
        UsuarioRol usuarioRol = UsuarioRol.builder().id(1L).idUsuario(1L).rol(rol).build();

        when(usuarioRolRepository.findByIdUsuario(1L)).thenReturn(List.of(usuarioRol));

        ValidarAccesoRequestDTO request = new ValidarAccesoRequestDTO();
        request.setIdUsuario(1L);
        request.setPermisoRequerido("ACCESS_ADMIN");

        ValidacionResponseDTO result = securityService.validarPermiso(request);

        assertTrue(result.isValido());
        assertEquals("Acceso autorizado", result.getMensaje());
        assertEquals(1L, result.getIdUsuario());
    }

    @Test
    void validarPermiso_noTienePermiso_retornaFalse() {
        Rol rol = Rol.builder().idRol(1L).nombre("CLIENTE").permisos(new HashSet<>()).build();
        UsuarioRol usuarioRol = UsuarioRol.builder().id(1L).idUsuario(1L).rol(rol).build();

        when(usuarioRolRepository.findByIdUsuario(1L)).thenReturn(List.of(usuarioRol));

        ValidarAccesoRequestDTO request = new ValidarAccesoRequestDTO();
        request.setIdUsuario(1L);
        request.setPermisoRequerido("ACCESS_ADMIN");

        ValidacionResponseDTO result = securityService.validarPermiso(request);

        assertFalse(result.isValido());
        assertEquals("No tiene permisos suficientes", result.getMensaje());
    }

    @Test
    void listarRoles_retornaLista() {
        Permiso permiso = Permiso.builder().idPermiso(1L).nombre("ACCESS_ADMIN").build();
        Rol rol = Rol.builder().idRol(1L).nombre("ADMIN").permisos(Set.of(permiso)).build();

        when(rolRepository.findAll()).thenReturn(List.of(rol));

        List<RolResponseDTO> result = securityService.listarRoles();

        assertEquals(1, result.size());
        assertEquals("ADMIN", result.getFirst().getNombre());
        assertEquals(List.of("ACCESS_ADMIN"), result.getFirst().getPermisos());
    }

    @Test
    void crearRol_exitoso() {
        RolRequestDTO dto = new RolRequestDTO();
        dto.setNombre("ADMIN");

        Rol rolGuardado = Rol.builder().idRol(1L).nombre("ADMIN").permisos(new HashSet<>()).build();

        when(rolRepository.findByNombre("ADMIN")).thenReturn(Optional.empty());
        when(rolRepository.save(any(Rol.class))).thenReturn(rolGuardado);

        RolResponseDTO result = securityService.crearRol(dto);

        assertEquals(1L, result.getIdRol());
        assertEquals("ADMIN", result.getNombre());
    }

    @Test
    void crearRol_yaExiste_lanzaExcepcion() {
        RolRequestDTO dto = new RolRequestDTO();
        dto.setNombre("ADMIN");

        when(rolRepository.findByNombre("ADMIN")).thenReturn(Optional.of(Rol.builder().build()));

        assertThrows(IllegalStateException.class, () -> securityService.crearRol(dto));
        verify(rolRepository, never()).save(any());
    }

    @Test
    void obtenerRol_exitoso() {
        Rol rol = Rol.builder().idRol(1L).nombre("ADMIN").permisos(Set.of()).build();

        when(rolRepository.findById(1L)).thenReturn(Optional.of(rol));

        RolResponseDTO result = securityService.obtenerRol(1L);

        assertEquals(1L, result.getIdRol());
        assertEquals("ADMIN", result.getNombre());
    }

    @Test
    void obtenerRol_noExiste_lanzaExcepcion() {
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> securityService.obtenerRol(99L));
    }

    @Test
    void actualizarRol_actualizaYRetorna() {
        RolRequestDTO dto = new RolRequestDTO();
        dto.setNombre("SUPER_ADMIN");

        Rol rolExistente = Rol.builder().idRol(1L).nombre("ADMIN").permisos(new HashSet<>()).build();
        Rol rolActualizado = Rol.builder().idRol(1L).nombre("SUPER_ADMIN").permisos(new HashSet<>()).build();

        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolExistente));
        when(rolRepository.save(any(Rol.class))).thenReturn(rolActualizado);

        RolResponseDTO result = securityService.actualizarRol(1L, dto);

        assertEquals("SUPER_ADMIN", result.getNombre());
    }

    @Test
    void eliminarRol_exitoso() {
        Rol rol = Rol.builder().idRol(1L).nombre("ADMIN").build();

        when(rolRepository.findById(1L)).thenReturn(Optional.of(rol));

        securityService.eliminarRol(1L);

        verify(rolRepository).delete(rol);
    }

    @Test
    void listarPermisos_retornaLista() {
        Permiso permiso1 = Permiso.builder().idPermiso(1L).nombre("ACCESS_ADMIN").build();
        Permiso permiso2 = Permiso.builder().idPermiso(2L).nombre("VER_PEDIDOS").build();

        when(permisoRepository.findAll()).thenReturn(List.of(permiso1, permiso2));

        List<PermisoResponseDTO> result = securityService.listarPermisos();

        assertEquals(2, result.size());
    }

    @Test
    void crearPermiso_exitoso() {
        PermisoRequestDTO dto = new PermisoRequestDTO();
        dto.setNombre("ACCESS_ADMIN");

        Permiso permisoGuardado = Permiso.builder().idPermiso(1L).nombre("ACCESS_ADMIN").build();

        when(permisoRepository.findByNombre("ACCESS_ADMIN")).thenReturn(Optional.empty());
        when(permisoRepository.save(any(Permiso.class))).thenReturn(permisoGuardado);

        PermisoResponseDTO result = securityService.crearPermiso(dto);

        assertEquals(1L, result.getIdPermiso());
        assertEquals("ACCESS_ADMIN", result.getNombre());
    }

    @Test
    void crearPermiso_yaExiste_lanzaExcepcion() {
        PermisoRequestDTO dto = new PermisoRequestDTO();
        dto.setNombre("ACCESS_ADMIN");

        when(permisoRepository.findByNombre("ACCESS_ADMIN")).thenReturn(Optional.of(Permiso.builder().build()));

        assertThrows(IllegalStateException.class, () -> securityService.crearPermiso(dto));
        verify(permisoRepository, never()).save(any());
    }

    @Test
    void eliminarPermiso_exitoso() {
        Permiso permiso = Permiso.builder().idPermiso(1L).nombre("ACCESS_ADMIN").build();

        when(permisoRepository.findById(1L)).thenReturn(Optional.of(permiso));

        securityService.eliminarPermiso(1L);

        verify(permisoRepository).delete(permiso);
    }

    @Test
    void eliminarPermiso_noExiste_lanzaExcepcion() {
        when(permisoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> securityService.eliminarPermiso(99L));
        verify(permisoRepository, never()).delete(any());
    }
}
