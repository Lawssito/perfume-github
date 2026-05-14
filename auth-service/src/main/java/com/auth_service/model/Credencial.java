package com.auth_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "credenciales")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Credencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCredencial;

    // Solo guardamos la referencia, los datos viven en user-service
    @Column(name = "id_usuario", nullable = false, unique = true)
    private Long idUsuario;

    @Column(name = "email_login", nullable = false, unique = true)
    private String emailLogin;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "estado_cuenta", nullable = false)
    private String estadoCuenta; // Ej: "ACTIVO", "BLOQUEADO"

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
        if (this.estadoCuenta == null) {
            this.estadoCuenta = "ACTIVO";
        }
    }
}