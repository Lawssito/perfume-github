package com.user_service.service;

import java.util.List;

import com.user_service.dto.ActualizarUsuarioDTO;
import com.user_service.dto.DireccionDTO;
import com.user_service.dto.DireccionResponseDTO;
import com.user_service.dto.RegistroUsuarioRequestDTO;
import com.user_service.dto.UsuarioResponseDTO;

public interface UsuarioService {

    UsuarioResponseDTO registrarUsuario(RegistroUsuarioRequestDTO dto);

    UsuarioResponseDTO obtenerPorId(Long idUsuario);

    List<UsuarioResponseDTO> listarTodos();

    UsuarioResponseDTO actualizar(Long idUsuario, ActualizarUsuarioDTO dto);

    void eliminar(Long idUsuario);

    DireccionResponseDTO agregarDireccion(Long idUsuario, DireccionDTO dto);

    List<DireccionResponseDTO> listarDirecciones(Long idUsuario);

    void eliminarDireccion(Long idUsuario, Long idDireccion);
}