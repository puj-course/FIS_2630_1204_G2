package entity;

public class EstadoMesa {

    private Long id;
    private String codigoEstado;
    private String descripcion;

    public EstadoMesa() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigoEstado() { return codigoEstado; }
    public void setCodigoEstado(String codigoEstado) { this.codigoEstado = codigoEstado; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}