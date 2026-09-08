-- HU-35: gestión de propinas y descuentos.
-- Agrega a `pedidos` la columna `propina` que requiere PagoDAO
-- (SELECT p.propina ... / UPDATE pedidos SET descuento = ?, propina = ?, total = ?).
--
-- El motivo del descuento NO va en `pedidos`: PagoDAO lo escribe únicamente en
-- `pagos.motivo_descuento`, que es donde queda el registro del pago.
--
-- Este script solo es necesario en bases de datos ya creadas.
-- `pedidos.ddl` ya incluye la columna para instalaciones nuevas.

ALTER TABLE pedidos
    ADD COLUMN IF NOT EXISTS propina NUMERIC(12,2) DEFAULT 0 NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_pedido_propina'
    ) THEN
        ALTER TABLE pedidos
            ADD CONSTRAINT ck_pedido_propina CHECK (propina >= 0);
    END IF;
END
$$;
