INSERT INTO ingredientes
(
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
        'Carne de res',
        'Carne de res para hamburguesas',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'KG'),
        25000,
        10.0000,
        2.0000,
        20.0000,
        'ACTIVO'
    ),
    (
        'Pan de hamburguesa',
        'Pan para hamburguesas',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'UND'),
        1500,
        20.0000,
        5.0000,
        40.0000,
        'ACTIVO'
    ),
    (
        'Pollo',
        'Pechuga de pollo',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'KG'),
        18000,
        10.0000,
        2.0000,
        20.0000,
        'ACTIVO'
    ),
    (
        'Queso',
        'Queso para hamburguesas',
        (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = 'G'),
        20,
        1000.0000,
        200.0000,
        2000.0000,
        'ACTIVO'
    )
    ON CONFLICT DO NOTHING;