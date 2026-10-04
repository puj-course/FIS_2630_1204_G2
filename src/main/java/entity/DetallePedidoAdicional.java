package entity;

public class DetallePedidoAdicional {

    private Long id;
    private Long detallePedidoId;
    private Long adicionalId;
    private Integer cantidad;

    public DetallePedidoAdicional() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDetallePedidoId() { return detallePedidoId; }
    public void setDetallePedidoId(Long detallePedidoId) { this.detallePedidoId = detallePedidoId; }

    public Long getAdicionalId() { return adicionalId; }
    public void setAdicionalId(Long adicionalId) { this.adicionalId = adicionalId; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}