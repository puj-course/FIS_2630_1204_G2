package entity;

import enums.EstadoListaEspera;

import java.time.LocalDateTime;

public class ListaEspera {
    private Long id;
    private String nombreCliente;
    private int cantidadPersonas;
    private LocalDateTime horaIngreso;
    private EstadoListaEspera estado;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public int getCantidadPersonas() { return cantidadPersonas; }
    public void setCantidadPersonas(int cantidadPersonas) { this.cantidadPersonas = cantidadPersonas; }

    public LocalDateTime getHoraIngreso() { return horaIngreso; }
    public void setHoraIngreso(LocalDateTime horaIngreso) { this.horaIngreso = horaIngreso; }

    public EstadoListaEspera getEstado() { return estado; }
    public void setEstado(EstadoListaEspera estado) { this.estado = estado; }
}
