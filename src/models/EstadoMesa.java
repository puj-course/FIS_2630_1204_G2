package com.restaurante.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "estados_mesa")
public class EstadoMesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_mesa")
    private Long id;

    @Column(name = "codigo_estado", nullable = false)
    private String codigoEstado;

    @Column
    private String descripcion;

    public EstadoMesa() {
    }

    public Long getId() {
        return id;
    }

    public String getCodigoEstado() {
        return codigoEstado;
    }

    public void setCodigoEstado(String codigoEstado) {
        this.codigoEstado = codigoEstado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
