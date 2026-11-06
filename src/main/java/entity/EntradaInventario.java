package entity;

// Declara la clase EntradaInventario.
// Esta clase representa una entrada de un ingrediente al inventario.
public class EntradaInventario {

    private int idMovimiento;
    private Long ingredienteId;
    private String tipoMovimiento; // 'entrada', 'salida', etc.
    private double cantidad;
    private String motivo;

    public EntradaInventario() {}

    public EntradaInventario(Long ingredienteId, double cantidad, String motivo) {
        this.ingredienteId = ingredienteId;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.tipoMovimiento = "entrada";
    }

    public int getIdMovimiento() { return idMovimiento; }
    public void setIdMovimiento(int idMovimiento) { this.idMovimiento = idMovimiento; }

    public Long getIngredienteId() { return ingredienteId; }
    public void setIngredienteId(Long ingredienteId) { this.ingredienteId = ingredienteId; }

    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }

    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
