package com.auth_service.controller;

import com.auth_service.dto.*;
import com.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/credenciales")
    public ResponseEntity<Void> crearCredencial(@Valid @RequestBody CrearCredencialRequestDTO request) {
        log.info("[AUDIT idUsuario={}] POST /api/auth/credenciales", request.getIdUsuario());
        authService.crearCredencial(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/credenciales")
    public ResponseEntity<List<CredencialResponseDTO>> listarCredenciales() {
        log.info("[AUDIT] GET /api/auth/credenciales — listando todas");
        return ResponseEntity.ok(authService.listarCredenciales());
    }

    @GetMapping("/credenciales/usuario/{idUsuario}")
    public ResponseEntity<CredencialResponseDTO> obtenerPorUsuario(@PathVariable Long idUsuario) {
        log.info("[AUDIT idUsuario={}] GET /api/auth/credenciales/usuario/{}", idUsuario, idUsuario);
        return ResponseEntity.ok(authService.obtenerPorIdUsuario(idUsuario));
    }

    @PutMapping("/credenciales/usuario/{idUsuario}/estado")
    public ResponseEntity<CredencialResponseDTO> actualizarEstado(
            @PathVariable Long idUsuario,
            @Valid @RequestBody ActualizarEstadoCuentaDTO dto) {
        log.info("[AUDIT idUsuario={}] PUT estado → {}", idUsuario, dto.getEstadoCuenta());
        return ResponseEntity.ok(authService.actualizarEstadoCuenta(idUsuario, dto));
    }

    @DeleteMapping("/credenciales/usuario/{idUsuario}")
    public ResponseEntity<Void> eliminarCredencial(@PathVariable Long idUsuario) {
        log.info("[AUDIT idUsuario={}] DELETE credencial", idUsuario);
        authService.eliminarCredencial(idUsuario);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("[AUDIT email={}] POST /api/auth/login", request.getEmail());
        AuthResponseDTO response = authService.autenticarUsuario(request);
        log.info("[AUDIT email={}] Login exitoso → idUsuario={}", request.getEmail(), response.getIdUsuario());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenClaimsResponseDTO> validate(@Valid @RequestBody ValidateTokenRequestDTO request) {
        log.info("[AUDIT] POST /api/auth/validate");
        TokenClaimsResponseDTO response = authService.validarToken(request);
        if (response.isValido()) {
            log.info("[AUDIT email={}] Token valido para idUsuario={}", response.getEmail(), response.getIdUsuario());
        } else {
            log.warn("[AUDIT] Token invalido: {}", response.getMensaje());
        }
        return response.isValido()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        log.info("[AUDIT] POST /api/auth/refresh");
        AuthResponseDTO response = authService.refreshToken(request.getRefreshToken());
        log.info("[AUDIT] Token renovado exitosamente");
        return ResponseEntity.ok(response);
    }
}
