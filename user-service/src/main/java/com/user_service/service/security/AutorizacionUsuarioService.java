package com.user_service.service.security;

import java.util.Collections;

import org.springframework.stereotype.Service;

import com.user_service.client.AuthServiceClient;
import com.user_service.dto.TokenClaimsClientDTO;
import com.user_service.dto.UsuarioAutenticadoDTO;
import com.user_service.dto.ValidateTokenClientDTO;
import com.user_service.exception.ForbiddenException;
import com.user_service.exception.RemoteServiceException;
import com.user_service.exception.UnauthorizedException;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutorizacionUsuarioService {

    private final AuthServiceClient authServiceClient;

    public UsuarioAutenticadoDTO validarSesion(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.warn("[AUTHZ] Solicitud sin token Authorization");
            throw new UnauthorizedException("Debes iniciar sesion y enviar el token Bearer");
        }

        try {
            TokenClaimsClientDTO claims = authServiceClient.validarToken(new ValidateTokenClientDTO(authorizationHeader));
            if (claims == null || !claims.isValido() || claims.getIdUsuario() == null) {
                log.warn("[AUTHZ] Token invalido o sin idUsuario. Mensaje={}", claims != null ? claims.getMensaje() : "sin respuesta");
                throw new UnauthorizedException("Token invalido o expirado");
            }
            log.info("[AUTHZ] Token valido idUsuario={} roles={}", claims.getIdUsuario(), claims.getRoles());
            return new UsuarioAutenticadoDTO(
                    claims.getIdUsuario(),
                    claims.getEmail(),
                    claims.getRoles() != null ? claims.getRoles() : Collections.emptyList()
            );
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (FeignException.Unauthorized ex) {
            log.warn("[AUTHZ] auth-service rechazo el token: status={} body={}", ex.status(), ex.contentUTF8());
            throw new UnauthorizedException("Token invalido o expirado");
        } catch (FeignException ex) {
            log.error("[AUTHZ] Error validando token contra auth-service. status={} body={}", ex.status(), ex.contentUTF8());
            throw new RemoteServiceException("No se pudo validar el token con auth-service");
        }
    }

    public void exigirAdmin(String authorizationHeader) {
        UsuarioAutenticadoDTO usuario = validarSesion(authorizationHeader);
        if (!usuario.esAdmin()) {
            log.warn("[AUTHZ] Usuario {} intento accion solo ADMIN. Roles={}", usuario.getIdUsuario(), usuario.getRoles());
            throw new ForbiddenException("No tienes permisos de administrador para realizar esta accion");
        }
    }

    public void exigirMismoUsuarioOAdmin(String authorizationHeader, Long idUsuarioObjetivo) {
        UsuarioAutenticadoDTO usuario = validarSesion(authorizationHeader);
        if (!usuario.esAdmin() && !usuario.getIdUsuario().equals(idUsuarioObjetivo)) {
            log.warn("[AUTHZ] Usuario {} intento acceder/modificar usuario {}. Roles={}",
                    usuario.getIdUsuario(), idUsuarioObjetivo, usuario.getRoles());
            throw new ForbiddenException("Solo puedes gestionar tus propios datos personales");
        }
    }
}
