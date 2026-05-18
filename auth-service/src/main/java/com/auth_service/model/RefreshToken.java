package com.auth_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRefreshTk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_credencial", nullable = false)
    private Credencial credencial;

    @Column(name = "token_hash", nullable = false, unique = true, length = 500)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(nullable = false)
    private boolean revocado;
}