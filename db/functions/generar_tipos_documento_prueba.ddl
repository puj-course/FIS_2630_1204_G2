-- Catálogo de tipos de documento de identidad.
-- Idempotente: correrlo dos veces no duplica filas.
INSERT INTO tipos_documento (codigo, nombre)
SELECT v.codigo, v.nombre
FROM (VALUES
    ('CC', 'Cédula de Ciudadanía'),
    ('CE', 'Cédula de Extranjería'),
    ('PA', 'Pasaporte'),
    ('NIT','Número de Identificación Tributaria')
) AS v(codigo, nombre)
WHERE NOT EXISTS (
    SELECT 1 FROM tipos_documento t WHERE t.codigo = v.codigo
);
