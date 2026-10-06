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
VALUES (
           1,
           'MESA-001',
           4,
           1,
           1,
           'QR-MESA-001',
           1,
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP,
           'HU-088'
       );