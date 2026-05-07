package com.auth_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.auth_service.dto.AuthRequestDto;
import com.auth_service.dto.AuthResponseDto;
import com.auth_service.service.ServiceAuth;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private ServiceAuth authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody AuthRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
