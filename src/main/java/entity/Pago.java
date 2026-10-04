package com.restaurante.entity;

import com.restaurante.enums.MetodoPago;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pago",
        indexes = {
                @Index(
                        name = "idx_pago_pedido",
                        columnList = "pedido_id"
                )
        }
)
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPago metodo;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal monto;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Pago() {
        this.createdAt = LocalDateTime.now();
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

    public MetodoPago getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPago metodo) {
        this.metodo = metodo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}