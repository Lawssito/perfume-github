package com.ms_notificaciones.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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