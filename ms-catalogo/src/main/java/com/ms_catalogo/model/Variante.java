package com.ms_catalogo.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "variantes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Variante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idVariante;

    @ManyToOne
    @JoinColumn(name = "id_perfume", nullable = false)
    @JsonIgnore // Evita ciclos infinitos al serializar el JSON
    private Perfume perfume;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private Integer ml;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;
}