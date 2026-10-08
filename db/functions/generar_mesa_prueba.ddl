-- Mesa de prueba para HU-088.
-- La zona y el estado se resuelven por nombre en lugar de fijar el id en 1: los
-- catálogos se siembran con IDENTITY y el id depende del orden de inserción.
-- Idempotente: correrlo dos veces no duplica la mesa.
INSERT INTO mesas (
    numero_mesa,
    codigo_mesa,
    capacidad,
    id_zona,
    id_estado_mesa,
    codigo_qr,
    is_active,
    created_at,
    updated_at,
    created_by
)
SELECT
    1,
    'MESA-001',
    4,
    (SELECT id_zona FROM zonas WHERE nombre_zona = 'Salón principal'),
    (SELECT id_estado_mesa FROM estados_mesa WHERE codigo_estado = 'DISPONIBLE'),
    'QR-MESA-001',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'HU-088'
WHERE NOT EXISTS (
    SELECT 1 FROM mesas WHERE codigo_mesa = 'MESA-001'
)
AND EXISTS (SELECT 1 FROM zonas WHERE nombre_zona = 'Salón principal')
AND EXISTS (SELECT 1 FROM estados_mesa WHERE codigo_estado = 'DISPONIBLE');
