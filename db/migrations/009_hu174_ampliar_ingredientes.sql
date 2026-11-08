-- HU-174: ampliar el catálogo de ingredientes de prueba / operación.
-- Idempotente: no inserta nombres que ya existan (uq_ingredientes_nombre).

INSERT INTO ingredientes (
    nombre,
    descripcion,
    unidad_medida_id,
    costo_unitario,
    stock_actual,
    stock_minimo,
    stock_maximo,
    estado
)
VALUES
    (
        'Tomate',
        'Tomate fresco para ensaladas y salsas',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'KG'),
        4500,
        15.0000,
        3.0000,
        30.0000,
        'ACTIVO'
    ),
    (
        'Cebolla',
        'Cebolla cabezona',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'KG'),
        2800,
        12.0000,
        2.0000,
        25.0000,
        'ACTIVO'
    ),
    (
        'Ajo',
        'Ajo fresco',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'KG'),
        8000,
        5.0000,
        1.0000,
        10.0000,
        'ACTIVO'
    ),
    (
        'Aceite de oliva',
        'Aceite de oliva para cocina',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'L'),
        22000,
        8.0000,
        2.0000,
        15.0000,
        'ACTIVO'
    ),
    (
        'Sal',
        'Sal de cocina',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'KG'),
        1500,
        20.0000,
        5.0000,
        40.0000,
        'ACTIVO'
    ),
    (
        'Pimienta negra',
        'Pimienta negra molida',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'G'),
        50,
        500.0000,
        100.0000,
        1000.0000,
        'ACTIVO'
    ),
    (
        'Lechuga',
        'Lechuga fresca',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'UND'),
        2000,
        30.0000,
        8.0000,
        50.0000,
        'ACTIVO'
    ),
    (
        'Papa',
        'Papa para frituras y acompañamientos',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'KG'),
        3200,
        25.0000,
        5.0000,
        50.0000,
        'ACTIVO'
    ),
    (
        'Arroz',
        'Arroz blanco',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'KG'),
        4500,
        40.0000,
        10.0000,
        80.0000,
        'ACTIVO'
    ),
    (
        'Huevo',
        'Huevo fresco',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'UND'),
        600,
        60.0000,
        15.0000,
        120.0000,
        'ACTIVO'
    ),
    (
        'Mantequilla',
        'Mantequilla para cocina',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'G'),
        40,
        2000.0000,
        400.0000,
        4000.0000,
        'ACTIVO'
    ),
    (
        'Limon',
        'Limón tahití',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'KG'),
        5000,
        8.0000,
        2.0000,
        15.0000,
        'ACTIVO'
    )
ON CONFLICT (nombre) DO NOTHING;