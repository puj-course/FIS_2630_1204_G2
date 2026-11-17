-- =============================================================================
-- Productos de prueba. La categoría se resuelve por nombre de forma idempotente.
-- =============================================================================
INSERT INTO productos
(
    codigo,
    nombre,
    descripcion,
    categoria_id,
    precio_venta,
    costo,
    stock_minimo,
    stock_maximo,
    estado
)
SELECT
    v.codigo,
    v.nombre,
    v.descripcion,
    (SELECT categoria_id FROM categorias WHERE nombre = 'Hamburguesas'),
    v.precio_venta,
    v.costo,
    v.stock_minimo,
    v.stock_maximo,
    v.estado
FROM (VALUES
          (
              'HAM-001',
              'Hamburguesa Clásica',
              'Hamburguesa de carne de res con pan y queso',
              18000.00,
              9000.00,
              0.0000,
              NULL::NUMERIC(12,4),
              'DISPONIBLE'
          ),
          (
              'HAM-002',
              'Hamburguesa de Pollo',
              'Hamburguesa de pollo con pan y queso',
              17000.00,
              8500.00,
              0.0000,
              NULL::NUMERIC(12,4),
              'DISPONIBLE'
          )
     ) AS v(codigo, nombre, descripcion, precio_venta, costo, stock_minimo, stock_maximo, estado)
WHERE NOT EXISTS (
    SELECT 1 FROM productos p WHERE p.codigo = v.codigo
);