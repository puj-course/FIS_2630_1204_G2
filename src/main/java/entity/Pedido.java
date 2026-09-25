package entity;

import java.time.LocalDateTime;

public class Pedido {

    private Long id;
    private String numeroPedido;
    private Long mesaId;
    private Long usuarioId;
    private String productos = "[]";
    private EstadoPedido estado = EstadoPedido.PENDIENTE;
    private LocalDateTime fechaPedido = LocalDateTime.now();

    public Pedido() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroPedido() { return numeroPedido; }
    public void setNumeroPedido(String numeroPedido) { this.numeroPedido = numeroPedido; }

    public Long getMesaId() { return mesaId; }
    public void setMesaId(Long mesaId) { this.mesaId = mesaId; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getProductos() { return productos; }
    public void setProductos(String productos) { this.productos = productos; }

    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }

    public LocalDateTime getFechaPedido() { return fechaPedido; }
    public void setFechaPedido(LocalDateTime fechaPedido) { this.fechaPedido = fechaPedido; }
}