package com.gastroflow.model;

public class ProductoDisponibilidad {
    private final long productoId;
    private final String codigo;
    private final String nombre;
    private final String categoria;
    private final String estado;
    private final String motivo;

    public ProductoDisponibilidad(long productoId, String codigo, String nombre,
                                  String categoria, String estado, String motivo) {
        this.productoId = productoId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.categoria = categoria;
        this.estado = estado;
        this.motivo = motivo;
    }

    public long getProductoId() { return productoId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getEstado() { return estado; }
    public String getMotivo() { return motivo; }
}
