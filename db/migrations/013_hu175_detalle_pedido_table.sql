-- HU-175: Migracion de datos de JSON a tabla detalle_pedido
-- Centrakización de administracion de datos sobre base de datos


-- Transacción para asegurar que la migración de datos sea atómica
BEGIN;

-- 1. Insertar en detalle_pedido los elementos que estaban en JSONB de pedidos.
INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT
    p.pedido_id,
    (item->>'producto_id')::BIGINT,
    (item->>'cantidad')::INTEGER,
    COALESCE((item->>'precio_unitario')::NUMERIC(12,2), 0.00)
FROM pedidos p,
     LATERAL jsonb_array_elements(p.productos) AS item
WHERE p.productos IS NOT NULL
  AND jsonb_typeof(p.productos) = 'array'
  AND jsonb_array_length(p.productos) > 0;

-- 2. (Opcional temporal) Permitir temporalmente nulos o limpiar la columna antes de alterarla
ALTER TABLE pedidos ALTER COLUMN productos DROP NOT NULL;

COMMIT;