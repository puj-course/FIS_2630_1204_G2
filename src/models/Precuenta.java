
import java.util.ArrayList;
import java.util.List;

/**
 * Representa la cuenta de una mesa/pedido: la lista de productos pedidos
 * (cada uno con su cantidad y personalización) y permite calcular y
 * mostrar la precuenta con precios, subtotales y el total a pagar.
 */
public class Precuenta {

    private final List<ItemPedido> items = new ArrayList<>();

    public void agregarItem(ItemPedido item) {
        items.add(item);
    }

    public List<ItemPedido> getItems() {
        return items;
    }

    public boolean estaVacia() {
        return items.isEmpty();
    }

    /**
     * Suma el subtotal de todas las líneas del pedido.
     */
    public double calcularTotal() {
        double total = 0;
        for (ItemPedido item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    /**
     * Genera el texto de la precuenta: producto, cantidad, precio unitario,
     * subtotal por línea, notas de personalización, y el total a pagar.
     */
    public String generarPrecuenta() {
        StringBuilder sb = new StringBuilder();
        sb.append("===================== PRECUENTA =====================\n");

        if (items.isEmpty()) {
            sb.append("El pedido todavía no tiene productos.\n");
            return sb.toString();
        }

        sb.append(String.format("%-25s %8s %12s %12s%n", "Producto", "Cant.", "Precio Unit.", "Subtotal"));
        sb.append("-------------------------------------------------------\n");

        for (ItemPedido item : items) {
            sb.append(String.format("%-25s %8d %12s %12s%n",
                    item.getProducto().getNombre(),
                    item.getCantidadUnidades(),
                    formatoMoneda(item.getPrecioUnitario()),
                    formatoMoneda(item.getSubtotal())));

            // si el plato tiene personalizaciones, se muestran como nota debajo de la línea
            for (Modificacion m : item.getPedido().getModificaciones()) {
                sb.append("      * ").append(m).append("\n");
            }
        }

        sb.append("-------------------------------------------------------\n");
        sb.append(String.format("%-46s %12s%n", "TOTAL A PAGAR", formatoMoneda(calcularTotal())));
        sb.append("=======================================================\n");

        return sb.toString();
    }

    /**
     * Genera Y muestra la precuenta directamente por consola.
     */
    public void mostrarPrecuenta() {
        System.out.println(generarPrecuenta());
    }

    private String formatoMoneda(double valor) {
        return String.format("$%,.0f", valor);
    }
}
 