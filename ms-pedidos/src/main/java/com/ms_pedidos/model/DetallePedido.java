package com.ms_pedidos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_pedidos")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedido {

     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long idDetalle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    // Referencia lógica a ms-catalogo
    @Column(name = "id_variante", nullable = false)
    private Long idVariante;

    @Column(name = "nombre_producto", nullable = false)
    private String nombreProducto;

    @Column(name = "ml", nullable = false)
    private Integer ml;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;
}
