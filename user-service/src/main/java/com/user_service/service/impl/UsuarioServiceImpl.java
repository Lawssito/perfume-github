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
        log.info("[SERVICE] Registrando perfil con email={}", dto.getEmail());
        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            log.warn("[SERVICE] Email ya registrado: {}", dto.getEmail());
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
            log.error("[SERVICE] Error comunicando con auth-service/security-service para usuario {}: status={} body={}",
                    guardado.getIdUsuario(), ex.status(), ex.contentUTF8());
            throw new RemoteServiceException("No se pudo completar el registro: fallo comunicando con auth-service o security-service");
        }

        log.info("[SERVICE] Perfil creado idUsuario={}", guardado.getIdUsuario());
        return mapToDTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPorId(Long idUsuario) {
        log.info("[SERVICE] Consultando usuario id={}", idUsuario);
        return usuarioRepository.findById(idUsuario)
                .map(this::mapToDTO)
                .orElseThrow(() -> {
                    log.warn("[SERVICE] Usuario no encontrado id={}", idUsuario);
                    return new IllegalArgumentException("Usuario no encontrado");
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        log.info("[SERVICE] Listando todos los usuarios");
        List<UsuarioResponseDTO> lista = usuarioRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
        log.info("[SERVICE] Total usuarios: {}", lista.size());
        return lista;
    }

    @Override
    @Transactional
    public UsuarioResponseDTO actualizar(Long idUsuario, ActualizarUsuarioDTO dto) {
        log.info("[SERVICE] Actualizando usuario id={}", idUsuario);
        Usuario usuario = obtenerEntidad(idUsuario);
        if (dto.getNombre() != null) {
            usuario.setNombre(dto.getNombre());
        }
        if (dto.getTelefono() != null) {
            usuario.setTelefono(dto.getTelefono());
        }
        Usuario actualizado = usuarioRepository.save(usuario);
        log.info("[SERVICE] Usuario id={} actualizado", idUsuario);
        return mapToDTO(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long idUsuario) {
        log.info("[SERVICE] Eliminacion logica usuario id={}", idUsuario);
        Usuario usuario = obtenerEntidad(idUsuario);
        usuario.setEstado("ELIMINADO");
        usuarioRepository.save(usuario);
        log.info("[SERVICE] Usuario id={} marcado como ELIMINADO", idUsuario);
    }

    @Override
    @Transactional
    public DireccionResponseDTO agregarDireccion(Long idUsuario, DireccionDTO dto) {
        log.info("[SERVICE] Agregando direccion a usuario id={}", idUsuario);
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
        log.info("[SERVICE] Direccion id={} creada para usuario id={}", guardada.getIdDireccion(), idUsuario);
        return mapDireccion(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DireccionResponseDTO> listarDirecciones(Long idUsuario) {
        log.info("[SERVICE] Listando direcciones de usuario id={}", idUsuario);
        obtenerEntidad(idUsuario);
        return direccionRepository.findByUsuarioIdUsuario(idUsuario).stream()
                .map(this::mapDireccion)
                .toList();
    }

    @Override
    @Transactional
    public void eliminarDireccion(Long idUsuario, Long idDireccion) {
        log.info("[SERVICE] Eliminando direccion id={} de usuario id={}", idDireccion, idUsuario);
        Direccion direccion = direccionRepository.findByIdDireccionAndUsuarioIdUsuario(idDireccion, idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Direccion no encontrada"));
        direccionRepository.delete(direccion);
        log.info("[SERVICE] Direccion id={} eliminada", idDireccion);
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
