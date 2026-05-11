package com.user_service.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.user_service.dto.direccionDTO;
import com.user_service.dto.usuarioDTO;
import com.user_service.model.Direccion;
import com.user_service.model.Usuario;
import com.user_service.service.UsuarioService;

@Slf4j
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<Usuario> crear(@Valid @RequestBody usuarioDTO dto) {
        log.info("Recibida peticion POST para crear usuario: {}", dto);
        
        Usuario resultado = usuarioService.crearUsuario(dto);
        
        log.info("Usuario creado exitosamente con ID: {}", resultado.getIdUsuario());
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/{id}/direcciones")
    public ResponseEntity<Direccion> agregarDireccion(
            @PathVariable Long id, 
            @Valid @RequestBody direccionDTO dto) {
        
        log.info("Recibida peticion POST para agregar dirección al usuario ID {}: {}", id, dto);
        
        Direccion resultado = usuarioService.agregarDireccion(id, dto);
        
        log.info("Dirección creada exitosamente y asociada al usuario ID: {}", id);
        return ResponseEntity.ok(resultado);
    }
}