package com.security_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.security_service.dto.ValidacionResponseDTO;
import com.security_service.dto.ValidarAccesoRequestDTO;
import com.security_service.service.ServiceSecurity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {

    private final ServiceSecurity securityService;

    @PostMapping("/validar")
    public ResponseEntity<ValidacionResponseDTO> validarAcceso(@Valid @RequestBody ValidarAccesoRequestDTO request) {
        log.info("Gateway solicitando validación de acceso...");
        
        ValidacionResponseDTO resultado = securityService.validarTokenYPermiso(request);

        if (resultado.isValido()) {
            return ResponseEntity.ok(resultado);
        } else {
            // Retorna 403 Forbidden si el token es válido pero no tiene permisos, o 401 si es inválido
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resultado);
        }
    }
}