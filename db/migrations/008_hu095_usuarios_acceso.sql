-- HU-095 · Acceso al sistema
--
-- Deja la base lista para iniciar sesión: los cuatro roles del restaurante, un
-- tipo de documento y un usuario de prueba por rol.
--
-- Formato de password_hash:  sha256$<salt>$<hash>
--   hash = SHA-256(salt || contraseña), en hexadecimal minúscula.
--
-- Usuarios de prueba (cámbielos antes de cualquier uso real):
--   ADM-001 / admin123    Administrador del restaurante
--   MES-001 / mesero123   Mesero
--   COC-001 / cocina123   Personal de cocina
--   CAJ-001 / cajero123   Cajero
--
-- Es idempotente: se puede volver a ejecutar sin duplicar filas.

-- ---------------------------------------------------------------- roles
INSERT INTO roles (nombre_rol, descripcion)
SELECT v.nombre, v.descripcion
FROM (VALUES
        ('ADMINISTRADOR', 'Administrador del restaurante'),
        ('MESERO',        'Atiende mesas y toma pedidos'),
        ('COCINA',        'Personal de cocina'),
        ('CAJERO',        'Cobra las cuentas')
     ) AS v(nombre, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.nombre_rol = v.nombre);

-- ------------------------------------------------- tipos de documento
INSERT INTO tipos_documento (codigo, nombre)
SELECT 'CC', 'Cédula de ciudadanía'
WHERE NOT EXISTS (SELECT 1 FROM tipos_documento t WHERE t.codigo = 'CC');

-- ------------------------------------------------------------ usuarios
INSERT INTO usuarios (codigo_empleado, id_tipo_documento, numero_documento,
                      nombre, apellido, correo, password_hash, id_rol,
                      is_on_shift, is_active, created_by)
SELECT v.codigo,
       (SELECT id_tipo_documento FROM tipos_documento WHERE codigo = 'CC'),
       v.documento, v.nombre, v.apellido, v.correo, v.hash,
       (SELECT id_rol FROM roles WHERE nombre_rol = v.rol),
       0, 1, 'hu095'
FROM (VALUES
        ('ADM-001', '1001', 'Ana',   'Restrepo', 'ana.restrepo@gastroflow.local',
         'sha256$gfadm001$be0a002aeb6496ee19a8b5ff31b88110b93e8759cc457e7e1c0ec166f3140020',
         'ADMINISTRADOR'),
        ('MES-001', '1002', 'Luis',  'Marin',    'luis.marin@gastroflow.local',
         'sha256$gfmes001$bfc0a8df5294655858428becb9b9337037ba6406d34b79730a89304127b74621',
         'MESERO'),
        ('COC-001', '1003', 'Sara',  'Pena',     'sara.pena@gastroflow.local',
         'sha256$gfcoc001$9d348142819c3ef93599bea2577e7d267abf96c0f6775b8d67f3e5c1d64fdc39',
         'COCINA'),
        ('CAJ-001', '1004', 'Diego', 'Torres',   'diego.torres@gastroflow.local',
         'sha256$gfcaj001$ab6eba63cd2c374ac982ecab801a6ff1ae3f7fb7b86b853bef47cd085004d513',
         'CAJERO')
     ) AS v(codigo, documento, nombre, apellido, correo, hash, rol)
WHERE NOT EXISTS (SELECT 1 FROM usuarios u WHERE u.codigo_empleado = v.codigo);
