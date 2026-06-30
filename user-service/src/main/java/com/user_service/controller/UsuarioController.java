package com.user_service.controller;

import com.user_service.dto.*;
import com.user_service.service.UsuarioService;
import com.user_service.service.security.AutorizacionUsuarioService;
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
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AutorizacionUsuarioService autorizacionUsuarioService;

    @PostMapping
    @Operation(summary = "Registrar usuario", description = "Crea un nuevo usuario con credenciales de acceso")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos en la solicitud"),
            @ApiResponse(responseCode = "409", description = "El email ya está registrado")
    })
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistroUsuarioRequestDTO dto) {
        log.info("[AUDIT email={}] POST /api/usuarios — registro", dto.getEmail());
        UsuarioResponseDTO respuesta = usuarioService.registrarUsuario(dto);
        log.info("[AUDIT idUsuario={}] Usuario creado exitosamente", respuesta.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Obtiene todos los usuarios registrados (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos de administrador")
    })
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UsuarioAutenticadoDTO auth = autorizacionUsuarioService.validarSesion(authorization);
        autorizacionUsuarioService.exigirAdmin(auth);
        log.info("[AUDIT email={} idUsuario={}] GET /api/usuarios — listado admin", auth.getEmail(), auth.getIdUsuario());
        List<UsuarioResponseDTO> lista = usuarioService.listarTodos();
        log.info("[AUDIT] Retornando {} usuarios", lista.size());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID", description = "Obtiene los datos de un usuario por su ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(
            @Parameter(description = "ID del usuario", example = "1") @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UsuarioAutenticadoDTO auth = autorizacionUsuarioService.validarSesion(authorization);
        autorizacionUsuarioService.exigirMismoUsuarioOAdmin(auth, id);
        log.info("[AUDIT email={} idUsuario={}] GET /api/usuarios/{}", auth.getEmail(), auth.getIdUsuario(), id);
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza los datos de un usuario existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<UsuarioResponseDTO> actualizar(
            @Parameter(description = "ID del usuario", example = "1") @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioDTO dto,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UsuarioAutenticadoDTO auth = autorizacionUsuarioService.validarSesion(authorization);
        autorizacionUsuarioService.exigirMismoUsuarioOAdmin(auth, id);
        log.info("[AUDIT email={} idUsuario={}] PUT /api/usuarios/{}}", auth.getEmail(), auth.getIdUsuario(), id);
        return ResponseEntity.ok(usuarioService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario (baja lógica)", description = "Marca un usuario como eliminado (solo admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuario eliminado exitosamente"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos de administrador"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del usuario", example = "1") @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UsuarioAutenticadoDTO auth = autorizacionUsuarioService.validarSesion(authorization);
        autorizacionUsuarioService.exigirAdmin(auth);
        log.info("[AUDIT email={} idUsuario={}] DELETE /api/usuarios/{} — baja logica", auth.getEmail(), auth.getIdUsuario(), id);
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/direcciones")
    @Operation(summary = "Agregar dirección", description = "Agrega una nueva dirección a un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Dirección creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<DireccionResponseDTO> agregarDireccion(
            @Parameter(description = "ID del usuario", example = "1") @PathVariable Long id,
            @Valid @RequestBody DireccionDTO dto,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UsuarioAutenticadoDTO auth = autorizacionUsuarioService.validarSesion(authorization);
        autorizacionUsuarioService.exigirMismoUsuarioOAdmin(auth, id);
        log.info("[AUDIT email={}] POST /api/usuarios/{}/direcciones", auth.getEmail(), id);
        DireccionResponseDTO respuesta = usuarioService.agregarDireccion(id, dto);
        log.info("[AUDIT email={}] Direccion {} creada para usuario {}", auth.getEmail(), respuesta.getIdDireccion(), id);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{id}/direcciones")
    @Operation(summary = "Listar direcciones", description = "Obtiene todas las direcciones de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de direcciones obtenida")
    })
    public ResponseEntity<List<DireccionResponseDTO>> listarDirecciones(
            @Parameter(description = "ID del usuario", example = "1") @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UsuarioAutenticadoDTO auth = autorizacionUsuarioService.validarSesion(authorization);
        autorizacionUsuarioService.exigirMismoUsuarioOAdmin(auth, id);
        log.info("[AUDIT email={}] GET /api/usuarios/{}/direcciones", auth.getEmail(), id);
        return ResponseEntity.ok(usuarioService.listarDirecciones(id));
    }

    @DeleteMapping("/{id}/direcciones/{idDireccion}")
    @Operation(summary = "Eliminar dirección", description = "Elimina una dirección específica de un usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Dirección eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Dirección no encontrada")
    })
    public ResponseEntity<Void> eliminarDireccion(
            @Parameter(description = "ID del usuario", example = "1") @PathVariable Long id,
            @Parameter(description = "ID de la dirección", example = "1") @PathVariable Long idDireccion,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UsuarioAutenticadoDTO auth = autorizacionUsuarioService.validarSesion(authorization);
        autorizacionUsuarioService.exigirMismoUsuarioOAdmin(auth, id);
        log.info("[AUDIT email={}] DELETE /api/usuarios/{}/direcciones/{}", auth.getEmail(), id, idDireccion);
        usuarioService.eliminarDireccion(id, idDireccion);
        return ResponseEntity.noContent().build();
    }
}
