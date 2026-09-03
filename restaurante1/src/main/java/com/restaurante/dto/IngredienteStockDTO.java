package com.restaurante.dto;

import java.math.BigDecimal;

public class IngredienteStockDTO {

    private Long id;
    private String nombre;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private Boolean isActive;
    private String estado;

    public IngredienteStockDTO(
            Long id,
            String nombre,
            BigDecimal stockActual,
            BigDecimal stockMinimo,
            Boolean isActive,
            String estado
    ) {
        this.id = id;
        this.nombre = nombre;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.isActive = isActive;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getStockActual() {
        return stockActual;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public String getEstado() {
        return estado;
    }
}