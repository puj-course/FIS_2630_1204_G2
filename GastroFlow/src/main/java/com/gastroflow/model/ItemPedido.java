package com.gastroflow.model;

import java.math.BigDecimal;

public class ItemPedido {
    private final Producto producto;
    private int cantidad = 1;

    public ItemPedido(Producto producto) { this.producto = producto; }
    public Producto getProducto() { return producto; }
    public int getCantidad() { return cantidad; }
    public void aumentarCantidad() { cantidad++; }
    public void disminuirCantidad() { if (cantidad > 1) cantidad--; }
    public BigDecimal getSubtotal() {
        return producto.getPrecioVenta().multiply(BigDecimal.valueOf(cantidad));
    }
}
