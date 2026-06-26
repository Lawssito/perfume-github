package com.ms_stock.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Min;

@Table(name = "inventario")
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter @Entity
public class Inventario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inventario")
    private Long idInventario;

    @Column(name = "id_variante", nullable = false, unique = true)
    private Long idVariante;

    @Column(name = "cantidad_disponible", nullable = false)
    @Min(0)
    private Integer cantidadDisponible;

    @Column(name = "cantidad_reservada", nullable = false)
    @Min(0)
    private Integer cantidadReservada;

    @Version
    @Column(name = "version")
    private Long version;
}
