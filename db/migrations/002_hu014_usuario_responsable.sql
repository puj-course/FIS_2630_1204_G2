-- HU-014: trazabilidad de los movimientos de inventario.
-- Agrega el usuario responsable que exige el criterio de aceptacion y unifica
-- la precision de `cantidad` con los campos de stock, que son NUMERIC(12,4).
--
-- Requiere que la tabla `usuarios` ya exista.

ALTER TABLE movimientos_inventario
    ADD COLUMN IF NOT EXISTS usuario_id BIGINT;

ALTER TABLE movimientos_inventario
    ALTER COLUMN cantidad TYPE NUMERIC(12,4);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_movimientos_usuario'
    ) THEN
        ALTER TABLE movimientos_inventario
            ADD CONSTRAINT fk_movimientos_usuario
            FOREIGN KEY (usuario_id) REFERENCES usuarios(id_usuario);
    END IF;
END
$$;

-- Se deja NOT NULL solo si no quedan filas historicas sin responsable.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM movimientos_inventario WHERE usuario_id IS NULL) THEN
        ALTER TABLE movimientos_inventario
            ALTER COLUMN usuario_id SET NOT NULL;
    ELSE
        RAISE NOTICE 'Hay movimientos sin usuario_id: asignelos y luego aplique SET NOT NULL.';
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS ix_movimientos_ingrediente
    ON movimientos_inventario (ingrediente_id, fecha_movimiento);

CREATE INDEX IF NOT EXISTS ix_movimientos_producto
    ON movimientos_inventario (producto_id, fecha_movimiento);

CREATE INDEX IF NOT EXISTS ix_movimientos_usuario
    ON movimientos_inventario (usuario_id, fecha_movimiento);
