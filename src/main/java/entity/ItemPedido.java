package models;

/**
 * Representa una línea del pedido de la mesa: un producto, cuántas
 * unidades pidió el cliente, y cómo lo personalizó (si aplica).
 * A partir de aquí se calcula cuánto debe pagar por esa línea.
 */
public class ItemPedido {

    private final PedidoPersonalizado pedido; // el plato ya personalizado (o sin cambios)
    private final int cantidadUnidades;        // cuántas unidades de ESE mismo plato pidió

    public ItemPedido(PedidoPersonalizado pedido, int cantidadUnidades) {
        if (cantidadUnidades <= 0) {
            throw new IllegalArgumentException("La cantidad de unidades debe ser mayor a 0.");
        }
        this.pedido = pedido;
        this.cantidadUnidades = cantidadUnidades;
    }

    public Producto getProducto() {
        return pedido.getProducto();
    }

    public PedidoPersonalizado getPedido() {
        return pedido;
    }

    public int getCantidadUnidades() {
        return cantidadUnidades;
    }

    public double getPrecioUnitario() {
        return pedido.getProducto().getPrecio();
    }

    /**
     * Lo que se debe pagar por esta línea: precio unitario x cantidad de unidades.
     */
    public double getSubtotal() {
        return getPrecioUnitario() * cantidadUnidades;
    }
}

