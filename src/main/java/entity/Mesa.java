package entity;

public class Mesa {
    private int idMesa;
    private int numeroMesa;
    private String codigoMesa;
    private int capacidad;
    private int idZona;
    private String nombreZona;
    private int idEstadoMesa;
    private String codigoEstado;
    private Integer cantidadComensales;
    private String pedidoActivo;
    private String meseroResponsable;

    public Mesa() {}

    public int getIdMesa() { return idMesa; }
    public void setIdMesa(int idMesa) { this.idMesa = idMesa; }

    public int getNumeroMesa() { return numeroMesa; }
    public void setNumeroMesa(int numeroMesa) { this.numeroMesa = numeroMesa; }

    public String getCodigoMesa() { return codigoMesa; }
    public void setCodigoMesa(String codigoMesa) { this.codigoMesa = codigoMesa; }

    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

    public int getIdZona() { return idZona; }
    public void setIdZona(int idZona) { this.idZona = idZona; }

    public String getNombreZona() { return nombreZona; }
    public void setNombreZona(String nombreZona) { this.nombreZona = nombreZona; }

    public int getIdEstadoMesa() { return idEstadoMesa; }
    public void setIdEstadoMesa(int idEstadoMesa) { this.idEstadoMesa = idEstadoMesa; }

    public String getCodigoEstado() { return codigoEstado; }
    public void setCodigoEstado(String codigoEstado) { this.codigoEstado = codigoEstado; }

    public Integer getCantidadComensales() { return cantidadComensales; }
    public void setCantidadComensales(Integer cantidadComensales) { this.cantidadComensales = cantidadComensales; }

    public String getPedidoActivo() { return pedidoActivo; }
    public void setPedidoActivo(String pedidoActivo) { this.pedidoActivo = pedidoActivo; }

    public String getMeseroResponsable() { return meseroResponsable; }
    public void setMeseroResponsable(String meseroResponsable) { this.meseroResponsable = meseroResponsable; }
}
