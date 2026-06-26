package com.ms_stock.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotencia_keys")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IdempotenciaKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "id_variante", nullable = false)
    private Long idVariante;

    @Column(name = "operacion", nullable = false, length = 50)
    private String operacion;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }
}
