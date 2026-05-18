package com.security_service.controller;

import com.security_service.dto.*;
import com.security_service.service.SecurityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;

    @PostMapping("/api/usuario-roles")
    public ResponseEntity<RolesUsuarioResponseDTO> asignarRol(@Valid @RequestBody AsignarRolRequestDTO request) {
        log.info("[CONTROLLER] POST /api/usuario-roles usuario={} rol={}", request.getIdUsuario(), request.getRolNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(securityService.asignarRol(request));
    }

    @GetMapping("/api/usuario-roles/{idUsuario}")
    public ResponseEntity<RolesUsuarioResponseDTO> obtenerRoles(@PathVariable Long idUsuario) {
        log.info("[CONTROLLER] GET /api/usuario-roles/{}", idUsuario);
        return ResponseEntity.ok(securityService.obtenerRoles(idUsuario));
    }

    @DeleteMapping("/api/usuario-roles/{idUsuario}/{rolNombre}")
    public ResponseEntity<Void> revocarRol(@PathVariable Long idUsuario, @PathVariable String rolNombre) {
        log.info("[CONTROLLER] DELETE /api/usuario-roles/{}/{}", idUsuario, rolNombre);
        securityService.revocarRol(idUsuario, rolNombre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/security/validar-permiso")
    public ResponseEntity<ValidacionResponseDTO> validarPermiso(@Valid @RequestBody ValidarAccesoRequestDTO request) {
        log.info("[CONTROLLER] POST /api/security/validar-permiso request={}", request);
        ValidacionResponseDTO resultado = securityService.validarPermiso(request);
        return resultado.isValido()
                ? ResponseEntity.ok(resultado)
                : ResponseEntity.status(HttpStatus.FORBIDDEN).body(resultado);
    }

    @GetMapping("/api/security/roles")
    public ResponseEntity<List<RolResponseDTO>> listarRoles() {
        log.info("[CONTROLLER] GET /api/security/roles");
        return ResponseEntity.ok(securityService.listarRoles());
    }

    @PostMapping("/api/security/roles")
    public ResponseEntity<RolResponseDTO> crearRol(@Valid @RequestBody RolRequestDTO dto) {
        log.info("[CONTROLLER] POST /api/security/roles nombre={}", dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(securityService.crearRol(dto));
    }

    @GetMapping("/api/security/roles/{id}")
    public ResponseEntity<RolResponseDTO> obtenerRol(@PathVariable Long id) {
        log.info("[CONTROLLER] GET /api/security/roles/{}", id);
        return ResponseEntity.ok(securityService.obtenerRol(id));
    }

    @PutMapping("/api/security/roles/{id}")
    public ResponseEntity<RolResponseDTO> actualizarRol(@PathVariable Long id, @Valid @RequestBody RolRequestDTO dto) {
        log.info("[CONTROLLER] PUT /api/security/roles/{}", id);
        return ResponseEntity.ok(securityService.actualizarRol(id, dto));
    }

    @DeleteMapping("/api/security/roles/{id}")
    public ResponseEntity<Void> eliminarRol(@PathVariable Long id) {
        log.info("[CONTROLLER] DELETE /api/security/roles/{}", id);
        securityService.eliminarRol(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/security/permisos")
    public ResponseEntity<List<PermisoResponseDTO>> listarPermisos() {
        log.info("[CONTROLLER] GET /api/security/permisos");
        return ResponseEntity.ok(securityService.listarPermisos());
    }

    @PostMapping("/api/security/permisos")
    public ResponseEntity<PermisoResponseDTO> crearPermiso(@Valid @RequestBody PermisoRequestDTO dto) {
        log.info("[CONTROLLER] POST /api/security/permisos nombre={}", dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(securityService.crearPermiso(dto));
    }

    @DeleteMapping("/api/security/permisos/{id}")
    public ResponseEntity<Void> eliminarPermiso(@PathVariable Long id) {
        log.info("[CONTROLLER] DELETE /api/security/permisos/{}", id);
        securityService.eliminarPermiso(id);
        return ResponseEntity.noContent().build();
    }
}
