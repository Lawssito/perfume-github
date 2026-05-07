package com.auth_service.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.auth_service.dto.AuthRequestDto;
import com.auth_service.dto.AuthResponseDto;
import com.auth_service.exception.BadCredentialsException;
import com.auth_service.model.Credencial;
import com.auth_service.model.Estado;
import com.auth_service.repository.CredencialRepository;

@Service    
public class ServiceAuth {

    @Autowired
    private CredencialRepository repository;

    public AuthResponseDto login(AuthRequestDto request) {

        Credencial credencial = repository.findfindByUsrname(request.username())
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

        if (credencial.getEstado() != Estado.ACTIVO) {
            throw new BadCredentialsException("El usuario no está activo");
        }

        if (!credencial.getPassword().equals(request.password())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        String jwt = generarTokenJwt(credencial);

        credencial.setToken(jwt);
        repository.save(credencial);

        return new AuthResponseDto(jwt);
    }

    private String generarTokenJwt(Credencial credencial) {
        return "jwt-simulado-" + UUID.randomUUID().toString() + "-para-" + credencial.getUsername();
    }
}