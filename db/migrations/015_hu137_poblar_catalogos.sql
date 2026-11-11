-- HU-137 · Poblar catálogos desde texto libre existente (idempotente)

INSERT INTO proveedores (nombre)
SELECT DISTINCT TRIM(i.proveedor)
FROM ingredientes i
WHERE i.proveedor IS NOT NULL
  AND TRIM(i.proveedor) <> ''
  AND NOT EXISTS (
    SELECT 1 FROM proveedores p WHERE p.nombre = TRIM(i.proveedor)
);

INSERT INTO categoria_ingrediente (nombre)
SELECT DISTINCT TRIM(i.categoria)
FROM ingredientes i
WHERE i.categoria IS NOT NULL
  AND TRIM(i.categoria) <> ''
  AND NOT EXISTS (
    SELECT 1 FROM categoria_ingrediente c WHERE c.nombre = TRIM(i.categoria)
);