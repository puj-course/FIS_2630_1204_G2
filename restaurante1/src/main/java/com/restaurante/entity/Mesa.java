package com.restaurante.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "mesa",
        indexes = {
                @Index(
                        name = "idx_mesa_zona",
                        columnList = "zona_id"
                ),
                @Index(
                        name = "idx_mesa_estado",
                        columnList = "estado"
                )
        }
)
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_mesa", nullable = false)
    private Integer numeroMesa;

    @Column(name = "codigo_mesa")
    private String codigoMesa;

    @Column(nullable = false)
    private Integer capacidad = 2;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zona_id", nullable = false)
    private Zona zona;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMesa estado;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Mesa() {
        this.estado = EstadoMesa.DISPONIBLE;
    }

    public Long getId() {
        return id;
    }

    public Integer getNumeroMesa() {
        return numeroMesa;
    }

    public void setNumeroMesa(Integer numeroMesa) {
        this.numeroMesa = numeroMesa;
    }

    public String getCodigoMesa() {
        return codigoMesa;
    }

    public void setCodigoMesa(String codigoMesa) {
        this.codigoMesa = codigoMesa;
    }

    public Integer getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Integer capacidad) {
        this.capacidad = capacidad;
    }

    public Zona getZona() {
        return zona;
    }

    public void setZona(Zona zona) {
        this.zona = zona;
    }

    public EstadoMesa getEstado() {
        return estado;
    }

    public void setEstado(EstadoMesa estado) {
        this.estado = estado;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}

