-- Catálogo de estados de mesa. El codigo_estado es el que leen MesaRepository y
-- el mapa de salón; no se traduce ni se cambia de mayúsculas.
-- Idempotente: correrlo dos veces no duplica filas.
INSERT INTO estados_mesa (codigo_estado, descripcion)
SELECT v.codigo, v.descripcion
FROM (VALUES
    ('DISPONIBLE',   'Mesa disponible para recibir pedidos'),
    ('OCUPADA',      'Mesa con comensales sentados'),
    ('RESERVADA',    'Mesa apartada por una reserva'),
    ('MANTENIMIENTO','Mesa fuera de servicio')
) AS v(codigo, descripcion)
WHERE NOT EXISTS (
    SELECT 1 FROM estados_mesa e WHERE e.codigo_estado = v.codigo
);
