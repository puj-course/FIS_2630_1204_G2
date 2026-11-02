package dto;

public class ProductoDisponibilidad {
    private final long productoId;
    private final String nombre;
    private final String categoria;
    private final String estado;

    public ProductoDisponibilidad(long productoId, String codigo, String nombre,
                                  String categoria, String estado) {
        this.productoId = productoId;
        this.nombre = nombre;
        this.categoria = categoria;
        this.estado = estado;
    }

    public long getProductoId() { return productoId; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getEstado() { return estado; }
}
