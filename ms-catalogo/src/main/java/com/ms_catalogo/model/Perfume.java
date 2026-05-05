package com.ms_catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "perfumes")
public class Perfume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Perfumeid;

    private String nombre;
    private String marca;
    private Integer ml;
    private Double precio;

    @Enumerated(EnumType.STRING)
    private Concentracion concentracion;

    @Enumerated(EnumType.STRING)
    private Genero genero;

    @Column(length = 500)
    private String descripcion;
}
