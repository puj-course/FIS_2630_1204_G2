-- Asociar los ingredientes correspondientes a cada producto creado (Receta normalizada)
INSERT INTO producto_ingrediente (producto_id, ingrediente_id, cantidad_necesaria)
SELECT
    p.producto_id,
    i.ingrediente_id,
    v.cantidad
FROM (VALUES
          ('HAM-001', 'Carne de res',        0.2500),
          ('HAM-001', 'Pan de hamburguesa', 1.0000),
          ('HAM-001', 'Queso',             50.0000),
          ('HAM-002', 'Pollo',              0.2500),
          ('HAM-002', 'Pan de hamburguesa', 1.0000),
          ('HAM-002', 'Queso',             50.0000)
     ) AS v(codigo_producto, nombre_ingrediente, cantidad)
         JOIN productos p ON p.codigo = v.codigo_producto
         JOIN ingredientes i ON i.nombre = v.nombre_ingrediente
    ON CONFLICT (producto_id, ingrediente_id) DO NOTHING;