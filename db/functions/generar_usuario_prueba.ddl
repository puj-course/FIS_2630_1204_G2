-- Usuario de prueba para HU-088.
-- El tipo de documento y el rol se resuelven por código en lugar de fijar el id:
-- los catálogos usan IDENTITY y el id depende del orden de inserción.
-- Idempotente: correrlo dos veces no duplica la fila.
INSERT INTO usuarios (
    codigo_empleado,
    id_tipo_documento,
    numero_documento,
    nombre,
    apellido,
    correo,
    password_hash,
    pin_acceso_hash,
    id_rol,
    is_on_shift,
    is_active,
    created_at,
    updated_at,
    created_by
)
SELECT
    'HU088-001',
    (SELECT id_tipo_documento FROM tipos_documento WHERE codigo = 'CC'),
    '1000000001',
    'Usuario',
    'Prueba',
    'hu088.prueba@restaurante.com',
    'HU088_TEST',
    'HU088_TEST',
    (SELECT id_rol FROM roles WHERE nombre_rol = 'MESERO'),
    1,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'HU-088'
WHERE NOT EXISTS (
    SELECT 1 FROM usuarios u WHERE u.codigo_empleado = 'HU088-001'
)
AND EXISTS (SELECT 1 FROM tipos_documento WHERE codigo = 'CC')
AND EXISTS (SELECT 1 FROM roles WHERE nombre_rol = 'MESERO');
