
public class RecetaItem {
    private Long productoId;
    private String productoNombre;
    private Long ingredienteId;
    private String ingredienteNombre;
    private double cantidad;
    private String unidadMedida;
    private double costoUnitario;
    private double costoSubtotal;

    public RecetaItem(Long productoId, String productoNombre, Long ingredienteId,
                      String ingredienteNombre, double cantidad, String unidadMedida,
                      double costoUnitario, double costoSubtotal) {
        this.productoId = productoId;
        this.productoNombre = productoNombre;
        this.ingredienteId = ingredienteId;
        this.ingredienteNombre = ingredienteNombre;
        this.cantidad = cantidad;
        this.unidadMedida = unidadMedida;
        this.costoUnitario = costoUnitario;
        this.costoSubtotal = costoSubtotal;
    }

    // Getters y toString para depuración/visualización
    public Long getProductoId() { return productoId; }
    public String getProductoNombre() { return productoNombre; }
    public Long getIngredienteId() { return ingredienteId; }
    public String getIngredienteNombre() { return ingredienteNombre; }
    public double getCantidad() { return cantidad; }
    public String getUnidadMedida() { return unidadMedida; }
    public double getCostoUnitario() { return costoUnitario; }
    public double getCostoSubtotal() { return costoSubtotal; }

    @Override
    public String toString() {
        return String.format("%s -> %s: %.3f %s (Subtotal: $%.2f)",
                productoNombre, ingredienteNombre, cantidad, unidadMedida, costoSubtotal);
    }
}