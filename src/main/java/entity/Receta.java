package modelo;

import java.util.ArrayList;
import java.util.List;

public class Receta {

    private Producto producto;
    private List<RecetaDetalle> detalles;

    public Receta(Producto producto) {
        this.producto = producto;
        this.detalles = new ArrayList<>();
    }

    public Producto getProducto() {
        return producto;
    }

    public List<RecetaDetalle> getDetalles() {
        return detalles;
    }

    public void agregarIngrediente(Ingrediente ingrediente, double cantidad) {
        if (ingrediente == null) {
            throw new IllegalArgumentException("El ingrediente no puede ser nulo.");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0.");
        }
        for (RecetaDetalle d : detalles) {
            if (d.getIngrediente().getId() == ingrediente.getId()) {
                throw new IllegalArgumentException("El ingrediente '" + ingrediente.getNombre() + "' ya está en la receta.");
            }
        }
        detalles.add(new RecetaDetalle(ingrediente, cantidad));
    }

    public boolean tieneIngredientes() {
        return !detalles.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Receta de: ").append(producto.getNombre()).append("\n");
        if (detalles.isEmpty()) {
            sb.append("  (sin ingredientes)\n");
        } else {
            for (RecetaDetalle d : detalles) {
                sb.append(d).append("\n");
            }
        }
        return sb.toString();
    }
}
