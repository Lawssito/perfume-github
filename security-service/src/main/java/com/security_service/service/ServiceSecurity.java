package com.security_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.security_service.dto.ValidacionResponseDTO;
import com.security_service.dto.ValidarAccesoRequestDTO;
import com.security_service.model.Permiso;
import com.security_service.model.UsuarioRol;
import com.security_service.repository.UsuarioRolRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceSecurity {

    private final UsuarioRolRepository usuarioRolRepository;

    // La misma clave secreta que usa el auth-service para crear el token
    @Value("${jwt.secret}")
    private String jwtSecret;

    public ValidacionResponseDTO validarTokenYPermiso(ValidarAccesoRequestDTO request) {
        log.info("Iniciando validación de token para el permiso: {}", request.getPermisoRequerido());
        ValidacionResponseDTO response = new ValidacionResponseDTO();

        try {
            // 1. Validar la firma y expiración del JWT (Stateless)
            // Si el token es inválido o expiró, esto lanzará una excepción
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret.getBytes())
                    .parseClaimsJws(request.getToken().replace("Bearer ", ""))
                    .getBody();

            // 2. Extraer el ID del usuario del payload del token
            Long idUsuario = Long.valueOf(claims.getSubject());
            response.setIdUsuario(idUsuario);

            // 3. Buscar los roles del usuario en la Base de Datos
            List<UsuarioRol> rolesUsuario = usuarioRolRepository.findByIdUsuario(idUsuario);

            if (rolesUsuario.isEmpty()) {
                log.warn("El usuario ID {} no tiene roles asignados.", idUsuario);
                response.setValido(false);
                response.setMensaje("Usuario sin roles asignados");
                return response;
            }

            // 4. Verificar si alguno de los roles del usuario tiene el permiso requerido
            boolean tienePermiso = rolesUsuario.stream()
                    .flatMap(ur -> ur.getRol().getPermisos().stream())
                    .map(Permiso::getNombre)
                    .anyMatch(nombrePermiso -> nombrePermiso.equals(request.getPermisoRequerido()));

            if (tienePermiso) {
                log.info("Validación exitosa. Usuario ID {} tiene el permiso {}.", idUsuario, request.getPermisoRequerido());
                response.setValido(true);
                response.setMensaje("Acceso autorizado");
            } else {
                log.warn("Acceso denegado. Usuario ID {} NO tiene el permiso {}.", idUsuario, request.getPermisoRequerido());
                response.setValido(false);
                response.setMensaje("No tiene los permisos suficientes");
            }

            return response;

        } catch (Exception e) {
            log.error("Token JWT inválido o expirado: {}", e.getMessage());
            response.setValido(false);
            response.setMensaje("Token inválido o expirado");
            return response;
        }
    }
}