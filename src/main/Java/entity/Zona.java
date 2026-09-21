package com.restaurante.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "zonas")
public class Zona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_zona")
    private Long id;

    @Column(name = "nombre_zona", nullable = false)
    private String nombreZona;

    @Column
    private String descripcion;

    @Column(name = "is_active", nullable = false)
    private Integer isActive = 1;

    public Zona() {
    }

    public Long getId() {
        return id;
    }

    public String getNombreZona() {
        return nombreZona;
    }

    public void setNombreZona(String nombreZona) {
        this.nombreZona = nombreZona;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer active) {
        isActive = active;
    }
}
