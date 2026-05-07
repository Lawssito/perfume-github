package com.auth_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "credenciales")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Credencial {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // ¡Recuerda que esto debe guardarse encriptado (ej. BCrypt)!

    @Column(length = 500)
    private String token; // Opcional: solo si necesitas revocar o rastrear el último token

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;
}
