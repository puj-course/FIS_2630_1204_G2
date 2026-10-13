-- Productos de prueba. La categoria se resuelve por nombre en lugar de fijar el
-- id en 1. Idempotente: correrlo dos veces no duplica filas.
INSERT INTO productos
(
    codigo,
    nombre,
    descripcion,
    categoria_id,
    precio_venta,
    costo,
    ingredientes,
    stock_actual,
    stock_minimo,
    stock_maximo,
    estado
)
SELECT v.* FROM (VALUES
    (
        'HAM-001',
        'Hamburguesa Clásica',
        'Hamburguesa de carne de res con pan y queso',
        (SELECT categoria_id FROM categorias WHERE nombre = 'Hamburguesas'),
        18000,
        9000,
        '[
            {"ingrediente_id": 1, "cantidad": 0.250},
            {"ingrediente_id": 2, "cantidad": 1},
            {"ingrediente_id": 4, "cantidad": 50}
        ]'::jsonb,
        0,
        0,
        NULL::NUMERIC(12,4),
        'DISPONIBLE'
    ),
    (
        'HAM-002',
        'Hamburguesa de Pollo',
        'Hamburguesa de pollo con pan y queso',
        (SELECT categoria_id FROM categorias WHERE nombre = 'Hamburguesas'),
        17000,
        8500,
        '[
            {"ingrediente_id": 3, "cantidad": 0.250},
            {"ingrediente_id": 2, "cantidad": 1},
            {"ingrediente_id": 4, "cantidad": 50}
        ]'::jsonb,
        0,
        0,
        NULL::NUMERIC(12,4),
        'DISPONIBLE'
    )
) AS v(codigo, nombre, descripcion, categoria_id, precio_venta, costo,
       ingredientes, stock_actual, stock_minimo, stock_maximo, estado)
WHERE NOT EXISTS (
    SELECT 1 FROM productos p WHERE p.codigo = v.codigo
);
