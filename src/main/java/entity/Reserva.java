package entity;

import java.time.LocalDateTime;

public class Reserva {
    private int idReserva;
    private int idMesa;
    private int numeroMesa;
    private String nombreZona;
    private LocalDateTime fechaHoraReserva;
    private String nombreCliente;
    private int cantidadPersonas;
    private boolean activa;
    private LocalDateTime fechaCancelacion;

    public Reserva() {}

    public int getIdReserva() { return idReserva; }
    public void setIdReserva(int idReserva) { this.idReserva = idReserva; }

    public int getIdMesa() { return idMesa; }
    public void setIdMesa(int idMesa) { this.idMesa = idMesa; }

    public int getNumeroMesa() { return numeroMesa; }
    public void setNumeroMesa(int numeroMesa) { this.numeroMesa = numeroMesa; }

    public String getNombreZona() { return nombreZona; }
    public void setNombreZona(String nombreZona) { this.nombreZona = nombreZona; }

    public LocalDateTime getFechaHoraReserva() { return fechaHoraReserva; }
    public void setFechaHoraReserva(LocalDateTime fechaHoraReserva) { this.fechaHoraReserva = fechaHoraReserva; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public int getCantidadPersonas() { return cantidadPersonas; }
    public void setCantidadPersonas(int cantidadPersonas) { this.cantidadPersonas = cantidadPersonas; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public LocalDateTime getFechaCancelacion() { return fechaCancelacion; }
    public void setFechaCancelacion(LocalDateTime fechaCancelacion) { this.fechaCancelacion = fechaCancelacion; }
}