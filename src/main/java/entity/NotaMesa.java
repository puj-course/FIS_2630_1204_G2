package entity;

import java.time.LocalDateTime;

public class NotaMesa {
    private int idNota;
    private int idMesa;
    private String textoNota;
    private LocalDateTime fechaCreacion;

    public NotaMesa() {}

    public int getIdNota() { return idNota; }
    public void setIdNota(int idNota) { this.idNota = idNota; }

    public int getIdMesa() { return idMesa; }
    public void setIdMesa(int idMesa) { this.idMesa = idMesa; }

    public String getTextoNota() { return textoNota; }
    public void setTextoNota(String textoNota) { this.textoNota = textoNota; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
