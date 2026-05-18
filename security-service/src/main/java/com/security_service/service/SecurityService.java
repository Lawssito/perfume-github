package com.security_service.service;

import com.security_service.dto.*;

import java.util.List;

public interface SecurityService {

    RolesUsuarioResponseDTO asignarRol(AsignarRolRequestDTO request);

    RolesUsuarioResponseDTO obtenerRoles(Long idUsuario);

    void revocarRol(Long idUsuario, String rolNombre);

    ValidacionResponseDTO validarPermiso(ValidarAccesoRequestDTO request);

    List<RolResponseDTO> listarRoles();

    RolResponseDTO crearRol(RolRequestDTO dto);

    RolResponseDTO obtenerRol(Long idRol);

    RolResponseDTO actualizarRol(Long idRol, RolRequestDTO dto);

    void eliminarRol(Long idRol);

    List<PermisoResponseDTO> listarPermisos();

    PermisoResponseDTO crearPermiso(PermisoRequestDTO dto);

    void eliminarPermiso(Long idPermiso);
}
