package com.auth_service.controller;

import com.auth_service.dto.*;
import com.auth_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de login, registro y refresh token")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/credenciales")
    @Operation(summary = "Crear credencial", description = "Crea un nuevo par de credenciales (email+password) para un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Credencial creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos en la solicitud"),
            @ApiResponse(responseCode = "409", description = "El usuario ya tiene credenciales")
    })
    public ResponseEntity<Void> crearCredencial(@Valid @RequestBody CrearCredencialRequestDTO request) {
        log.info("[AUDIT idUsuario={}] POST /api/auth/credenciales", request.getIdUsuario());
        authService.crearCredencial(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/credenciales")
    @Operation(summary = "Listar credenciales", description = "Obtiene todas las credenciales registradas (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de credenciales obtenida exitosamente")
    })
    public ResponseEntity<List<CredencialResponseDTO>> listarCredenciales() {
        log.info("[AUDIT] GET /api/auth/credenciales — listando todas");
        return ResponseEntity.ok(authService.listarCredenciales());
    }

    @GetMapping("/credenciales/usuario/{idUsuario}")
    @Operation(summary = "Obtener credencial por usuario", description = "Obtiene la credencial asociada a un usuario por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credencial encontrada"),
            @ApiResponse(responseCode = "404", description = "Credencial no encontrada para el usuario")
    })
    public ResponseEntity<CredencialResponseDTO> obtenerPorUsuario(@Parameter(description = "ID del usuario", example = "1") @PathVariable Long idUsuario) {
        log.info("[AUDIT idUsuario={}] GET /api/auth/credenciales/usuario/{}", idUsuario, idUsuario);
        return ResponseEntity.ok(authService.obtenerPorIdUsuario(idUsuario));
    }

    @PutMapping("/credenciales/usuario/{idUsuario}/estado")
    @Operation(summary = "Actualizar estado de cuenta", description = "Activa o desactiva la cuenta de un usuario (ACTIVO/INACTIVO)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido"),
            @ApiResponse(responseCode = "404", description = "Credencial no encontrada")
    })
    public ResponseEntity<CredencialResponseDTO> actualizarEstado(
            @Parameter(description = "ID del usuario", example = "1") @PathVariable Long idUsuario,
            @Valid @RequestBody ActualizarEstadoCuentaDTO dto) {
        log.info("[AUDIT idUsuario={}] PUT estado → {}", idUsuario, dto.getEstadoCuenta());
        return ResponseEntity.ok(authService.actualizarEstadoCuenta(idUsuario, dto));
    }

    @DeleteMapping("/credenciales/usuario/{idUsuario}")
    @Operation(summary = "Eliminar credencial", description = "Elimina las credenciales de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Credencial eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Credencial no encontrada")
    })
    public ResponseEntity<Void> eliminarCredencial(@Parameter(description = "ID del usuario", example = "1") @PathVariable Long idUsuario) {
        log.info("[AUDIT idUsuario={}] DELETE credencial", idUsuario);
        authService.eliminarCredencial(idUsuario);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario con email y password, devuelve tokens JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa, tokens generados"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        log.info("[AUDIT email={}] POST /api/auth/login", request.getEmail());
        AuthResponseDTO response = authService.autenticarUsuario(request);
        log.info("[AUDIT email={}] Login exitoso → idUsuario={}", request.getEmail(), response.getIdUsuario());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validar token JWT", description = "Verifica si un access token es válido y devuelve los claims")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token válido - retorna claims del usuario"),
            @ApiResponse(responseCode = "401", description = "Token inválido o expirado")
    })
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
    @Operation(summary = "Renovar token JWT", description = "Genera un nuevo access token a partir de un refresh token válido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token renovado exitosamente"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado")
    })
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO request) {
        log.info("[AUDIT] POST /api/auth/refresh");
        AuthResponseDTO response = authService.refreshToken(request.getRefreshToken());
        log.info("[AUDIT] Token renovado exitosamente");
        return ResponseEntity.ok(response);
    }
}
