package com.auth_service.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth_service.client.SecurityServiceClient;
import com.auth_service.dto.ActualizarEstadoCuentaDTO;
import com.auth_service.dto.AuthResponseDTO;
import com.auth_service.dto.CrearCredencialRequestDTO;
import com.auth_service.dto.CredencialResponseDTO;
import com.auth_service.dto.LoginRequestDTO;
import com.auth_service.dto.RolesUsuarioResponseDTO;
import com.auth_service.dto.TokenClaimsResponseDTO;
import com.auth_service.dto.ValidateTokenRequestDTO;
import com.auth_service.model.Credencial;
import com.auth_service.model.RefreshToken;
import com.auth_service.repository.CredencialRepository;
import com.auth_service.repository.RefreshTokenRepository;
import com.auth_service.service.AuthService;
import com.auth_service.service.JwtService;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final CredencialRepository credencialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityServiceClient securityServiceClient;
    private final JwtService jwtService;

    @Override
    @Transactional
    public void crearCredencial(CrearCredencialRequestDTO dto) {
        log.info("[AUDIT idUsuario={}] Creando credencial email={}", dto.getIdUsuario(), dto.getEmail());
        if (credencialRepository.existsByEmailLogin(dto.getEmail())) {
            log.warn("[AUDIT idUsuario={}] Email {} ya registrado", dto.getIdUsuario(), dto.getEmail());
            throw new IllegalStateException("Ya existen credenciales para el email indicado");
        }
        if (credencialRepository.existsByIdUsuario(dto.getIdUsuario())) {
            log.warn("[AUDIT idUsuario={}] Ya existen credenciales", dto.getIdUsuario());
            throw new IllegalStateException("Ya existen credenciales para el usuario indicado");
        }
        Credencial credencial = Credencial.builder()
                .idUsuario(dto.getIdUsuario())
                .emailLogin(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .estadoCuenta("ACTIVO")
                .build();
        credencialRepository.save(credencial);
        log.info("[AUDIT idUsuario={}] Credenciales creadas exitosamente", dto.getIdUsuario());
    }

    @Override
    @Transactional
    public AuthResponseDTO autenticarUsuario(LoginRequestDTO request) {
        log.info("[AUDIT email={}] Intento de login", request.getEmail());
        Credencial credencial = credencialRepository.findByEmailLogin(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("[AUDIT email={}] Credenciales invalidas — email no encontrado", request.getEmail());
                    return new IllegalArgumentException("Credenciales invalidas");
                });

        if (!"ACTIVO".equals(credencial.getEstadoCuenta())) {
            log.warn("[AUDIT idUsuario={}] Cuenta inactiva ({})", credencial.getIdUsuario(), credencial.getEstadoCuenta());
            throw new IllegalStateException("Cuenta inactiva");
        }
        if (!passwordEncoder.matches(request.getPassword(), credencial.getPasswordHash())) {
            log.warn("[AUDIT idUsuario={}] Password incorrecto", credencial.getIdUsuario());
            throw new IllegalArgumentException("Credenciales invalidas");
        }

        List<String> roles = obtenerRolesDesdeSecurity(credencial.getIdUsuario());
        String token = jwtService.generarToken(credencial.getEmailLogin(), credencial.getIdUsuario(), roles);
        String refreshToken = generarYGuardarRefreshToken(credencial);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setIdUsuario(credencial.getIdUsuario());
        response.setEmail(credencial.getEmailLogin());
        response.setRoles(roles);
        response.setMensaje("Login exitoso");
        log.info("[AUDIT idUsuario={} email={}] Login exitoso. Roles: {}", credencial.getIdUsuario(), credencial.getEmailLogin(), roles);
        return response;
    }

    @Override
    @Transactional
    public AuthResponseDTO refreshToken(String refreshTokenRaw) {
        log.info("[AUDIT] Procesando refresh token");

        String hash = sha256(refreshTokenRaw);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> {
                    log.warn("[AUDIT] Refresh token no encontrado o invalido");
                    return new IllegalArgumentException("Refresh token invalido");
                });

        if (stored.isRevocado()) {
            log.warn("[AUDIT] Refresh token revocado");
            throw new IllegalArgumentException("Refresh token revocado");
        }

        if (stored.getExpiraEn().isBefore(java.time.LocalDateTime.now())) {
            log.warn("[AUDIT] Refresh token expirado");
            stored.setRevocado(true);
            refreshTokenRepository.save(stored);
            throw new IllegalArgumentException("Refresh token expirado");
        }

        // Revocar el token anterior (rotación)
        stored.setRevocado(true);
        refreshTokenRepository.save(stored);

        Credencial credencial = stored.getCredencial();
        List<String> roles = obtenerRolesDesdeSecurity(credencial.getIdUsuario());
        String nuevoToken = jwtService.generarToken(credencial.getEmailLogin(), credencial.getIdUsuario(), roles);
        String nuevoRefresh = generarYGuardarRefreshToken(credencial);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(nuevoToken);
        response.setRefreshToken(nuevoRefresh);
        response.setIdUsuario(credencial.getIdUsuario());
        response.setEmail(credencial.getEmailLogin());
        response.setRoles(roles);
        response.setMensaje("Token renovado exitosamente");
        log.info("[AUDIT idUsuario={}] Token renovado via refresh", credencial.getIdUsuario());
        return response;
    }

    private String generarYGuardarRefreshToken(Credencial credencial) {
        String token = java.util.UUID.randomUUID().toString() + "-" + java.util.UUID.randomUUID().toString();
        String hash = sha256(token);

        RefreshToken rt = RefreshToken.builder()
                .credencial(credencial)
                .tokenHash(hash)
                .expiraEn(java.time.LocalDateTime.now().plusDays(7))
                .revocado(false)
                .build();
        refreshTokenRepository.save(rt);
        return token;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    @Override
    public TokenClaimsResponseDTO validarToken(ValidateTokenRequestDTO request) {
        try {
            Claims claims = jwtService.validarToken(request.getToken());
            Long idUsuario = claims.get("idUsuario", Long.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            log.info("[AUDIT idUsuario={}] Token validado exitosamente", idUsuario);
            return new TokenClaimsResponseDTO(true, claims.getSubject(), idUsuario, roles, "Token valido");
        } catch (Exception e) {
            log.warn("[AUDIT] Token invalido: {}", e.getMessage());
            return new TokenClaimsResponseDTO(false, null, null, Collections.emptyList(), "Token invalido o expirado");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CredencialResponseDTO> listarCredenciales() {
        log.info("[AUDIT] Listando todas las credenciales");
        return credencialRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CredencialResponseDTO obtenerPorIdUsuario(Long idUsuario) {
        log.info("[AUDIT idUsuario={}] Consultando credencial", idUsuario);
        return credencialRepository.findByIdUsuario(idUsuario)
                .map(this::mapToResponse)
                .orElseThrow(() -> {
                    log.warn("[AUDIT idUsuario={}] Credencial no encontrada", idUsuario);
                    return new IllegalArgumentException("Credencial no encontrada");
                });
    }

    @Override
    @Transactional
    public CredencialResponseDTO actualizarEstadoCuenta(Long idUsuario, ActualizarEstadoCuentaDTO dto) {
        log.info("[AUDIT idUsuario={}] Cambiando estado a {}", idUsuario, dto.getEstadoCuenta());
        Credencial credencial = credencialRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> {
                    log.warn("[AUDIT idUsuario={}] Credencial no encontrada para actualizar estado", idUsuario);
                    return new IllegalArgumentException("Credencial no encontrada");
                });
        credencial.setEstadoCuenta(dto.getEstadoCuenta());
        CredencialResponseDTO response = mapToResponse(credencialRepository.save(credencial));
        log.info("[AUDIT idUsuario={}] Estado actualizado a {}", idUsuario, response.getEstadoCuenta());
        return response;
    }

    @Override
    @Transactional
    public void eliminarCredencial(Long idUsuario) {
        log.info("[AUDIT idUsuario={}] Desactivando credencial (INACTIVO)", idUsuario);
        Credencial credencial = credencialRepository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> {
                    log.warn("[AUDIT idUsuario={}] Credencial no encontrada para eliminar", idUsuario);
                    return new IllegalArgumentException("Credencial no encontrada");
                });
        credencial.setEstadoCuenta("INACTIVO");
        credencialRepository.save(credencial);
        log.info("[AUDIT idUsuario={}] Credencial desactivada exitosamente", idUsuario);
    }

    private CredencialResponseDTO mapToResponse(Credencial credencial) {
        return new CredencialResponseDTO(
                credencial.getIdCredencial(),
                credencial.getIdUsuario(),
                credencial.getEmailLogin(),
                credencial.getEstadoCuenta(),
                credencial.getCreadoEn());
    }

    private List<String> obtenerRolesDesdeSecurity(Long idUsuario) {
        try {
            RolesUsuarioResponseDTO response = securityServiceClient.obtenerRoles(idUsuario);
            if (response.getRoles() == null || response.getRoles().isEmpty()) {
                return List.of("ROLE_CLIENTE");
            }
            return response.getRoles();
        } catch (Exception e) {
            log.warn("[AUDIT] No se pudieron obtener roles: {}", e.getMessage());
            return List.of("ROLE_CLIENTE");
        }
    }
}
