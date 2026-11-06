package entity;

import java.math.BigDecimal;

public class ProductoIngrediente {
    private Long productoId;
    private Long ingredienteId;
    private BigDecimal cantidadNecesaria;

    // Constructor vacío
    public ProductoIngrediente() {}

    // Constructor con parámetros
    public ProductoIngrediente(long productoId, long ingredienteId, BigDecimal cantidadNecesaria) {
        this.productoId = productoId;
        this.ingredienteId = ingredienteId;
        this.cantidadNecesaria = cantidadNecesaria;
    }

    public long getProductoId() { return productoId;}
    public void setProductoId(long productoId) { this.productoId = productoId;}

    public long getIngredienteId() { return ingredienteId;}
    public void setIngredienteId(long ingredienteId) { this.ingredienteId = ingredienteId;}

    public BigDecimal getCantidadNecesaria() { return cantidadNecesaria; }
    public void setCantidadNecesaria(BigDecimal cantidadNecesaria) { this.cantidadNecesaria = cantidadNecesaria; }
}