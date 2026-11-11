-- HU-082: usuario de prueba para el login web del backend.
-- Ejecutar DESPUÉS de iniciar una vez Spring Boot, para que Hibernate cree la tabla usuario.

-- HU-082: usuario de prueba para el login web del backend.
-- Adaptado al esquema actual de la base de datos de GastroFlow.

INSERT INTO usuarios (
    codigo_empleado,
    id_tipo_documento,
    numero_documento,
    nombre,
    apellido,
    correo,
    password_hash,
    id_rol,
    is_active
)
SELECT
    'CAJ-001',
    (SELECT id_tipo_documento FROM tipos_documento WHERE codigo = 'CC'),
    '1004000001', -- Número de documento de prueba requerido por la restricción NOT NULL
    'Ana',
    'Cajera',
    'ana.cajera@gastroflow.local', -- Correo único requerido por la tabla
    'pbkdf2_sha256$120000$mMgPWRcvpqq2H5kaAZwZ/w==$5KXOc+Xd3+C/mcgAztcibr8lnuWizs6yZLdCgJxAupU=',
    (SELECT id_rol FROM roles WHERE nombre_rol = 'CAJERO'),
    1
    WHERE NOT EXISTS (
    SELECT 1 FROM usuarios WHERE codigo_empleado = 'CAJ-001'
)
AND EXISTS (SELECT 1 FROM tipos_documento WHERE codigo = 'CC')
AND EXISTS (SELECT 1 FROM roles WHERE nombre_rol = 'CAJERO');

-- Credenciales de prueba:
-- Código: CAJ-001
-- Contraseña: Cajero123!