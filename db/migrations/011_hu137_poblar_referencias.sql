-- HU-137 / HU-181
-- Se conserva la relacion entre reservas y clientes.
-- Se eliminan las referencias a proveedores y categorias
-- de ingredientes porque ya no forman parte del sistema.

-- Reservas: relacionar con un cliente si el nombre coincide.
UPDATE reservas r
SET id_cliente = cl.id_cliente
    FROM clientes cl
WHERE r.nombre_cliente IS NOT NULL
  AND TRIM(r.nombre_cliente) = TRIM(cl.nombre)
  AND r.id_cliente IS NULL;