package com.security_service.service.impl;

import com.security_service.dto.*;
import com.security_service.model.Permiso;
import com.security_service.model.Rol;
import com.security_service.model.UsuarioRol;
import com.security_service.repository.PermisoRepository;
import com.security_service.repository.RolRepository;
import com.security_service.repository.UsuarioRolRepository;
import com.security_service.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    private final UsuarioRolRepository usuarioRolRepository;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;

    @Override
    @Transactional
    public RolesUsuarioResponseDTO asignarRol(AsignarRolRequestDTO request) {
        log.info("[AUDIT] Asignando rol {} a usuario {}", request.getRolNombre(), request.getIdUsuario());
        Rol rol = rolRepository.findByNombre(request.getRolNombre())
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre(request.getRolNombre()).build()));

        boolean yaAsignado = usuarioRolRepository.findByIdUsuario(request.getIdUsuario()).stream()
                .anyMatch(ur -> ur.getRol().getNombre().equals(request.getRolNombre()));
        if (!yaAsignado) {
            usuarioRolRepository.save(UsuarioRol.builder()
                    .idUsuario(request.getIdUsuario())
                    .rol(rol)
                    .build());
        }
        return obtenerRoles(request.getIdUsuario());
    }

    @Override
    @Transactional(readOnly = true)
    public RolesUsuarioResponseDTO obtenerRoles(Long idUsuario) {
        List<String> roles = usuarioRolRepository.findByIdUsuario(idUsuario).stream()
                .map(ur -> ur.getRol().getNombre())
                .toList();
        log.info("[AUDIT] Roles de usuario {}: {}", idUsuario, roles);
        return new RolesUsuarioResponseDTO(idUsuario, roles);
    }

    @Override
    @Transactional
    public void revocarRol(Long idUsuario, String rolNombre) {
        log.info("[AUDIT] Revocando rol {} de usuario {}", rolNombre, idUsuario);
        usuarioRolRepository.findByIdUsuario(idUsuario).stream()
                .filter(ur -> ur.getRol().getNombre().equals(rolNombre))
                .findFirst()
                .ifPresentOrElse(
                        usuarioRolRepository::delete,
                        () -> {
                            throw new IllegalArgumentException("El usuario no tiene asignado ese rol");
                        });
    }

    @Override
    @Transactional(readOnly = true)
    public ValidacionResponseDTO validarPermiso(ValidarAccesoRequestDTO request) {
        log.info("[AUDIT] Validando permiso {} para usuario {}", request.getPermisoRequerido(), request.getIdUsuario());
        boolean tienePermiso = usuarioRolRepository.findByIdUsuario(request.getIdUsuario()).stream()
                .flatMap(ur -> ur.getRol().getPermisos() == null
                        ? java.util.stream.Stream.empty()
                        : ur.getRol().getPermisos().stream())
                .anyMatch(p -> p.getNombre().equals(request.getPermisoRequerido()));

        ValidacionResponseDTO response = new ValidacionResponseDTO();
        response.setValido(tienePermiso);
        response.setIdUsuario(request.getIdUsuario());
        response.setMensaje(tienePermiso ? "Acceso autorizado" : "No tiene permisos suficientes");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolResponseDTO> listarRoles() {
        log.info("[AUDIT] Listando roles");
        return rolRepository.findAll().stream().map(this::mapRol).toList();
    }

    @Override
    @Transactional
    public RolResponseDTO crearRol(RolRequestDTO dto) {
        log.info("[AUDIT] Creando rol {}", dto.getNombre());
        if (rolRepository.findByNombre(dto.getNombre()).isPresent()) {
            throw new IllegalStateException("El rol ya existe");
        }
        Rol rol = Rol.builder().nombre(dto.getNombre()).permisos(new HashSet<>()).build();
        return mapRol(rolRepository.save(rol));
    }

    @Override
    @Transactional(readOnly = true)
    public RolResponseDTO obtenerRol(Long idRol) {
        return mapRol(obtenerRolEntidad(idRol));
    }

    @Override
    @Transactional
    public RolResponseDTO actualizarRol(Long idRol, RolRequestDTO dto) {
        log.info("[AUDIT] Actualizando rol id={}", idRol);
        Rol rol = obtenerRolEntidad(idRol);
        rol.setNombre(dto.getNombre());
        return mapRol(rolRepository.save(rol));
    }

    @Override
    @Transactional
    public void eliminarRol(Long idRol) {
        log.info("[AUDIT] Eliminando rol id={}", idRol);
        rolRepository.delete(obtenerRolEntidad(idRol));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermisoResponseDTO> listarPermisos() {
        log.info("[AUDIT] Listando permisos");
        return permisoRepository.findAll().stream()
                .map(p -> new PermisoResponseDTO(p.getIdPermiso(), p.getNombre()))
                .toList();
    }

    @Override
    @Transactional
    public PermisoResponseDTO crearPermiso(PermisoRequestDTO dto) {
        log.info("[AUDIT] Creando permiso {}", dto.getNombre());
        if (permisoRepository.findByNombre(dto.getNombre()).isPresent()) {
            throw new IllegalStateException("El permiso ya existe");
        }
        Permiso permiso = permisoRepository.save(Permiso.builder().nombre(dto.getNombre()).build());
        return new PermisoResponseDTO(permiso.getIdPermiso(), permiso.getNombre());
    }

    @Override
    @Transactional
    public void eliminarPermiso(Long idPermiso) {
        log.info("[AUDIT] Eliminando permiso id={}", idPermiso);
        permisoRepository.delete(permisoRepository.findById(idPermiso)
                .orElseThrow(() -> new IllegalArgumentException("Permiso no encontrado")));
    }

    private Rol obtenerRolEntidad(Long idRol) {
        return rolRepository.findById(idRol)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));
    }

    private RolResponseDTO mapRol(Rol rol) {
        List<String> permisos = rol.getPermisos() == null
                ? List.of()
                : rol.getPermisos().stream().map(Permiso::getNombre).toList();
        return new RolResponseDTO(rol.getIdRol(), rol.getNombre(), permisos);
    }
}
