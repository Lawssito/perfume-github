package com.ms_catalogo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "perfumes")
@Data // Lombok: genera getters, setters, toString, equals y hashCode
@NoArgsConstructor
@AllArgsConstructor
public class Perfume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private Integer ml;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio; // BigDecimal es la mejor práctica para dinero

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Concentracion concentracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genero genero;

    @Column(length = 500)
    private String descripcion;
}