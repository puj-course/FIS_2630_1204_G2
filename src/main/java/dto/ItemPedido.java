package dto;

import entity.PlatoInsumo;
import entity.Producto;

import java.math.BigDecimal;

public class ItemPedido {

    private final Productos producto;
    private int cantidad = 1;

    public ItemPedido(Productos producto) {
        this.producto = producto;
    }

    public ItemPedido(Productos producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
    }

    public Productos getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void aumentarCantidad() {
        cantidad++;
    }

    public void disminuirCantidad() {
        if (cantidad > 1) {
            cantidad--;
        }
    }

    public void establecerCantidad(int cantidad) {
        if (cantidad > 0) {
            this.cantidad = cantidad;
        }
    }

    public BigDecimal getSubtotal() {
        return producto.getPrecioVenta()
                .multiply(BigDecimal.valueOf(cantidad));
    }
}