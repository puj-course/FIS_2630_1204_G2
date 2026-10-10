package entity;

import enums.EstadoPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Pedido {

    private Long id;
    private String numeroPedido;
    private Long mesaId;
    private Long usuarioId;
    private String productos = "[]";
    private EstadoPedido estado = EstadoPedido.PENDIENTE;
    private LocalDateTime fechaPedido = LocalDateTime.now();
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
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

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public void setNumeroPedido(String numeroPedido) {
        this.numeroPedido = numeroPedido;
    }

    public Long getMesaId() {
        return mesaId;
    }

    public void setMesaId(Long mesaId) {
        this.mesaId = mesaId;
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

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public boolean isInventarioDescontado() {
        return inventarioDescontado;
    }

    public void setInventarioDescontado(boolean inventarioDescontado) {
        this.inventarioDescontado = inventarioDescontado;
    }
}