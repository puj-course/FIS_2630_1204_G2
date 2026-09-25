package entity;

import java.time.LocalDateTime;

public class HistorialEstadoMesa {

    private Long id;
    private Long mesaId;
    private Long estadoAnteriorId;
    private Long estadoNuevoId;
    private LocalDateTime fechaCambio = LocalDateTime.now();

    public HistorialEstadoMesa() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMesaId() { return mesaId; }
    public void setMesaId(Long mesaId) { this.mesaId = mesaId; }

    public Long getEstadoAnteriorId() { return estadoAnteriorId; }
    public void setEstadoAnteriorId(Long estadoAnteriorId) { this.estadoAnteriorId = estadoAnteriorId; }

    public Long getEstadoNuevoId() { return estadoNuevoId; }
    public void setEstadoNuevoId(Long estadoNuevoId) { this.estadoNuevoId = estadoNuevoId; }

    public LocalDateTime getFechaCambio() { return fechaCambio; }
    public void setFechaCambio(LocalDateTime fechaCambio) { this.fechaCambio = fechaCambio; }
}