package entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovimientoInventario {

    private Long idMovimiento;
    private Long productoId;
    private Long ingredienteId;
    private String tipoMovimiento;
    private BigDecimal cantidad;
    private String motivo;
    private Long usuarioId;
    private LocalDateTime fechaMovimiento;

    public MovimientoInventario() {
    }

    public Long getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(Long idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Long getIngredienteId() {
        return ingredienteId;
    }

    public void setIngredienteId(Long ingredienteId) {
        this.ingredienteId = ingredienteId;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public boolean esEntrada() {
        return "ENTRADA".equalsIgnoreCase(tipoMovimiento);
    }

    public boolean esSalida() {
        return "SALIDA".equalsIgnoreCase(tipoMovimiento);
    }
}