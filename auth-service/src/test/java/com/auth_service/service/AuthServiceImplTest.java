package com.auth_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.auth_service.client.SecurityServiceClient;
import com.auth_service.dto.ActualizarEstadoCuentaDTO;
import com.auth_service.dto.AuthResponseDTO;
import com.auth_service.dto.CrearCredencialRequestDTO;
import com.auth_service.dto.CredencialResponseDTO;
import com.auth_service.dto.LoginRequestDTO;
import com.auth_service.dto.RolesUsuarioResponseDTO;
import com.auth_service.dto.TokenClaimsResponseDTO;
import com.auth_service.dto.ValidateTokenRequestDTO;
import com.auth_service.model.Credencial;
import com.auth_service.model.RefreshToken;
import com.auth_service.repository.CredencialRepository;
import com.auth_service.repository.RefreshTokenRepository;
import com.auth_service.service.impl.AuthServiceImpl;

import io.jsonwebtoken.Claims;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private CredencialRepository credencialRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityServiceClient securityServiceClient;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void crearCredencial_success() {
        CrearCredencialRequestDTO dto = new CrearCredencialRequestDTO();
        dto.setIdUsuario(1L);
        dto.setEmail("test@email.com");
        dto.setPassword("password123");

        when(credencialRepository.existsByEmailLogin(dto.getEmail())).thenReturn(false);
        when(credencialRepository.existsByIdUsuario(dto.getIdUsuario())).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encoded");

        authService.crearCredencial(dto);

        verify(credencialRepository).existsByEmailLogin(dto.getEmail());
        verify(credencialRepository).existsByIdUsuario(dto.getIdUsuario());
        verify(passwordEncoder).encode(dto.getPassword());
        verify(credencialRepository).save(any(Credencial.class));
    }

    @Test
    void crearCredencial_emailDuplicate_throwsException() {
        CrearCredencialRequestDTO dto = new CrearCredencialRequestDTO();
        dto.setIdUsuario(1L);
        dto.setEmail("test@email.com");
        dto.setPassword("password123");

        when(credencialRepository.existsByEmailLogin(dto.getEmail())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> authService.crearCredencial(dto));
        verify(credencialRepository, never()).existsByIdUsuario(any());
        verify(credencialRepository, never()).save(any());
    }

    @Test
    void crearCredencial_usuarioDuplicate_throwsException() {
        CrearCredencialRequestDTO dto = new CrearCredencialRequestDTO();
        dto.setIdUsuario(1L);
        dto.setEmail("test@email.com");
        dto.setPassword("password123");

        when(credencialRepository.existsByEmailLogin(dto.getEmail())).thenReturn(false);
        when(credencialRepository.existsByIdUsuario(dto.getIdUsuario())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> authService.crearCredencial(dto));
        verify(credencialRepository, never()).save(any());
    }

    @Test
    void autenticarUsuario_success() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@email.com");
        request.setPassword("password123");

        Credencial credencial = Credencial.builder()
                .idCredencial(1L)
                .idUsuario(1L)
                .emailLogin("test@email.com")
                .passwordHash("encoded")
                .estadoCuenta("ACTIVO")
                .build();

        when(credencialRepository.findByEmailLogin(request.getEmail())).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches(request.getPassword(), credencial.getPasswordHash())).thenReturn(true);

        RolesUsuarioResponseDTO rolesResponse = new RolesUsuarioResponseDTO();
        rolesResponse.setIdUsuario(1L);
        rolesResponse.setRoles(List.of("ROLE_USER"));
        when(securityServiceClient.obtenerRoles(credencial.getIdUsuario())).thenReturn(rolesResponse);
        when(jwtService.generarToken(credencial.getEmailLogin(), credencial.getIdUsuario(), List.of("ROLE_USER")))
                .thenReturn("jwt-token");

        AuthResponseDTO response = authService.autenticarUsuario(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertNotNull(response.getRefreshToken());
        assertEquals(1L, response.getIdUsuario());
        assertEquals("test@email.com", response.getEmail());
        assertEquals(List.of("ROLE_USER"), response.getRoles());
        assertEquals("Login exitoso", response.getMensaje());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void autenticarUsuario_emailNotFound_throwsException() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("notfound@email.com");
        request.setPassword("password123");

        when(credencialRepository.findByEmailLogin(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.autenticarUsuario(request));
    }

    @Test
    void autenticarUsuario_inactiveAccount_throwsException() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@email.com");
        request.setPassword("password123");

        Credencial credencial = Credencial.builder()
                .idCredencial(1L)
                .idUsuario(1L)
                .emailLogin("test@email.com")
                .passwordHash("encoded")
                .estadoCuenta("INACTIVO")
                .build();

        when(credencialRepository.findByEmailLogin(request.getEmail())).thenReturn(Optional.of(credencial));

        assertThrows(IllegalStateException.class, () -> authService.autenticarUsuario(request));
    }

    @Test
    void autenticarUsuario_wrongPassword_throwsException() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@email.com");
        request.setPassword("wrong");

        Credencial credencial = Credencial.builder()
                .idCredencial(1L)
                .idUsuario(1L)
                .emailLogin("test@email.com")
                .passwordHash("encoded")
                .estadoCuenta("ACTIVO")
                .build();

        when(credencialRepository.findByEmailLogin(request.getEmail())).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches(request.getPassword(), credencial.getPasswordHash())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.autenticarUsuario(request));
    }

    @Test
    void autenticarUsuario_rolesFallbackOnError() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("test@email.com");
        request.setPassword("password123");

        Credencial credencial = Credencial.builder()
                .idCredencial(1L)
                .idUsuario(1L)
                .emailLogin("test@email.com")
                .passwordHash("encoded")
                .estadoCuenta("ACTIVO")
                .build();

        when(credencialRepository.findByEmailLogin(request.getEmail())).thenReturn(Optional.of(credencial));
        when(passwordEncoder.matches(request.getPassword(), credencial.getPasswordHash())).thenReturn(true);
        when(securityServiceClient.obtenerRoles(credencial.getIdUsuario())).thenThrow(new RuntimeException("timeout"));
        when(jwtService.generarToken(credencial.getEmailLogin(), credencial.getIdUsuario(), List.of("ROLE_CLIENTE")))
                .thenReturn("jwt-token");

        AuthResponseDTO response = authService.autenticarUsuario(request);

        assertEquals(List.of("ROLE_CLIENTE"), response.getRoles());
        assertEquals("Login exitoso", response.getMensaje());
    }

    @Test
    void refreshToken_success() {
        String rawToken = "some-refresh-token-value";
        String hash = sha256(rawToken);

        Credencial credencial = Credencial.builder()
                .idCredencial(1L)
                .idUsuario(1L)
                .emailLogin("test@email.com")
                .passwordHash("encoded")
                .estadoCuenta("ACTIVO")
                .build();

        RefreshToken stored = RefreshToken.builder()
                .idRefreshTk(1L)
                .credencial(credencial)
                .tokenHash(hash)
                .expiraEn(LocalDateTime.now().plusDays(1))
                .revocado(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        RolesUsuarioResponseDTO rolesResponse = new RolesUsuarioResponseDTO();
        rolesResponse.setIdUsuario(1L);
        rolesResponse.setRoles(List.of("ROLE_USER"));
        when(securityServiceClient.obtenerRoles(credencial.getIdUsuario())).thenReturn(rolesResponse);
        when(jwtService.generarToken(credencial.getEmailLogin(), credencial.getIdUsuario(), List.of("ROLE_USER")))
                .thenReturn("new-jwt-token");

        AuthResponseDTO response = authService.refreshToken(rawToken);

        assertNotNull(response);
        assertEquals("new-jwt-token", response.getToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Token renovado exitosamente", response.getMensaje());
        assertTrue(stored.isRevocado());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refreshToken_notFound_throwsException() {
        String rawToken = "invalid-token";
        String hash = sha256(rawToken);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken(rawToken));
    }

    @Test
    void refreshToken_revoked_throwsException() {
        String rawToken = "revoked-token";
        String hash = sha256(rawToken);

        RefreshToken stored = RefreshToken.builder()
                .idRefreshTk(1L)
                .revocado(true)
                .expiraEn(LocalDateTime.now().plusDays(1))
                .build();

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken(rawToken));
    }

    @Test
    void refreshToken_expired_throwsException() {
        String rawToken = "expired-token";
        String hash = sha256(rawToken);

        RefreshToken stored = RefreshToken.builder()
                .idRefreshTk(1L)
                .revocado(false)
                .expiraEn(LocalDateTime.now().minusDays(1))
                .build();

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken(rawToken));
        assertTrue(stored.isRevocado());
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void validarToken_valid_returnsSuccess() {
        ValidateTokenRequestDTO request = new ValidateTokenRequestDTO();
        request.setToken("valid.jwt.token");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("test@email.com");
        when(claims.get("idUsuario", Long.class)).thenReturn(1L);
        when(claims.get("roles", List.class)).thenReturn(List.of("ROLE_USER"));

        when(jwtService.validarToken(request.getToken())).thenReturn(claims);

        TokenClaimsResponseDTO response = authService.validarToken(request);

        assertTrue(response.isValido());
        assertEquals("test@email.com", response.getEmail());
        assertEquals(1L, response.getIdUsuario());
        assertEquals(List.of("ROLE_USER"), response.getRoles());
        assertEquals("Token valido", response.getMensaje());
    }

    @Test
    void validarToken_invalid_returnsError() {
        ValidateTokenRequestDTO request = new ValidateTokenRequestDTO();
        request.setToken("invalid.jwt.token");

        when(jwtService.validarToken(request.getToken())).thenThrow(new RuntimeException("Invalid token"));

        TokenClaimsResponseDTO response = authService.validarToken(request);

        assertFalse(response.isValido());
        assertNull(response.getEmail());
        assertNull(response.getIdUsuario());
        assertTrue(response.getRoles().isEmpty());
        assertEquals("Token invalido o expirado", response.getMensaje());
    }

    @Test
    void listarCredenciales() {
        Credencial c1 = Credencial.builder()
                .idCredencial(1L).idUsuario(1L).emailLogin("a@test.com").estadoCuenta("ACTIVO").build();
        Credencial c2 = Credencial.builder()
                .idCredencial(2L).idUsuario(2L).emailLogin("b@test.com").estadoCuenta("ACTIVO").build();

        when(credencialRepository.findAll()).thenReturn(List.of(c1, c2));

        List<CredencialResponseDTO> result = authService.listarCredenciales();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getIdUsuario());
        assertEquals("a@test.com", result.get(0).getEmail());
        assertEquals("ACTIVO", result.get(0).getEstadoCuenta());
    }

    @Test
    void obtenerPorIdUsuario_found() {
        Credencial credencial = Credencial.builder()
                .idCredencial(1L).idUsuario(1L).emailLogin("test@email.com").estadoCuenta("ACTIVO").build();

        when(credencialRepository.findByIdUsuario(1L)).thenReturn(Optional.of(credencial));

        CredencialResponseDTO result = authService.obtenerPorIdUsuario(1L);

        assertEquals(1L, result.getIdUsuario());
        assertEquals("test@email.com", result.getEmail());
        assertEquals("ACTIVO", result.getEstadoCuenta());
    }

    @Test
    void obtenerPorIdUsuario_notFound_throwsException() {
        when(credencialRepository.findByIdUsuario(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.obtenerPorIdUsuario(99L));
    }

    @Test
    void actualizarEstadoCuenta_success() {
        Credencial credencial = Credencial.builder()
                .idCredencial(1L).idUsuario(1L).emailLogin("test@email.com").estadoCuenta("ACTIVO").build();

        ActualizarEstadoCuentaDTO dto = new ActualizarEstadoCuentaDTO();
        dto.setEstadoCuenta("INACTIVO");

        when(credencialRepository.findByIdUsuario(1L)).thenReturn(Optional.of(credencial));
        when(credencialRepository.save(any(Credencial.class))).thenReturn(credencial);

        CredencialResponseDTO result = authService.actualizarEstadoCuenta(1L, dto);

        assertEquals("INACTIVO", result.getEstadoCuenta());
        assertEquals("INACTIVO", credencial.getEstadoCuenta());
    }

    @Test
    void actualizarEstadoCuenta_notFound_throwsException() {
        ActualizarEstadoCuentaDTO dto = new ActualizarEstadoCuentaDTO();
        dto.setEstadoCuenta("INACTIVO");

        when(credencialRepository.findByIdUsuario(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.actualizarEstadoCuenta(99L, dto));
    }

    @Test
    void eliminarCredencial_success() {
        Credencial credencial = Credencial.builder()
                .idCredencial(1L).idUsuario(1L).emailLogin("test@email.com").estadoCuenta("ACTIVO").build();

        when(credencialRepository.findByIdUsuario(1L)).thenReturn(Optional.of(credencial));

        authService.eliminarCredencial(1L);

        assertEquals("INACTIVO", credencial.getEstadoCuenta());
        verify(credencialRepository).save(credencial);
    }

    @Test
    void eliminarCredencial_notFound_throwsException() {
        when(credencialRepository.findByIdUsuario(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.eliminarCredencial(99L));
    }

}
