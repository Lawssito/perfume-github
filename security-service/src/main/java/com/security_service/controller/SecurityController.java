package com.security_service.controller;

import com.security_service.dto.*;
import com.security_service.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Seguridad y Roles", description = "Gestión de roles, permisos y validación de acceso")
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
    @Operation(summary = "Asignar rol a usuario", description = "Asigna un rol a un usuario específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rol asignado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos de administrador")
    })
    public ResponseEntity<RolesUsuarioResponseDTO> asignarRol(@Valid @RequestBody AsignarRolRequestDTO request) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/usuario-roles usuario={} rol={}", request.getIdUsuario(), request.getRolNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(securityService.asignarRol(request));
    }

    @GetMapping("/api/usuario-roles/{idUsuario}")
    @Operation(summary = "Obtener roles de usuario", description = "Obtiene todos los roles asignados a un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles del usuario obtenidos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<RolesUsuarioResponseDTO> obtenerRoles(@Parameter(description = "ID del usuario", example = "1") @PathVariable Long idUsuario) {
        exigirAdmin();
        log.info("[AUDIT] GET /api/usuario-roles/{}", idUsuario);
        return ResponseEntity.ok(securityService.obtenerRoles(idUsuario));
    }

    @DeleteMapping("/api/usuario-roles/{idUsuario}/{rolNombre}")
    @Operation(summary = "Revocar rol de usuario", description = "Elimina un rol específico asignado a un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Rol revocado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Rol o usuario no encontrado")
    })
    public ResponseEntity<Void> revocarRol(@Parameter(description = "ID del usuario", example = "1") @PathVariable Long idUsuario, @Parameter(description = "Nombre del rol", example = "ROLE_USER") @PathVariable String rolNombre) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/usuario-roles/{}/{}", idUsuario, rolNombre);
        securityService.revocarRol(idUsuario, rolNombre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/security/validar-permiso")
    @Operation(summary = "Validar permiso de acceso", description = "Verifica si un usuario tiene un permiso específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permiso validado - retorna resultado de la validación"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<ValidacionResponseDTO> validarPermiso(@Valid @RequestBody ValidarAccesoRequestDTO request) {
        log.info("[AUDIT] POST /api/security/validar-permiso request={}", request);
        ValidacionResponseDTO resultado = securityService.validarPermiso(request);
        return resultado.isValido()
                ? ResponseEntity.ok(resultado)
                : ResponseEntity.status(HttpStatus.FORBIDDEN).body(resultado);
    }

    @GetMapping("/api/security/roles")
    @Operation(summary = "Listar roles", description = "Obtiene todos los roles registrados en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de roles obtenida exitosamente")
    })
    public ResponseEntity<List<RolResponseDTO>> listarRoles() {
        exigirAdmin();
        log.info("[AUDIT] GET /api/security/roles");
        return ResponseEntity.ok(securityService.listarRoles());
    }

    @PostMapping("/api/security/roles")
    @Operation(summary = "Crear rol", description = "Crea un nuevo rol en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rol creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<RolResponseDTO> crearRol(@Valid @RequestBody RolRequestDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/security/roles nombre={}", dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(securityService.crearRol(dto));
    }

    @GetMapping("/api/security/roles/{id}")
    @Operation(summary = "Obtener rol por ID", description = "Obtiene los detalles de un rol específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol encontrado"),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    public ResponseEntity<RolResponseDTO> obtenerRol(@Parameter(description = "ID del rol", example = "1") @PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] GET /api/security/roles/{}", id);
        return ResponseEntity.ok(securityService.obtenerRol(id));
    }

    @PutMapping("/api/security/roles/{id}")
    @Operation(summary = "Actualizar rol", description = "Actualiza el nombre de un rol existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    public ResponseEntity<RolResponseDTO> actualizarRol(@Parameter(description = "ID del rol", example = "1") @PathVariable Long id, @Valid @RequestBody RolRequestDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] PUT /api/security/roles/{}", id);
        return ResponseEntity.ok(securityService.actualizarRol(id, dto));
    }

    @DeleteMapping("/api/security/roles/{id}")
    @Operation(summary = "Eliminar rol", description = "Elimina un rol del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Rol eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    public ResponseEntity<Void> eliminarRol(@Parameter(description = "ID del rol", example = "1") @PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/security/roles/{}", id);
        securityService.eliminarRol(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/security/permisos")
    @Operation(summary = "Listar permisos", description = "Obtiene todos los permisos registrados en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de permisos obtenida exitosamente")
    })
    public ResponseEntity<List<PermisoResponseDTO>> listarPermisos() {
        exigirAdmin();
        log.info("[AUDIT] GET /api/security/permisos");
        return ResponseEntity.ok(securityService.listarPermisos());
    }

    @PostMapping("/api/security/permisos")
    @Operation(summary = "Crear permiso", description = "Crea un nuevo permiso en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Permiso creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<PermisoResponseDTO> crearPermiso(@Valid @RequestBody PermisoRequestDTO dto) {
        exigirAdmin();
        log.info("[AUDIT] POST /api/security/permisos nombre={}", dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(securityService.crearPermiso(dto));
    }

    @DeleteMapping("/api/security/permisos/{id}")
    @Operation(summary = "Eliminar permiso", description = "Elimina un permiso del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Permiso eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Permiso no encontrado")
    })
    public ResponseEntity<Void> eliminarPermiso(@Parameter(description = "ID del permiso", example = "1") @PathVariable Long id) {
        exigirAdmin();
        log.info("[AUDIT] DELETE /api/security/permisos/{}", id);
        securityService.eliminarPermiso(id);
        return ResponseEntity.noContent().build();
    }
}
