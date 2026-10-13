-- HU-082: usuario de prueba para el login web del backend.
-- Ejecutar DESPUÉS de iniciar una vez Spring Boot, para que Hibernate cree la tabla usuario.

INSERT INTO usuario (codigo_empleado, nombre, apellido, password_hash, rol, is_active)
SELECT 'CAJ-001', 'Ana', 'Cajera',
       'pbkdf2_sha256$120000$mMgPWRcvpqq2H5kaAZwZ/w==$5KXOc+Xd3+C/mcgAztcibr8lnuWizs6yZLdCgJxAupU=',
       'CAJERO', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM usuario WHERE codigo_empleado = 'CAJ-001'
);

-- Credenciales de prueba:
-- Código: CAJ-001
-- Contraseña: Cajero123!
