import java.util.ArrayList;
import java.util.List;

/**
 * Representa el pedido de un cliente para un producto, partiendo de su
 * receta base pero permitiendo personalizarlo (quitar, agregar, cambiar
 * ingredientes o ajustar cantidades) SIN modificar la receta original,
 * que sigue sirviendo para otros clientes que lo pidan "normal".
 */
public class PedidoPersonalizado {

    private final Producto producto;
    private final List<RecetaDetalle> ingredientesFinales; // lo que realmente lleva el plato
    private final List<Modificacion> modificaciones;       // historial, para la nota de cocina

    public PedidoPersonalizado(Receta recetaBase) {
        this.producto = recetaBase.getProducto();
        // copia independiente: modificar esta lista no afecta la receta original
        this.ingredientesFinales = new ArrayList<>(recetaBase.getDetalles());
        this.modificaciones = new ArrayList<>();
    }

    public Producto getProducto() {
        return producto;
    }

    public List<RecetaDetalle> getIngredientesFinales() {
        return ingredientesFinales;
    }

    public List<Modificacion> getModificaciones() {
        return modificaciones;
    }

    /**
     * Quita un ingrediente que venía en la receta base (ej: "sin cebolla").
     */
    public void quitarIngrediente(Ingrediente ingrediente) {
        RecetaDetalle detalle = buscarDetalle(ingrediente.getId());
        if (detalle == null) {
            throw new IllegalArgumentException(
                    "El plato no contiene '" + ingrediente.getNombre() + "', no se puede quitar.");
        }
        ingredientesFinales.remove(detalle);
        modificaciones.add(Modificacion.quitar(ingrediente));
    }

    /**
     * Agrega un ingrediente que NO venía en la receta base (ej: "extra queso").
     */
    public void agregarIngrediente(Ingrediente ingrediente, double cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0.");
        }
        if (buscarDetalle(ingrediente.getId()) != null) {
            throw new IllegalArgumentException(
                    "El plato ya contiene '" + ingrediente.getNombre() + "'. Usa 'cambiar cantidad' en su lugar.");
        }
        ingredientesFinales.add(new RecetaDetalle(ingrediente, cantidad));
        modificaciones.add(Modificacion.agregar(ingrediente, cantidad));
    }

    /**
     * Reemplaza un ingrediente de la receta base por otro distinto
     * (ej: "cambiar papa por más plátano").
     */
    public void cambiarIngrediente(Ingrediente ingredienteOriginal, Ingrediente ingredienteNuevo, double cantidadNueva) {
        if (cantidadNueva <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0.");
        }
        RecetaDetalle detalle = buscarDetalle(ingredienteOriginal.getId());
        if (detalle == null) {
            throw new IllegalArgumentException(
                    "El plato no contiene '" + ingredienteOriginal.getNombre() + "', no se puede cambiar.");
        }
        if (buscarDetalle(ingredienteNuevo.getId()) != null) {
            throw new IllegalArgumentException(
                    "El plato ya contiene '" + ingredienteNuevo.getNombre() + "'.");
        }
        ingredientesFinales.remove(detalle);
        ingredientesFinales.add(new RecetaDetalle(ingredienteNuevo, cantidadNueva));
        modificaciones.add(Modificacion.cambiar(ingredienteOriginal, ingredienteNuevo, cantidadNueva));
    }

    /**
     * Ajusta la cantidad de un ingrediente que ya está en el pedido
     * (ej: "doble carne", "media porción de arroz").
     */
    public void cambiarCantidad(Ingrediente ingrediente, double nuevaCantidad) {
        if (nuevaCantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0. Si quieres quitarlo, usa 'quitar'.");
        }
        RecetaDetalle detalle = buscarDetalle(ingrediente.getId());
        if (detalle == null) {
            throw new IllegalArgumentException(
                    "El plato no contiene '" + ingrediente.getNombre() + "'.");
        }
        ingredientesFinales.remove(detalle);
        ingredientesFinales.add(new RecetaDetalle(ingrediente, nuevaCantidad));
        modificaciones.add(Modificacion.cambiarCantidad(ingrediente, nuevaCantidad));
    }

    private RecetaDetalle buscarDetalle(int idIngrediente) {
        for (RecetaDetalle d : ingredientesFinales) {
            if (d.getIngrediente().getId() == idIngrediente) {
                return d;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Pedido: ").append(producto.getNombre()).append("\n");
        sb.append("Ingredientes finales:\n");
        for (RecetaDetalle d : ingredientesFinales) {
            sb.append(d).append("\n");
        }
        if (!modificaciones.isEmpty()) {
            sb.append("Nota para cocina:\n");
            for (Modificacion m : modificaciones) {
                sb.append("  * ").append(m).append("\n");
            }
        }
        return sb.toString();
    }
}
 