package com.restaurante.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "ingrediente",
        indexes = {
                @Index(
                        name = "idx_ingrediente_stock",
                        columnList = "stock_actual, stock_minimo"
                )
        }
)
public class Ingrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(
            name = "stock_actual",
            precision = 12,
            scale = 4,
            nullable = false
    )
    private BigDecimal stockActual;

    @Column(
            name = "stock_minimo",
            precision = 12,
            scale = 4,
            nullable = false
    )
    private BigDecimal stockMinimo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Ingrediente() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getStockActual() {
        return stockActual;
    }

    public void setStockActual(BigDecimal stockActual) {
        this.stockActual = stockActual;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}