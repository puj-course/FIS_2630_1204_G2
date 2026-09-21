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

}