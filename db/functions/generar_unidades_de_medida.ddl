INSERT INTO unidades_medida
(codigo, nombre, abreviatura, tipo)
VALUES
    ('KG',  'Kilogramo', 'kg', 'MASA'),
    ('G',   'Gramo',     'g',  'MASA'),
    ('L',   'Litro',     'L',  'VOLUMEN'),
    ('ML',  'Mililitro', 'ml', 'VOLUMEN'),
    ('UND', 'Unidad',    'und', 'UNIDAD'),
    ('PORC','Porcion',   'por', 'UNIDAD')
    ON CONFLICT (codigo) DO NOTHING;