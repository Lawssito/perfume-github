package com.security_service.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Slf4j
@Component
@Order(1)
public class JwtAuthFilter implements Filter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${security.internal-api-key}")
    private String internalApiKey;

    // Rutas públicas que NO requieren autenticación
    private static final List<String> PUBLIC_PATHS = List.of(
        "/swagger-ui",
        "/swagger-ui.html",
        "/v3/api-docs"
    );

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();

        // Permitir rutas públicas (swagger, api-docs)
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        // Permitir llamadas internas con API key (user-service → security-service via Feign)
        String apiKey = request.getHeader("X-Internal-Api-Key");
        if (apiKey != null && apiKey.equals(internalApiKey)) {
            log.debug("[AUDIT] Acceso interno permitido para: {}", path);
            chain.doFilter(request, response);
            return;
        }

        // Verificar Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[AUDIT] Token JWT requerido para: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token JWT requerido\"}");
            return;
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            request.setAttribute("idUsuario", claims.get("idUsuario"));
            request.setAttribute("roles", claims.get("roles"));
            request.setAttribute("email", claims.getSubject());

            log.debug("[AUDIT] Token valido para {} en {}", claims.getSubject(), path);
            chain.doFilter(request, response);

        } catch (Exception e) {
            log.warn("[AUDIT] Token JWT invalido para: {} — {}", path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token JWT invalido o expirado\"}");
        }
    }
}
