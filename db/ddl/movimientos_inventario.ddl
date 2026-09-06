CREATE TABLE movimientos_inventario (
    id_movimiento      INTEGER GENERATED ALWAYS AS IDENTITY,
    producto_id        BIGINT,
    ingrediente_id     BIGINT,
    tipo_movimiento    VARCHAR(20) NOT NULL,
    cantidad           NUMERIC(12,3) NOT NULL,
    motivo             VARCHAR(255) NOT NULL,
    fecha_movimiento   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT pk_movimientos_inventario
        PRIMARY KEY (id_movimiento),

    CONSTRAINT fk_movimientos_producto
        FOREIGN KEY (producto_id)
        REFERENCES productos(producto_id),

    CONSTRAINT fk_movimientos_ingrediente
        FOREIGN KEY (ingrediente_id)
        REFERENCES ingredientes(ingrediente_id),

    CONSTRAINT chk_movimientos_tipo
        CHECK (tipo_movimiento IN ('ENTRADA', 'SALIDA')),

    CONSTRAINT chk_movimientos_cantidad
        CHECK (cantidad > 0),

    CONSTRAINT chk_movimientos_item
        CHECK (
            (producto_id IS NOT NULL AND ingrediente_id IS NULL)
            OR
            (producto_id IS NULL AND ingrediente_id IS NOT NULL)
        )
);
