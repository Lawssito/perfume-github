package com.user_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.user_service.client.AuthServiceClient;
import com.user_service.client.SecurityServiceClient;
import com.user_service.dto.ActualizarUsuarioDTO;
import com.user_service.dto.AsignarRolClientDTO;
import com.user_service.dto.CrearCredencialClientDTO;
import com.user_service.dto.DireccionDTO;
import com.user_service.dto.DireccionResponseDTO;
import com.user_service.dto.RegistroUsuarioRequestDTO;
import com.user_service.dto.UsuarioResponseDTO;
import com.user_service.exception.RemoteServiceException;
import com.user_service.model.Direccion;
import com.user_service.model.Usuario;
import com.user_service.repository.DireccionRepository;
import com.user_service.repository.UsuarioRepository;
import com.user_service.service.impl.UsuarioServiceImpl;

import feign.FeignException;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private DireccionRepository direccionRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private SecurityServiceClient securityServiceClient;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Test
    void registrarUsuario_success() {
        RegistroUsuarioRequestDTO dto = new RegistroUsuarioRequestDTO();
        dto.setEmail("test@email.com");
        dto.setNombre("Test User");
        dto.setTelefono("123456789");
        dto.setPassword("password123");

        Usuario savedUsuario = Usuario.builder()
                .idUsuario(1L)
                .email(dto.getEmail())
                .nombre(dto.getNombre())
                .telefono(dto.getTelefono())
                .estado("ACTIVO")
                .build();

        when(usuarioRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(savedUsuario);

        UsuarioResponseDTO response = usuarioService.registrarUsuario(dto);

        assertNotNull(response);
        assertEquals(1L, response.getIdUsuario());
        assertEquals(dto.getEmail(), response.getEmail());
        assertEquals(dto.getNombre(), response.getNombre());
        assertEquals(dto.getTelefono(), response.getTelefono());
        assertEquals("ACTIVO", response.getEstado());
        verify(authServiceClient).crearCredencial(any(CrearCredencialClientDTO.class));
        verify(securityServiceClient).asignarRol(any(AsignarRolClientDTO.class));
    }

    @Test
    void registrarUsuario_emailDuplicate_throwsException() {
        RegistroUsuarioRequestDTO dto = new RegistroUsuarioRequestDTO();
        dto.setEmail("test@email.com");
        dto.setNombre("Test User");
        dto.setPassword("password123");

        when(usuarioRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(new Usuario()));

        assertThrows(IllegalStateException.class, () -> usuarioService.registrarUsuario(dto));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarUsuario_feignError_throwsRemoteServiceException() {
        RegistroUsuarioRequestDTO dto = new RegistroUsuarioRequestDTO();
        dto.setEmail("test@email.com");
        dto.setNombre("Test User");
        dto.setPassword("password123");

        Usuario savedUsuario = Usuario.builder()
                .idUsuario(1L)
                .email(dto.getEmail())
                .nombre(dto.getNombre())
                .estado("ACTIVO")
                .build();

        when(usuarioRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(savedUsuario);
        doThrow(mock(FeignException.class)).when(authServiceClient).crearCredencial(any());

        assertThrows(RemoteServiceException.class, () -> usuarioService.registrarUsuario(dto));
    }

    @Test
    void obtenerPorId_found() {
        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .email("test@email.com")
                .nombre("Test User")
                .telefono("123456789")
                .estado("ACTIVO")
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UsuarioResponseDTO response = usuarioService.obtenerPorId(1L);

        assertEquals(1L, response.getIdUsuario());
        assertEquals("test@email.com", response.getEmail());
        assertEquals("Test User", response.getNombre());
        assertEquals("123456789", response.getTelefono());
        assertEquals("ACTIVO", response.getEstado());
    }

    @Test
    void obtenerPorId_notFound_throwsException() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> usuarioService.obtenerPorId(99L));
    }

    @Test
    void listarTodos() {
        Usuario u1 = Usuario.builder()
                .idUsuario(1L).email("a@test.com").nombre("A").telefono("1").estado("ACTIVO").build();
        Usuario u2 = Usuario.builder()
                .idUsuario(2L).email("b@test.com").nombre("B").telefono("2").estado("ACTIVO").build();

        when(usuarioRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UsuarioResponseDTO> result = usuarioService.listarTodos();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getIdUsuario());
        assertEquals("a@test.com", result.get(0).getEmail());
        assertEquals(2L, result.get(1).getIdUsuario());
    }

    @Test
    void actualizar_success() {
        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .email("test@email.com")
                .nombre("Old Name")
                .telefono("111")
                .estado("ACTIVO")
                .build();

        ActualizarUsuarioDTO dto = new ActualizarUsuarioDTO();
        dto.setNombre("New Name");
        dto.setTelefono("222");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        UsuarioResponseDTO response = usuarioService.actualizar(1L, dto);

        assertEquals("New Name", response.getNombre());
        assertEquals("222", response.getTelefono());
        assertEquals("New Name", usuario.getNombre());
        assertEquals("222", usuario.getTelefono());
    }

    @Test
    void actualizar_notFound_throwsException() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> usuarioService.actualizar(99L, new ActualizarUsuarioDTO()));
    }

    @Test
    void eliminar_success() {
        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .email("test@email.com")
                .nombre("Test User")
                .estado("ACTIVO")
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        usuarioService.eliminar(1L);

        assertEquals("ELIMINADO", usuario.getEstado());
        verify(usuarioRepository).save(usuario);
        verify(authServiceClient).actualizarEstadoCuenta(eq(1L), any());
    }

    @Test
    void eliminar_notFound_throwsException() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> usuarioService.eliminar(99L));
    }

    @Test
    void agregarDireccion() {
        Usuario usuario = Usuario.builder()
                .idUsuario(1L)
                .email("test@email.com")
                .nombre("Test User")
                .estado("ACTIVO")
                .build();

        DireccionDTO dto = new DireccionDTO();
        dto.setCalle("Av. Siempre Viva");
        dto.setNumero("742");
        dto.setComuna("Springfield");
        dto.setCiudad("Santiago");
        dto.setTipoAlias("Casa");

        Direccion savedDireccion = Direccion.builder()
                .idDireccion(1L)
                .usuario(usuario)
                .calle(dto.getCalle())
                .numero(dto.getNumero())
                .comuna(dto.getComuna())
                .ciudad(dto.getCiudad())
                .tipoAlias(dto.getTipoAlias())
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(direccionRepository.save(any(Direccion.class))).thenReturn(savedDireccion);

        DireccionResponseDTO response = usuarioService.agregarDireccion(1L, dto);

        assertEquals(1L, response.getIdDireccion());
        assertEquals(dto.getCalle(), response.getCalle());
        assertEquals(dto.getNumero(), response.getNumero());
        assertEquals(dto.getComuna(), response.getComuna());
        assertEquals(dto.getCiudad(), response.getCiudad());
        assertEquals(dto.getTipoAlias(), response.getTipoAlias());
    }

    @Test
    void listarDirecciones() {
        Usuario usuario = Usuario.builder()
                .idUsuario(1L).email("test@email.com").nombre("Test").estado("ACTIVO").build();
        Direccion d1 = Direccion.builder()
                .idDireccion(1L).usuario(usuario).calle("Calle 1").numero("1")
                .comuna("Comuna 1").ciudad("Ciudad 1").tipoAlias("Casa").build();
        Direccion d2 = Direccion.builder()
                .idDireccion(2L).usuario(usuario).calle("Calle 2").numero("2")
                .comuna("Comuna 2").ciudad("Ciudad 2").tipoAlias("Trabajo").build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(direccionRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of(d1, d2));

        List<DireccionResponseDTO> result = usuarioService.listarDirecciones(1L);

        assertEquals(2, result.size());
        assertEquals("Calle 1", result.get(0).getCalle());
        assertEquals("Calle 2", result.get(1).getCalle());
    }

    @Test
    void eliminarDireccion_success() {
        Direccion direccion = Direccion.builder().idDireccion(1L).build();

        when(direccionRepository.findByIdDireccionAndUsuarioIdUsuario(1L, 1L))
                .thenReturn(Optional.of(direccion));

        usuarioService.eliminarDireccion(1L, 1L);

        verify(direccionRepository).delete(direccion);
    }

    @Test
    void eliminarDireccion_notFound_throwsException() {
        when(direccionRepository.findByIdDireccionAndUsuarioIdUsuario(99L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> usuarioService.eliminarDireccion(1L, 99L));
    }

}
