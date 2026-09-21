package com.restaurante.entity;

import com.restaurante.enums.EstadoMesa;
import jakarta.persistence.*;

@Entity
@Table(
        name = "mesa",
        indexes = {
                @Index(
                        name = "idx_mesa_estado",
                        columnList = "estado"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_mesa_numero",
                        columnNames = "numero"
                )
        }
)
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMesa estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesero_id")
    private Mesero mesero;

    public Mesa() {
        this.estado = EstadoMesa.DISPONIBLE;
    }

    public Long getId() {
        return id;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public EstadoMesa getEstado() {
        return estado;
    }

    public void setEstado(EstadoMesa estado) {
        this.estado = estado;
    }

    public Mesero getMesero() {
        return mesero;
    }

    public void setMesero(Mesero mesero) {
        this.mesero = mesero;
    }

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
