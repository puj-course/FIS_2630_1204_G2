package com.restaurante.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "detalle_pedido",
        indexes = {
                @Index(
                        name = "idx_detalle_pedido_pedido",
                        columnList = "pedido_id"
                ),
                @Index(
                        name = "idx_detalle_pedido_plato",
                        columnList = "plato_id"
                )
        }
)
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plato_id", nullable = false)
    private Plato plato;

    @Column(
            nullable = false,
            precision = 12,
            scale = 4
    )
    private BigDecimal cantidad;

    public DetallePedido() {
    }

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Plato getPlato() {
        return plato;
    }

    public void setPlato(Plato plato) {
        this.plato = plato;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }
}