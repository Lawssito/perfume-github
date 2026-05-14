package com.ms_catalogo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marcas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Marca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMarca;

    @Column(nullable = false, unique = true)
    private String nombre;
}