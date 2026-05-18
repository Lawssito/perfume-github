package com.auth_service.service;


import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:3600000}")
    private Long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(String email, Long idUsuario, List<String> roles) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(email)
                .claim("idUsuario", idUsuario)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key())
                .compact();
    }

    public Claims validarToken(String token) {
        String limpio = token.replace("Bearer ", "");
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(limpio).getPayload();
    }
}
