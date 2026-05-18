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
        log.info("[CONTROLLER] POST /api/auth/credenciales idUsuario={}", request.getIdUsuario());
        authService.crearCredencial(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/credenciales")
    public ResponseEntity<List<CredencialResponseDTO>> listarCredenciales() {
        log.info("[CONTROLLER] GET /api/auth/credenciales");
        return ResponseEntity.ok(authService.listarCredenciales());
    }

    @GetMapping("/credenciales/usuario/{idUsuario}")
    public ResponseEntity<CredencialResponseDTO> obtenerPorUsuario(@PathVariable Long idUsuario) {
        log.info("[CONTROLLER] GET /api/auth/credenciales/usuario/{}", idUsuario);
        return ResponseEntity.ok(authService.obtenerPorIdUsuario(idUsuario));
    }

    @PutMapping("/credenciales/usuario/{idUsuario}/estado")
    public ResponseEntity<CredencialResponseDTO> actualizarEstado(
            @PathVariable Long idUsuario,
            @Valid @RequestBody ActualizarEstadoCuentaDTO dto) {
        log.info("[CONTROLLER] PUT /api/auth/credenciales/usuario/{}/estado", idUsuario);
        return ResponseEntity.ok(authService.actualizarEstadoCuenta(idUsuario, dto));
    }

    @DeleteMapping("/credenciales/usuario/{idUsuario}")
    public ResponseEntity<Void> eliminarCredencial(@PathVariable Long idUsuario) {
        log.info("[CONTROLLER] DELETE /api/auth/credenciales/usuario/{}", idUsuario);
        authService.eliminarCredencial(idUsuario);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("[CONTROLLER] POST /api/auth/login email={}", request.getEmail());
        return ResponseEntity.ok(authService.autenticarUsuario(request));
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenClaimsResponseDTO> validate(@Valid @RequestBody ValidateTokenRequestDTO request) {
        log.info("[CONTROLLER] POST /api/auth/validate");
        TokenClaimsResponseDTO response = authService.validarToken(request);
        return response.isValido()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}
