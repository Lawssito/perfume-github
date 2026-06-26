package com.user_service.controller;

import com.user_service.dto.*;
import com.user_service.service.UsuarioService;
import com.user_service.service.security.AutorizacionUsuarioService;
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
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AutorizacionUsuarioService autorizacionUsuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistroUsuarioRequestDTO dto) {
        log.info("[AUDIT email={}] POST /api/usuarios — registro", dto.getEmail());
        UsuarioResponseDTO respuesta = usuarioService.registrarUsuario(dto);
        log.info("[AUDIT idUsuario={}] Usuario creado exitosamente", respuesta.getIdUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
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
    public ResponseEntity<UsuarioResponseDTO> obtenerPorId(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UsuarioAutenticadoDTO auth = autorizacionUsuarioService.validarSesion(authorization);
        autorizacionUsuarioService.exigirMismoUsuarioOAdmin(auth, id);
        log.info("[AUDIT email={} idUsuario={}] GET /api/usuarios/{}", auth.getEmail(), auth.getIdUsuario(), id);
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioDTO dto,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UsuarioAutenticadoDTO auth = autorizacionUsuarioService.validarSesion(authorization);
        autorizacionUsuarioService.exigirMismoUsuarioOAdmin(auth, id);
        log.info("[AUDIT email={} idUsuario={}] PUT /api/usuarios/{}}", auth.getEmail(), auth.getIdUsuario(), id);
        return ResponseEntity.ok(usuarioService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UsuarioAutenticadoDTO auth = autorizacionUsuarioService.validarSesion(authorization);
        autorizacionUsuarioService.exigirAdmin(auth);
        log.info("[AUDIT email={} idUsuario={}] DELETE /api/usuarios/{} — baja logica", auth.getEmail(), auth.getIdUsuario(), id);
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/direcciones")
    public ResponseEntity<DireccionResponseDTO> agregarDireccion(
            @PathVariable Long id,
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
    public ResponseEntity<List<DireccionResponseDTO>> listarDirecciones(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UsuarioAutenticadoDTO auth = autorizacionUsuarioService.validarSesion(authorization);
        autorizacionUsuarioService.exigirMismoUsuarioOAdmin(auth, id);
        log.info("[AUDIT email={}] GET /api/usuarios/{}/direcciones", auth.getEmail(), id);
        return ResponseEntity.ok(usuarioService.listarDirecciones(id));
    }

    @DeleteMapping("/{id}/direcciones/{idDireccion}")
    public ResponseEntity<Void> eliminarDireccion(
            @PathVariable Long id,
            @PathVariable Long idDireccion,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UsuarioAutenticadoDTO auth = autorizacionUsuarioService.validarSesion(authorization);
        autorizacionUsuarioService.exigirMismoUsuarioOAdmin(auth, id);
        log.info("[AUDIT email={}] DELETE /api/usuarios/{}/direcciones/{}", auth.getEmail(), id, idDireccion);
        usuarioService.eliminarDireccion(id, idDireccion);
        return ResponseEntity.noContent().build();
    }
}
