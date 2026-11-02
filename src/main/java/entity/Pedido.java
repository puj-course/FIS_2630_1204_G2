package entity;

import enums.EstadoPedido;
import entity.Cliente;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Pedido {

    private Long id;
    private Long mesaId;
    private Long clienteId;
    private Long usuarioId;
    private String productos = "[]";
    private EstadoPedido estado = EstadoPedido.PENDIENTE;
    private LocalDateTime fechaPedido = LocalDateTime.now();
    private String observaciones;

    /*
     * Indica si los ingredientes de este pedido
     * ya fueron descontados del inventario.
     *
     * Se utiliza para evitar que una segunda confirmación
     * vuelva a descontar los mismos ingredientes.
     */
    private boolean inventarioDescontado = false;

    public Pedido() {
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getMesaId() {
        return mesaId;
    }
    public void setMesaId(Long mesaId) {
        this.mesaId = mesaId;
    }

    public Long getClienteId() {
        return clienteId;
    }
    public void setClienteId(Long clienteId) {
        this.clienteId = usuarioId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }
    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getProductos() {
        return productos;
    }
    public void setProductos(String productos) {
        this.productos = productos;
    }

    public EstadoPedido getEstado() {
        return estado;
    }
    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaPedido() {
        return fechaPedido;
    }
    public void setFechaPedido(LocalDateTime fechaPedido) {
        this.fechaPedido = fechaPedido;
    }

    public boolean isInventarioDescontado() {
        return inventarioDescontado;
    }
    public void setInventarioDescontado(boolean inventarioDescontado) {
        this.inventarioDescontado = inventarioDescontado;
    }

    public String getObservaciones() {
        return observaciones;
    }
    public void setObservacioness() {
        this.observaciones = observaciones;
    }
}