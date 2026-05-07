package com.security_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.security_service.dto.AccesoDTO;
import com.security_service.service.SecurityService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/security")
public class SecurityController {

    @Autowired
    private SecurityService securityService;

    // Endpoint: GET /api/v1/security/roles/ADMIN/permisos
    @GetMapping("/roles/{nombreRol}/permisos")
    public ResponseEntity<List<AccesoDTO>> consultarPermisos(@PathVariable String nombreRol) {
        List<AccesoDTO> permisos = securityService.obtenerPermisosPorRol(nombreRol);
        return ResponseEntity.ok(permisos);
    }
}
