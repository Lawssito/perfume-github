package com.ms_catalogo.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "perfumes")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder

    public class Perfume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPerfume;

    @ManyToOne
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "id_marca", nullable = false)
    private Marca marca;

    @Column(nullable = false)
    private String concentracion; // EDP, EDT, Parfum

    @Column(nullable = false)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private String estado; // ACTIVO, DESCONTINUADO

    // mappedBy apunta al nombre del atributo en la clase Variante
    @OneToMany(mappedBy = "perfume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Variante> variantes;
}