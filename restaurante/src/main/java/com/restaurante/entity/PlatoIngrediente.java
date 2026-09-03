    package com.restaurante.entity;

    import jakarta.persistence.*;
    import java.math.BigDecimal;

    @Entity
    @Table(
            name = "plato_ingrediente",
            indexes = {
                    @Index(
                            name = "idx_plato_ingrediente_plato",
                            columnList = "plato_id"
                    ),
                    @Index(
                            name = "idx_plato_ingrediente_ingrediente",
                            columnList = "ingrediente_id"
                    )
            }
    )
    public class PlatoIngrediente {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "plato_id", nullable = false)
        private Plato plato;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "ingrediente_id", nullable = false)
        private Ingrediente ingrediente;

        @Column(
                name = "cantidad_receta",
                precision = 12,
                scale = 4,
                nullable = false
        )
        private BigDecimal cantidadReceta;

        public PlatoIngrediente() {
        }

        public Long getId() {
            return id;
        }

        public Plato getPlato() {
            return plato;
        }

        public void setPlato(Plato plato) {
            this.plato = plato;
        }

        public Ingrediente getIngrediente() {
            return ingrediente;
        }

        public void setIngrediente(Ingrediente ingrediente) {
            this.ingrediente = ingrediente;
        }

        public BigDecimal getCantidadReceta() {
            return cantidadReceta;
        }

        public void setCantidadReceta(BigDecimal cantidadReceta) {
            this.cantidadReceta = cantidadReceta;
        }
    }