package com.ms_pagos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity @Data
@Table(name = "transacciones")
@AllArgsConstructor
@NoArgsConstructor
public class Pago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTransaccion")
    private Long idTransaccion;

    @Column(name = "idPedido", nullable = false, unique = true)
    private Long idPedido;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false)
    private MetodoPago metodoPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPago estado;

    @Column(name = "referencia_externa")
    private String referenciaExterna;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "procesado_en")
    private LocalDateTime procesadoEn;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
        this.estado   = EstadoPago.PENDIENTE;
    }
}
