package entity;

import java.time.LocalDateTime;

public class AsignacionMesa {

    private Long id;
    private Long mesaId;
    private Long usuarioId;
    private LocalDateTime fechaAsignacion = LocalDateTime.now();
    private Boolean isActive = true;

    public AsignacionMesa() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMesaId() { return mesaId; }
    public void setMesaId(Long mesaId) { this.mesaId = mesaId; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDateTime fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}