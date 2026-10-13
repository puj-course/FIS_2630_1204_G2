-- Catálogo de roles del sistema.
-- Idempotente: correrlo dos veces no duplica filas.
INSERT INTO roles (nombre_rol, descripcion)
SELECT v.nombre, v.descripcion
FROM (VALUES
    ('ADMINISTRADOR', 'Acceso completo al sistema'),
    ('MESERO',        'Gestión de pedidos y atención de mesas'),
    ('CAJERO',        'Gestión de pagos y facturación'),
    ('COCINA',        'Gestión de pedidos en cocina')
) AS v(nombre, descripcion)
WHERE NOT EXISTS (
    SELECT 1 FROM roles r WHERE r.nombre_rol = v.nombre
);
