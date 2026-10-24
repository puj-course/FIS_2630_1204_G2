-- HU-137 · Rellenar FKs a partir de los catálogos (idempotente)

UPDATE ingredientes i
SET proveedor_id = p.proveedor_id
FROM proveedores p
WHERE i.proveedor IS NOT NULL
  AND TRIM(i.proveedor) = p.nombre
  AND (i.proveedor_id IS NULL OR i.proveedor_id IS DISTINCT FROM p.proveedor_id);

UPDATE ingredientes i
SET categoria_ingrediente_id = c.categoria_ingrediente_id
FROM categoria_ingrediente c
WHERE i.categoria IS NOT NULL
  AND TRIM(i.categoria) = c.nombre
  AND (i.categoria_ingrediente_id IS NULL
    OR i.categoria_ingrediente_id IS DISTINCT FROM c.categoria_ingrediente_id);

-- Reservas: solo si el nombre coincide con un cliente ya registrado
UPDATE reservas r
SET id_cliente = cl.id_cliente
FROM clientes cl
WHERE r.nombre_cliente IS NOT NULL
  AND TRIM(r.nombre_cliente) = TRIM(cl.nombre)
  AND r.id_cliente IS NULL;