package com.restaurante.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "detalle_pedido_adicional",
        indexes = {
                @Index(
                        name = "idx_detalle_pedido_adicional_detalle",
                        columnList = "detalle_pedido_id"
                ),
                @Index(
                        name = "idx_detalle_pedido_adicional_adicional",
                        columnList = "adicional_id"
                )
        }
)
public class DetallePedidoAdicional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_pedido_id", nullable = false)
    private DetallePedido detallePedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adicional_id", nullable = false)
    private Adicional adicional;

    @Column(nullable = false)
    private Integer cantidad = 1;

    @Column(
            name = "precio_adicional",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal precioAdicional;

    public DetallePedidoAdicional() {
    }

    public Long getId() {
        return id;
    }

    public DetallePedido getDetallePedido() {
        return detallePedido;
    }

    public void setDetallePedido(DetallePedido detallePedido) {
        this.detallePedido = detallePedido;
    }

    public Adicional getAdicional() {
        return adicional;
    }

    public void setAdicional(Adicional adicional) {
        this.adicional = adicional;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioAdicional() {
        return precioAdicional;
    }

    public void setPrecioAdicional(BigDecimal precioAdicional) {
        this.precioAdicional = precioAdicional;
    }
}
