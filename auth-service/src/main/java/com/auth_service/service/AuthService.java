package com.auth_service.service;

import java.util.List;

import com.auth_service.dto.ActualizarEstadoCuentaDTO;
import com.auth_service.dto.AuthResponseDTO;
import com.auth_service.dto.CrearCredencialRequestDTO;
import com.auth_service.dto.CredencialResponseDTO;
import com.auth_service.dto.LoginRequestDTO;
import com.auth_service.dto.TokenClaimsResponseDTO;
import com.auth_service.dto.ValidateTokenRequestDTO;

public interface AuthService {

    void crearCredencial(CrearCredencialRequestDTO dto);

    AuthResponseDTO autenticarUsuario(LoginRequestDTO request);

    TokenClaimsResponseDTO validarToken(ValidateTokenRequestDTO request);

    AuthResponseDTO refreshToken(String refreshToken);

    List<CredencialResponseDTO> listarCredenciales();

    CredencialResponseDTO obtenerPorIdUsuario(Long idUsuario);

    CredencialResponseDTO actualizarEstadoCuenta(Long idUsuario, ActualizarEstadoCuentaDTO dto);

    void eliminarCredencial(Long idUsuario);
}
