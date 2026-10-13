package entity;

public class Zona {

    private Long id;
    private String nombreZona;
    private String descripcion;
    private Integer isActive = 1;

    public Zona() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreZona() { return nombreZona; }
    public void setNombreZona(String nombreZona) { this.nombreZona = nombreZona; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }
}