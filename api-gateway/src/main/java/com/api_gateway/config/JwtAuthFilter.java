package com.api_gateway.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
@Order(1)
public class JwtAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    // Rutas públicas que NO requieren autenticación (path → métodos HTTP permitidos)
    // Si la lista de métodos está vacía, todos los métodos son permitidos
    private static final Map<String, List<String>> PUBLIC_PATHS = Map.ofEntries(
        Map.entry("/api/auth/login", List.of("POST")),
        Map.entry("/api/auth/refresh", List.of("POST")),
        Map.entry("/api/auth/validate", List.of("POST")),
        Map.entry("/api/usuarios", List.of("POST")),            // Solo POST (registro) es público
        Map.entry("/api/catalogo/productos", List.of("GET")),
        Map.entry("/api/catalogo/buscar", List.of("GET")),
        Map.entry("/api/catalogo/marcas", List.of("GET")),
        Map.entry("/api/catalogo/categorias", List.of("GET")),
        Map.entry("/api/catalogo/perfumes", List.of("GET")),
        Map.entry("/swagger-ui", List.of()),
        Map.entry("/v3/api-docs", List.of()),
        Map.entry("/swagger-ui.html", List.of())
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

        // Permitir rutas públicas (según path y método HTTP)
        if (esRutaPublica(path, request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Verificar Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[AUDIT] Token JWT faltante para: {}", path);
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

            // Pasar información del usuario a los servicios downstream
            // mediante headers HTTP (el proxy del gateway reenvía los headers)
            HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
                private final Map<String, String> extraHeaders = new LinkedHashMap<>();

                {
                    Object idUsuario = claims.get("idUsuario");
                    if (idUsuario != null) {
                        extraHeaders.put("X-User-Id", idUsuario.toString());
                    }
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) claims.get("roles");
                    if (roles != null && !roles.isEmpty()) {
                        extraHeaders.put("X-User-Roles", String.join(",", roles));
                    }
                    if (claims.getSubject() != null) {
                        extraHeaders.put("X-User-Email", claims.getSubject());
                    }
                }

                @Override
                public String getHeader(String name) {
                    String extra = extraHeaders.get(name);
                    return extra != null ? extra : super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    Set<String> names = new LinkedHashSet<>(extraHeaders.keySet());
                    Enumeration<String> original = super.getHeaderNames();
                    while (original.hasMoreElements()) {
                        names.add(original.nextElement());
                    }
                    return Collections.enumeration(names);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if (extraHeaders.containsKey(name)) {
                        return Collections.enumeration(List.of(extraHeaders.get(name)));
                    }
                    return super.getHeaders(name);
                }
            };

            log.debug("[AUDIT] Token valido para {} en {}", claims.getSubject(), path);
            chain.doFilter(wrappedRequest, response);

        } catch (Exception e) {
            log.warn("[AUDIT] Token JWT invalido para: {} — {}", path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token JWT invalido o expirado\"}");
        }
    }

    private boolean esRutaPublica(String path, String method) {
        return PUBLIC_PATHS.entrySet().stream()
                .anyMatch(entry -> path.startsWith(entry.getKey()) &&
                        (entry.getValue().isEmpty() || entry.getValue().contains(method)));
    }
}
