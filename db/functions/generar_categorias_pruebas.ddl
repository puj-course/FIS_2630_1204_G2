INSERT INTO categorias
(nombre, descripcion, orden_visual, estado)
VALUES
    ('Hamburguesas', 'Hamburguesas y productos relacionados', 1, 'ACTIVO')
    ON CONFLICT DO NOTHING;