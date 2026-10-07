package entity;

import java.math.BigDecimal;

public class Ingrediente {

    private long ingredienteId;
    private String nombre;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private String estado;

    public Ingrediente() {
    }

    public Ingrediente(
            long ingredienteId,
            String nombre,
            BigDecimal stockActual,
            BigDecimal stockMinimo,
            String estado
    ) {
        this.ingredienteId = ingredienteId;
        this.nombre = nombre;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.estado = estado;
    }

    public long getIngredienteId() {
        return ingredienteId;
    }

    public void setIngredienteId(long ingredienteId) {
        this.ingredienteId = ingredienteId;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean estaActivo() {
        return "ACTIVO".equalsIgnoreCase(estado);
    }

    public boolean tieneStockBajo() {
        if (stockActual == null || stockMinimo == null) {
            return false;
        }

        return stockActual.compareTo(stockMinimo) <= 0;
    }

    @Override
    public String toString() {
        return nombre;
    }
}