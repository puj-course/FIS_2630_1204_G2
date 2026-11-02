package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AlertaInventario {

    private Long id;
    private Long ingredienteId;
    private Long productoId;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private BigDecimal cantidadSugerida;
    private String estado;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public AlertaInventario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIngredienteId() {
        return ingredienteId;
    }

    public void setIngredienteId(Long ingredienteId) {
        this.ingredienteId = ingredienteId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
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

    public BigDecimal getCantidadSugerida() {
        return cantidadSugerida;
    }

    public void setCantidadSugerida(BigDecimal cantidadSugerida) {
        this.cantidadSugerida = cantidadSugerida;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public boolean estaPendiente() {
        return "PENDIENTE".equalsIgnoreCase(estado);
    }

    public boolean estaResuelta() {
        return "RESUELTA".equalsIgnoreCase(estado);
    }
}