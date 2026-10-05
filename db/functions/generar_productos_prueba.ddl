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
VALUES
    (
        'HAM-001',
        'Hamburguesa Clásica',
        'Hamburguesa de carne de res con pan y queso',
        1,
        18000,
        9000,
        '[
            {"ingrediente_id": 1, "cantidad": 0.250},
            {"ingrediente_id": 2, "cantidad": 1},
            {"ingrediente_id": 4, "cantidad": 50}
        ]'::jsonb,
        0,
        0,
        NULL,
        'DISPONIBLE'
    ),
    (
        'HAM-002',
        'Hamburguesa de Pollo',
        'Hamburguesa de pollo con pan y queso',
        1,
        17000,
        8500,
        '[
            {"ingrediente_id": 3, "cantidad": 0.250},
            {"ingrediente_id": 2, "cantidad": 1},
            {"ingrediente_id": 4, "cantidad": 50}
        ]'::jsonb,
        0,
        0,
        NULL,
        'DISPONIBLE'
    );