package com.auth_service.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.security.crypto.password.PasswordEncoder; // Necesitarás Spring Security
import org.springframework.stereotype.Service;
import com.auth_service.dto.AuthResponseDTO;
import com.auth_service.dto.LoginRequestDTO;
import com.auth_service.repository.CredencialRepository;
import com.auth_service.model.*;
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CredencialRepository credencialRepository;
    // private final PasswordEncoder passwordEncoder; -> Para comparar hashes con BCrypt

    public AuthResponseDTO autenticarUsuario(LoginRequestDTO request) {
        log.info("Iniciando intento de login para el email: {}", request.getEmail());

        // 1. Buscar la credencial por email
        Credencial credencial = credencialRepository.findByEmailLogin(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login fallido: Usuario no encontrado para el email {}", request.getEmail());
                    return new RuntimeException("Credenciales inválidas");
                });

        // 2. Validar estado de la cuenta
        if (!credencial.getEstadoCuenta().equals("ACTIVO")) {
            log.warn("Login fallido: Cuenta bloqueada o inactiva para el ID {}", credencial.getIdUsuario());
            throw new RuntimeException("Cuenta inactiva");
        }

        // 3. Validar contraseña (Aquí usarías passwordEncoder.matches() en la vida real)
        if (!request.getPassword().equals(credencial.getPasswordHash())) { // Comparación temporal
            log.error("Login fallido: Contraseña incorrecta para el email {}", request.getEmail());
            throw new RuntimeException("Credenciales inválidas");
        }

        log.info("Autenticación exitosa. Generando tokens para el usuario ID: {}", credencial.getIdUsuario());

        // 4. Generar Tokens (Aquí llamarías a tu utilidad de JWT que uses con la librería JJWT)
        String jwtMock = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mockToken..." + credencial.getIdUsuario();
        String refreshMock = "refresh-token-hash-12345";

        // 5. Retornar la respuesta
        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(jwtMock);
        response.setRefreshToken(refreshMock);
        response.setIdUsuario(credencial.getIdUsuario());
        response.setMensaje("Login exitoso");

        return response;
    }
}
