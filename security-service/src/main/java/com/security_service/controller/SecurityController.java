package com.security_service.controller;

import com.security_service.dto.*;
import com.security_service.service.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.security_service.exception.ForbiddenException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;

    @SuppressWarnings("unchecked")
    private void exigirAdmin() {
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        // Internal API key call → confiar
        if (req.getHeader("X-Internal-Api-Key") != null) return;
        // External call → verificar rol ADMIN en los atributos del filter
        java.util.List<String> roles = (java.util.List<String>) req.getAttribute("roles");
        if (roles == null || !roles.contains("ROLE_ADMIN")) {
            throw new ForbiddenException("No tienes permisos de administrador para realizar esta accion");
        }
    }

    @PostMapping("/api/usuario-roles")
    public ResponseEntity<RolesUsuarioResponseDTO> asignarRol(@Valid @RequestBody AsignarRolRequestDTO request) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/usuario-roles usuario={} rol={}", request.getIdUsuario(), request.getRolNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(securityService.asignarRol(request));
    }

    @GetMapping("/api/usuario-roles/{idUsuario}")
    public ResponseEntity<RolesUsuarioResponseDTO> obtenerRoles(@PathVariable Long idUsuario) {
        exigirAdmin();
        log.info("[AUDIT] GET /api/usuario-roles/{}", idUsuario);
        return ResponseEntity.ok(securityService.obtenerRoles(idUsuario));
    }

    @DeleteMapping("/api/usuario-roles/{idUsuario}/{rolNombre}")
    public ResponseEntity<Void> revocarRol(@PathVariable Long idUsuario, @PathVariable String rolNombre) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/usuario-roles/{}/{}", idUsuario, rolNombre);
        securityService.revocarRol(idUsuario, rolNombre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/security/validar-permiso")
    public ResponseEntity<ValidacionResponseDTO> validarPermiso(@Valid @RequestBody ValidarAccesoRequestDTO request) {
        log.info("[AUDIT] POST /api/security/validar-permiso request={}", request);
        ValidacionResponseDTO resultado = securityService.validarPermiso(request);
        return resultado.isValido()
                ? ResponseEntity.ok(resultado)
                : ResponseEntity.status(HttpStatus.FORBIDDEN).body(resultado);
    }

    @GetMapping("/api/security/roles")
    public ResponseEntity<List<RolResponseDTO>> listarRoles() {
        exigirAdmin();
        log.info("[AUDIT] GET /api/security/roles");
        return ResponseEntity.ok(securityService.listarRoles());
    }

    @PostMapping("/api/security/roles")
    public ResponseEntity<RolResponseDTO> crearRol(@Valid @RequestBody RolRequestDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/security/roles nombre={}", dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(securityService.crearRol(dto));
    }

    @GetMapping("/api/security/roles/{id}")
    public ResponseEntity<RolResponseDTO> obtenerRol(@PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] GET /api/security/roles/{}", id);
        return ResponseEntity.ok(securityService.obtenerRol(id));
    }

    @PutMapping("/api/security/roles/{id}")
    public ResponseEntity<RolResponseDTO> actualizarRol(@PathVariable Long id, @Valid @RequestBody RolRequestDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] PUT /api/security/roles/{}", id);
        return ResponseEntity.ok(securityService.actualizarRol(id, dto));
    }

    @DeleteMapping("/api/security/roles/{id}")
    public ResponseEntity<Void> eliminarRol(@PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/security/roles/{}", id);
        securityService.eliminarRol(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/security/permisos")
    public ResponseEntity<List<PermisoResponseDTO>> listarPermisos() {
        exigirAdmin();
        log.info("[AUDIT] GET /api/security/permisos");
        return ResponseEntity.ok(securityService.listarPermisos());
    }

    @PostMapping("/api/security/permisos")
    public ResponseEntity<PermisoResponseDTO> crearPermiso(@Valid @RequestBody PermisoRequestDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/security/permisos nombre={}", dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(securityService.crearPermiso(dto));
    }

    @DeleteMapping("/api/security/permisos/{id}")
    public ResponseEntity<Void> eliminarPermiso(@PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/security/permisos/{}", id);
        securityService.eliminarPermiso(id);
        return ResponseEntity.noContent().build();
    }
}
