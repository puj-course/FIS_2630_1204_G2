package com.gastroflow.model;

import java.math.BigDecimal;

public class Producto {
    private final long productoId;
    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final String categoria;
    private final BigDecimal precioVenta;
    private final String estado;

    public Producto(long productoId, String codigo, String nombre, String descripcion,
                    String categoria, BigDecimal precioVenta, String estado) {
        this.productoId = productoId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.precioVenta = precioVenta;
        this.estado = estado;
    }

    public long getProductoId() { return productoId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public BigDecimal getPrecioVenta() { return precioVenta; }
    public String getEstado() { return estado; }
    public boolean isDisponible() { return "DISPONIBLE".equalsIgnoreCase(estado); }
}
