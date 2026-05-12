package com.ms_notificaciones.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNotificacion;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario; // Referencia al MS de Usuarios

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Column(name = "estado_envio", nullable = false)
    private String estadoEnvio; // Ej: "PENDIENTE", "ENVIADO", "FALLIDO"

    @PrePersist
    protected void onCreate() {
        this.fecha = LocalDateTime.now();
        if (this.estadoEnvio == null) {
            this.estadoEnvio = "PENDIENTE";
        }
    }
}