package modelo;

public class RecetaDetalle {

    private Ingrediente ingrediente;
    private double cantidad;

    public RecetaDetalle(Ingrediente ingrediente, double cantidad) {
        this.ingrediente = ingrediente;
        this.cantidad = cantidad;
    }

    public Ingrediente getIngrediente() {
        return ingrediente;
    }

    public double getCantidad() {
        return cantidad;
    }

    @Override
    public String toString() {
        return "  - " + ingrediente.getNombre() + ": " + cantidad + " " + ingrediente.getUnidadMedida();
    }
}
