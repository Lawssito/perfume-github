package com.security_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "permisos")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre; // CREAR_PERFUME

    @Column(nullable = false)
    private String ruta; // /api/catalogo/perfumes

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoHttp metodo; // POST

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

}
