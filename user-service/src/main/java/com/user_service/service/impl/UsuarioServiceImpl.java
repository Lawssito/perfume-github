package com.user_service.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.user_service.service.UsuarioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final DireccionRepository direccionRepository;
    private final AuthServiceClient authServiceClient;
    private final SecurityServiceClient securityServiceClient;

    @Override
    @Transactional
    public UsuarioResponseDTO registrarUsuario(RegistroUsuarioRequestDTO dto) {
        log.info("[AUDIT email={}] Registrando perfil", dto.getEmail());
        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            log.warn("[AUDIT email={}] Email ya registrado", dto.getEmail());
            throw new IllegalStateException("Email ya registrado");
        }

        Usuario usuario = Usuario.builder()
                .email(dto.getEmail())
                .nombre(dto.getNombre())
                .telefono(dto.getTelefono())
                .estado("ACTIVO")
                .build();
        Usuario guardado = usuarioRepository.save(usuario);

        try {
            authServiceClient.crearCredencial(
                    new CrearCredencialClientDTO(guardado.getIdUsuario(), guardado.getEmail(), dto.getPassword()));
            securityServiceClient.asignarRol(new AsignarRolClientDTO(guardado.getIdUsuario(), "ROLE_CLIENTE"));
        } catch (feign.FeignException ex) {
            log.error("[AUDIT idUsuario={}] Error comunicando con auth-service/security-service: status={} body={}",
                    guardado.getIdUsuario(), ex.status(), ex.contentUTF8());
            throw new RemoteServiceException("No se pudo completar el registro: fallo comunicando con auth-service o security-service");
        }

        log.info("[AUDIT idUsuario={}] Perfil creado exitosamente", guardado.getIdUsuario());
        return mapToDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPorId(Long idUsuario) {
        log.info("[AUDIT idUsuario={}] Consultando datos", idUsuario);
        return usuarioRepository.findById(idUsuario)
                .map(this::mapToDTO)
                .orElseThrow(() -> {
                    log.warn("[AUDIT idUsuario={}] No encontrado", idUsuario);
                    return new IllegalArgumentException("Usuario no encontrado");
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        log.info("[AUDIT] Listando todos los usuarios");
        List<UsuarioResponseDTO> lista = usuarioRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
        log.info("[AUDIT] Total usuarios: {}", lista.size());
        return lista;
    }

    @Override
    @Transactional
    public UsuarioResponseDTO actualizar(Long idUsuario, ActualizarUsuarioDTO dto) {
        log.info("[AUDIT idUsuario={}] Actualizando datos", idUsuario);
        Usuario usuario = obtenerEntidad(idUsuario);
        if (dto.getNombre() != null) {
            usuario.setNombre(dto.getNombre());
        }
        if (dto.getTelefono() != null) {
            usuario.setTelefono(dto.getTelefono());
        }
        Usuario actualizado = usuarioRepository.save(usuario);
        log.info("[AUDIT idUsuario={}] Actualizado exitosamente", idUsuario);
        return mapToDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long idUsuario) {
        log.info("[AUDIT idUsuario={}] Baja logica de usuario", idUsuario);
        Usuario usuario = obtenerEntidad(idUsuario);
        usuario.setEstado("ELIMINADO");
        usuarioRepository.save(usuario);

        // Desactivar credenciales en auth-service para inhabilitar el acceso
        try {
            authServiceClient.actualizarEstadoCuenta(
                idUsuario,
                new com.user_service.dto.ActualizarEstadoCredencialClientDTO("INACTIVO")
            );
            log.info("[AUDIT idUsuario={}] Credenciales desactivadas en auth-service", idUsuario);
        } catch (feign.FeignException e) {
            log.warn("[AUDIT idUsuario={}] No se pudieron desactivar credenciales: {}", idUsuario, e.getMessage());
            // No revertimos la eliminación lógica — el usuario ya fue marcado
        }

        log.info("[AUDIT idUsuario={}] Marcado como ELIMINADO", idUsuario);
    }

    @Override
    @Transactional
    public DireccionResponseDTO agregarDireccion(Long idUsuario, DireccionDTO dto) {
        log.info("[AUDIT idUsuario={}] Agregando direccion", idUsuario);
        Usuario usuario = obtenerEntidad(idUsuario);
        Direccion direccion = Direccion.builder()
                .usuario(usuario)
                .calle(dto.getCalle())
                .numero(dto.getNumero())
                .comuna(dto.getComuna())
                .ciudad(dto.getCiudad())
                .tipoAlias(dto.getTipoAlias())
                .build();
        Direccion guardada = direccionRepository.save(direccion);
        log.info("[AUDIT idUsuario={}] Direccion {} creada", idUsuario, guardada.getIdDireccion());
        return mapDireccion(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DireccionResponseDTO> listarDirecciones(Long idUsuario) {
        log.info("[AUDIT idUsuario={}] Listando direcciones", idUsuario);
        obtenerEntidad(idUsuario);
        return direccionRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .map(this::mapDireccion)
                .toList();
    }

    @Override
    @Transactional
    public void eliminarDireccion(Long idUsuario, Long idDireccion) {
        log.info("[AUDIT idUsuario={}] Eliminando direccion {}", idUsuario, idDireccion);
        Direccion direccion = direccionRepository.findByIdDireccionAndUsuarioIdUsuario(idDireccion, idUsuario)
                .orElseThrow(() -> {
                    log.warn("[AUDIT idUsuario={}] Direccion {} no encontrada", idUsuario, idDireccion);
                    return new IllegalArgumentException("Direccion no encontrada");
                });
        direccionRepository.delete(direccion);
        log.info("[AUDIT idUsuario={}] Direccion {} eliminada", idUsuario, idDireccion);
    }

    private Usuario obtenerEntidad(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private UsuarioResponseDTO mapToDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getIdUsuario(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getTelefono(),
                usuario.getEstado());
    }

    private DireccionResponseDTO mapDireccion(Direccion direccion) {
        return new DireccionResponseDTO(
                direccion.getIdDireccion(),
                direccion.getCalle(),
                direccion.getNumero(),
                direccion.getComuna(),
                direccion.getCiudad(),
                direccion.getTipoAlias());
    }
}
