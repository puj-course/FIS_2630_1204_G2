-- =====================================================================
-- GastroFlow — datos de prueba para demostrar HU-28, HU-35 y HU-39
--
-- Uso:
--   psql -U postgres -d gastroflow -v ON_ERROR_STOP=1 -f db/seed.sql
--
-- Requiere que las 17 tablas de db/ddl y las funciones de db/functions
-- ya esten creadas.
--
-- Es idempotente: se puede correr varias veces sin duplicar nada.
-- No usa identificadores fijos; todo se resuelve por su llave natural,
-- asi que funciona sobre una base que ya tenga datos.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. Catalogos
-- ---------------------------------------------------------------------

INSERT INTO roles (nombre_rol, descripcion) VALUES
    ('ADMINISTRADOR', 'Gestiona inventario, productos, recetas y configuracion'),
    ('MESERO',        'Toma pedidos y administra mesas'),
    ('COCINERO',      'Recibe comandas y prepara los platos'),
    ('CAJERO',        'Cobra precuentas, aplica descuentos y cierra pagos')
ON CONFLICT (nombre_rol) DO NOTHING;

INSERT INTO tipos_documento (codigo, nombre) VALUES
    ('CC',  'Cedula de ciudadania'),
    ('CE',  'Cedula de extranjeria'),
    ('PAS', 'Pasaporte'),
    ('NIT', 'Numero de identificacion tributaria')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO zonas (nombre_zona, descripcion) VALUES
    ('Salon Principal', 'Area interior principal'),
    ('Terraza',         'Area exterior cubierta'),
    ('Bar',             'Barra y mesas altas')
ON CONFLICT (nombre_zona) DO NOTHING;

INSERT INTO estados_mesa (codigo_estado, descripcion) VALUES
    ('DISPONIBLE',   'Mesa libre'),
    ('OCUPADA',      'Mesa con pedido en curso'),
    ('RESERVADA',    'Mesa reservada'),
    ('MANTENIMIENTO','Mesa fuera de servicio')
ON CONFLICT (codigo_estado) DO NOTHING;

INSERT INTO unidades_medida (codigo, nombre, abreviatura, tipo) VALUES
    ('KG',  'Kilogramo', 'kg',  'MASA'),
    ('G',   'Gramo',     'g',   'MASA'),
    ('L',   'Litro',     'L',   'VOLUMEN'),
    ('ML',  'Mililitro', 'ml',  'VOLUMEN'),
    ('UND', 'Unidad',    'und', 'UNIDAD')
ON CONFLICT (codigo) DO NOTHING;

-- El orden_presentacion es lo que respeta el menu del mesero (HU-39).
INSERT INTO categorias (nombre, descripcion, orden_presentacion) VALUES
    ('Entradas',       'Para compartir y abrir el apetito', 1),
    ('Platos fuertes', 'Preparaciones principales',         2),
    ('Bebidas',        'Frias y calientes',                 3),
    ('Postres',        'Para cerrar',                       4)
ON CONFLICT (nombre) DO NOTHING;

-- ---------------------------------------------------------------------
-- 2. Personas
--    La contrasena y el PIN son marcadores: aun no hay autenticacion
--    real, la sesion de caja se pasa por propiedad del sistema.
-- ---------------------------------------------------------------------

INSERT INTO usuarios (codigo_empleado, id_tipo_documento, numero_documento,
                      nombre, apellido, correo, password_hash, pin_acceso_hash,
                      id_rol, is_on_shift, created_by)
SELECT v.codigo_empleado,
       (SELECT id_tipo_documento FROM tipos_documento WHERE codigo = 'CC'),
       v.numero_documento, v.nombre, v.apellido, v.correo,
       'PENDIENTE_HASH', 'PENDIENTE_HASH',
       (SELECT id_rol FROM roles WHERE nombre_rol = v.rol),
       v.turno, 'seed'
FROM (VALUES
    ('EMP-001', '1010101010', 'Laura',  'Restrepo', 'laura.restrepo@gastroflow.local',  'ADMINISTRADOR', 1),
    ('EMP-002', '1020202020', 'Andres', 'Molina',   'andres.molina@gastroflow.local',   'MESERO',        1),
    ('EMP-003', '1030303030', 'Camila', 'Rojas',    'camila.rojas@gastroflow.local',    'COCINERO',      1),
    ('EMP-004', '1040404040', 'Daniel', 'Ospina',   'daniel.ospina@gastroflow.local',   'CAJERO',        1),
    ('EMP-005', '1050505050', 'Sofia',  'Cardenas', 'sofia.cardenas@gastroflow.local',  'MESERO',        0)
) AS v(codigo_empleado, numero_documento, nombre, apellido, correo, rol, turno)
ON CONFLICT (codigo_empleado) DO NOTHING;

INSERT INTO clientes (id_tipo_documento, numero_documento, nombre, apellido,
                      correo, telefono, created_by)
SELECT (SELECT id_tipo_documento FROM tipos_documento WHERE codigo = 'CC'),
       v.doc, v.nombre, v.apellido, v.correo, v.tel, 'seed'
FROM (VALUES
    ('2010101010', 'Marcela', 'Guzman', 'marcela.guzman@correo.local', '3001112233'),
    ('2020202020', 'Felipe',  'Nieto',  'felipe.nieto@correo.local',   '3004445566')
) AS v(doc, nombre, apellido, correo, tel)
ON CONFLICT (numero_documento) DO NOTHING;

INSERT INTO mesas (numero_mesa, codigo_mesa, capacidad, id_zona, id_estado_mesa, created_by)
SELECT v.numero, v.codigo, v.capacidad,
       (SELECT id_zona FROM zonas WHERE nombre_zona = v.zona),
       (SELECT id_estado_mesa FROM estados_mesa WHERE codigo_estado = v.estado),
       'seed'
FROM (VALUES
    (1, 'MSA-01', 4, 'Salon Principal', 'OCUPADA'),
    (2, 'MSA-02', 4, 'Salon Principal', 'DISPONIBLE'),
    (3, 'MSA-03', 2, 'Salon Principal', 'OCUPADA'),
    (4, 'MSA-04', 6, 'Salon Principal', 'DISPONIBLE'),
    (5, 'MSA-05', 4, 'Terraza',         'OCUPADA'),
    (6, 'MSA-06', 2, 'Terraza',         'RESERVADA'),
    (7, 'MSA-07', 2, 'Bar',             'DISPONIBLE'),
    (8, 'MSA-08', 2, 'Bar',             'MANTENIMIENTO')
) AS v(numero, codigo, capacidad, zona, estado)
ON CONFLICT (codigo_mesa) DO NOTHING;

-- ---------------------------------------------------------------------
-- 3. Inventario
--
--    Los stocks NO son arbitrarios, estan puestos para que la demo
--    muestre cada caso:
--      Carne de res 0.400 kg  -> alcanza para la hamburguesa clasica
--                                (0.200) y para la doble (0.300) por
--                                separado, pero NO para las dos juntas.
--                                Es el caso que atrapa fn_pedido_faltantes.
--      Lechuga      0.000 kg  -> deja la Ensalada Cesar en AGOTADO sola.
-- ---------------------------------------------------------------------

INSERT INTO ingredientes (nombre, descripcion, unidad_medida_id, costo_unitario,
                          stock_actual, stock_minimo, stock_maximo, proveedor, categoria)
SELECT v.nombre, v.descripcion,
       (SELECT unidad_medida_id FROM unidades_medida WHERE codigo = v.unidad),
       v.costo, v.stock, v.minimo, v.maximo, v.proveedor, v.categoria
FROM (VALUES
    ('Carne de res',         'Molida 80/20',            'KG',  28000, 0.400,  2.000, 20.000, 'Carnes del Valle',  'Carnicos'),
    ('Pan de hamburguesa',   'Brioche',                 'UND',  1200, 50.000, 20.000, 200.000,'Panaderia Central', 'Panaderia'),
    ('Queso cheddar',        'Tajado',                  'KG',  32000, 2.000,  0.500, 10.000, 'Lacteos Andinos',   'Lacteos'),
    ('Papa',                 'Para freir',              'KG',   3200, 10.000, 3.000, 50.000, 'Agro Sabana',       'Verduras'),
    ('Lechuga',              'Romana',                  'KG',   6500, 0.000,  1.000, 8.000,  'Agro Sabana',       'Verduras'),
    ('Tomate',               'Chonto',                  'KG',   4800, 3.000,  1.000, 15.000, 'Agro Sabana',       'Verduras'),
    ('Cafe en grano',        'Tostado medio',           'KG',  38000, 5.000,  1.000, 20.000, 'Cafe de Origen',    'Bebidas'),
    ('Leche',                'Entera',                  'L',    4200, 8.000,  4.000, 40.000, 'Lacteos Andinos',   'Lacteos'),
    ('Harina de trigo',      'Todo uso',                'KG',   4500, 4.000,  2.000, 25.000, 'Molinos del Sur',   'Abarrotes'),
    ('Chocolate semiamargo', '60% cacao',               'KG',  42000, 1.000,  0.500, 6.000,  'Chocolates PUJ',    'Reposteria'),
    ('Limon',                'Tahiti',                  'KG',   3800, 6.000,  2.000, 20.000, 'Agro Sabana',       'Frutas'),
    ('Azucar',               'Blanca refinada',         'KG',   3500, 7.000,  2.000, 30.000, 'Molinos del Sur',   'Abarrotes')
) AS v(nombre, descripcion, unidad, costo, stock, minimo, maximo, proveedor, categoria)
ON CONFLICT (nombre) DO NOTHING;

-- ---------------------------------------------------------------------
-- 4. Catalogo del menu
--    Se insertan sin receta y las recetas se cargan aparte, porque
--    referencian ingrediente_id y hay que resolverlo por nombre.
-- ---------------------------------------------------------------------

INSERT INTO productos (codigo, nombre, descripcion, categoria_id,
                       precio_venta, costo, stock_actual, stock_minimo, estado)
SELECT v.codigo, v.nombre, v.descripcion,
       (SELECT categoria_id FROM categorias WHERE nombre = v.categoria),
       v.precio, v.costo, v.stock, v.minimo, v.estado
FROM (VALUES
    ('ENT-CESAR',  'Ensalada Cesar',          'Lechuga romana, queso y aderezo de la casa',      'Entradas',       18000,  6200, 0,  0, 'DISPONIBLE'),
    ('ENT-PAPAS',  'Papas a la francesa',     'Corte clasico, con salsas',                      'Entradas',       12000,  2400, 0,  0, 'DISPONIBLE'),
    ('FUE-HAMCLA', 'Hamburguesa clasica',     'Carne, queso cheddar y pan brioche',             'Platos fuertes', 28000,  9800, 0,  0, 'DISPONIBLE'),
    ('FUE-HAMDOB', 'Hamburguesa doble',       'Doble carne y doble queso',                      'Platos fuertes', 38000, 14600, 0,  0, 'DISPONIBLE'),
    ('BEB-CAFAME', 'Cafe americano',          'Cafe de origen, 8 onzas',                        'Bebidas',         5000,   900, 0,  0, 'DISPONIBLE'),
    ('BEB-LIMNAT', 'Limonada natural',        'Limon Tahiti, servida en jarra',                 'Bebidas',         7000,  1500, 25, 5, 'DISPONIBLE'),
    ('POS-BROWNI', 'Brownie con helado',      'Brownie de chocolate semiamargo',                'Postres',        14000,  4700, 0,  0, 'DISPONIBLE'),
    ('POS-TORZAN', 'Torta de zanahoria',      'Fuera de carta por decision del administrador',  'Postres',        13000,  4300, 0,  0, 'INACTIVO')
) AS v(codigo, nombre, descripcion, categoria, precio, costo, stock, minimo, estado)
ON CONFLICT (codigo) DO NOTHING;

-- Recetas. La cantidad corresponde a UNA unidad del producto.
UPDATE productos SET ingredientes = jsonb_build_array(
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Lechuga'),              'cantidad', 0.150),
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Queso cheddar'),        'cantidad', 0.030))
 WHERE codigo = 'ENT-CESAR';

UPDATE productos SET ingredientes = jsonb_build_array(
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Papa'),                 'cantidad', 0.250))
 WHERE codigo = 'ENT-PAPAS';

UPDATE productos SET ingredientes = jsonb_build_array(
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Carne de res'),         'cantidad', 0.200),
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Pan de hamburguesa'),   'cantidad', 1),
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Queso cheddar'),        'cantidad', 0.050))
 WHERE codigo = 'FUE-HAMCLA';

UPDATE productos SET ingredientes = jsonb_build_array(
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Carne de res'),         'cantidad', 0.300),
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Pan de hamburguesa'),   'cantidad', 1),
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Queso cheddar'),        'cantidad', 0.100))
 WHERE codigo = 'FUE-HAMDOB';

UPDATE productos SET ingredientes = jsonb_build_array(
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Cafe en grano'),        'cantidad', 0.020))
 WHERE codigo = 'BEB-CAFAME';

-- Limonada natural: sin receta a proposito. Un producto con receta NULL
-- no depende del inventario de ingredientes y se evalua por su propio
-- stock_actual.
UPDATE productos SET ingredientes = NULL WHERE codigo = 'BEB-LIMNAT';

UPDATE productos SET ingredientes = jsonb_build_array(
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Harina de trigo'),      'cantidad', 0.100),
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Chocolate semiamargo'), 'cantidad', 0.050),
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Azucar'),               'cantidad', 0.060))
 WHERE codigo = 'POS-BROWNI';

UPDATE productos SET ingredientes = jsonb_build_array(
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Harina de trigo'),      'cantidad', 0.120),
        jsonb_build_object('ingrediente_id', (SELECT ingrediente_id FROM ingredientes WHERE nombre='Azucar'),               'cantidad', 0.080))
 WHERE codigo = 'POS-TORZAN';

-- Historial de precios. productos.precio_venta sigue siendo el vigente.
INSERT INTO precios_producto (producto_id, precio, fecha_inicio, fecha_fin, motivo)
SELECT p.producto_id, v.precio, v.desde, v.hasta, v.motivo
FROM (VALUES
    ('FUE-HAMCLA', 24000, TIMESTAMP '2026-01-15 00:00:00', TIMESTAMP '2026-06-30 23:59:59', 'Precio de apertura'),
    ('FUE-HAMCLA', 28000, TIMESTAMP '2026-07-01 00:00:00', NULL,                             'Ajuste por costo de insumos'),
    ('FUE-HAMDOB', 34000, TIMESTAMP '2026-01-15 00:00:00', TIMESTAMP '2026-06-30 23:59:59', 'Precio de apertura'),
    ('FUE-HAMDOB', 38000, TIMESTAMP '2026-07-01 00:00:00', NULL,                             'Ajuste por costo de insumos')
) AS v(codigo, precio, desde, hasta, motivo)
JOIN productos p ON p.codigo = v.codigo
WHERE NOT EXISTS (
    SELECT 1 FROM precios_producto pp
    WHERE pp.producto_id = p.producto_id AND pp.fecha_inicio = v.desde
);

-- ---------------------------------------------------------------------
-- 5. Kardex de inventario (HU-022): cada movimiento con su responsable
-- ---------------------------------------------------------------------

INSERT INTO movimientos_inventario (ingrediente_id, tipo_movimiento, cantidad, motivo, usuario_id, fecha_movimiento)
SELECT i.ingrediente_id, v.tipo, v.cantidad, v.motivo,
       (SELECT id_usuario FROM usuarios WHERE codigo_empleado = v.empleado),
       v.fecha
FROM (VALUES
    ('Carne de res',  'ENTRADA', 5.000, 'Compra a proveedor - factura FV-1201', 'EMP-001', TIMESTAMP '2026-09-01 08:15:00'),
    ('Carne de res',  'SALIDA',  4.600, 'Consumo del servicio del fin de semana','EMP-001', TIMESTAMP '2026-09-05 22:40:00'),
    ('Lechuga',       'ENTRADA', 3.000, 'Compra a proveedor - factura FV-1202',  'EMP-001', TIMESTAMP '2026-09-01 08:20:00'),
    ('Lechuga',       'SALIDA',  3.000, 'Consumo y merma por deterioro',         'EMP-001', TIMESTAMP '2026-09-06 19:10:00'),
    ('Queso cheddar', 'ENTRADA', 4.000, 'Compra a proveedor - factura FV-1203',  'EMP-001', TIMESTAMP '2026-09-02 09:00:00'),
    ('Queso cheddar', 'SALIDA',  2.000, 'Consumo del servicio',                  'EMP-001', TIMESTAMP '2026-09-06 21:30:00')
) AS v(ingrediente, tipo, cantidad, motivo, empleado, fecha)
JOIN ingredientes i ON i.nombre = v.ingrediente
WHERE NOT EXISTS (
    SELECT 1 FROM movimientos_inventario mi
    WHERE mi.ingrediente_id = i.ingrediente_id
      AND mi.fecha_movimiento = v.fecha
      AND mi.motivo = v.motivo
);

-- ---------------------------------------------------------------------
-- 6. Pedidos para la demo de caja (HU-35)
--
--    PED-0001  COMPLETADO, sin pagar  -> este es el que se cobra en la demo
--    PED-0002  ENTREGADO,  ya pagado  -> muestra el aviso de cuenta pagada
--    PED-0003  PENDIENTE              -> pedido en curso
--    PED-0004  CANCELADO              -> la busqueda no lo debe encontrar
--
--    El impuesto es el 8% de impoconsumo sobre el subtotal.
-- ---------------------------------------------------------------------

INSERT INTO pedidos (numero_pedido, cliente_id, mesa_id, usuario_id, productos,
                     fecha_pedido, estado, subtotal, descuento, impuestos, propina, total,
                     observaciones, inventario_descontado)
SELECT v.numero,
       (SELECT id_cliente FROM clientes WHERE numero_documento = v.doc_cliente),
       (SELECT id_mesa    FROM mesas    WHERE codigo_mesa      = v.mesa),
       (SELECT id_usuario FROM usuarios WHERE codigo_empleado  = v.mesero),
       '[]'::jsonb,
       v.fecha, v.estado, v.subtotal, 0, v.impuestos, 0, v.subtotal + v.impuestos,
       v.observaciones, 'N'
FROM (VALUES
    ('PED-0001', '2010101010', 'MSA-03', 'EMP-002', TIMESTAMP '2026-09-07 13:05:00', 'COMPLETADO', 66000, 5280, 'Sin cebolla en la hamburguesa'),
    ('PED-0002', '2020202020', 'MSA-05', 'EMP-002', TIMESTAMP '2026-09-07 12:20:00', 'ENTREGADO',  40000, 3200, NULL),
    ('PED-0003', NULL,         'MSA-01', 'EMP-005', TIMESTAMP '2026-09-07 13:40:00', 'PENDIENTE',  19000, 1520, 'Para llevar'),
    ('PED-0004', NULL,         'MSA-07', 'EMP-002', TIMESTAMP '2026-09-07 11:10:00', 'CANCELADO',  12000,  960, 'El cliente se retiro')
) AS v(numero, doc_cliente, mesa, mesero, fecha, estado, subtotal, impuestos, observaciones)
ON CONFLICT (numero_pedido) DO NOTHING;

-- Lineas de cada pedido, en las dos representaciones que hoy conviven
-- en el esquema: pedidos.productos (JSONB) y detalle_pedido.
-- Cuando el equipo decida cual queda, se elimina la otra de aqui.
UPDATE pedidos ped
   SET productos = (
        SELECT jsonb_agg(jsonb_build_object(
                    'producto_id', p.producto_id,
                    'cantidad',    l.cantidad))
        FROM (VALUES
            ('PED-0001', 'FUE-HAMCLA', 1),
            ('PED-0001', 'FUE-HAMDOB', 1),
            ('PED-0001', 'BEB-LIMNAT', 0),
            ('PED-0002', 'ENT-PAPAS',  2),
            ('PED-0002', 'BEB-CAFAME', 2),
            ('PED-0003', 'ENT-CESAR',  1),
            ('PED-0004', 'ENT-PAPAS',  1)
        ) AS l(numero, codigo, cantidad)
        JOIN productos p ON p.codigo = l.codigo
        WHERE l.numero = ped.numero_pedido AND l.cantidad > 0)
 WHERE ped.numero_pedido IN ('PED-0001','PED-0002','PED-0003','PED-0004');

INSERT INTO detalle_pedido (pedido_id, plato_id, cantidad, precio_unitario, subtotal)
SELECT ped.pedido_id, p.producto_id, l.cantidad, p.precio_venta,
       l.cantidad * p.precio_venta
FROM (VALUES
    ('PED-0001', 'FUE-HAMCLA', 1),
    ('PED-0001', 'FUE-HAMDOB', 1),
    ('PED-0002', 'ENT-PAPAS',  2),
    ('PED-0002', 'BEB-CAFAME', 2),
    ('PED-0003', 'ENT-CESAR',  1),
    ('PED-0004', 'ENT-PAPAS',  1)
) AS l(numero, codigo, cantidad)
JOIN pedidos   ped ON ped.numero_pedido = l.numero
JOIN productos p   ON p.codigo = l.codigo
WHERE NOT EXISTS (
    SELECT 1 FROM detalle_pedido dp
    WHERE dp.pedido_id = ped.pedido_id AND dp.plato_id = p.producto_id
);

-- PED-0002 ya esta cobrado, con propina y con un descuento justificado.
-- El CHECK de pagos exige que todo descuento tenga motivo y que
-- total_pagado = subtotal + impuestos + propina - descuento.
INSERT INTO pagos (pedido_id, usuario_id, subtotal, descuento, impuestos, propina,
                   motivo_descuento, total_pagado, estado, fecha_pago)
SELECT ped.pedido_id,
       (SELECT id_usuario FROM usuarios WHERE codigo_empleado = 'EMP-004'),
       40000, 4000, 3200, 4000,
       'Cortesia por demora en la entrega',
       40000 + 3200 + 4000 - 4000,
       'PAGADO', TIMESTAMP '2026-09-07 13:15:00'
FROM pedidos ped
WHERE ped.numero_pedido = 'PED-0002'
  AND NOT EXISTS (SELECT 1 FROM pagos pg WHERE pg.pedido_id = ped.pedido_id);

-- Deja pedidos coherente con el pago registrado.
UPDATE pedidos
   SET descuento = 4000, propina = 4000, total = 40000 + 3200 + 4000 - 4000
 WHERE numero_pedido = 'PED-0002';

COMMIT;

-- ---------------------------------------------------------------------
-- 7. Recalcular la disponibilidad y mostrar el resultado
-- ---------------------------------------------------------------------

SELECT fn_recalcular_estados_productos() AS productos_que_cambiaron_de_estado;

\echo ''
\echo '--- Menu como lo vera el mesero (HU-39) ---'
SELECT c.orden_presentacion AS orden, c.nombre AS categoria,
       p.codigo, p.nombre, p.precio_venta, p.estado
FROM productos p
JOIN categorias c ON c.categoria_id = p.categoria_id
WHERE p.estado IN ('DISPONIBLE','AGOTADO') AND c.estado = 'ACTIVO'
ORDER BY c.orden_presentacion, c.nombre, p.nombre;

\echo ''
\echo '--- HU-28: el pedido completo NO alcanza aunque cada plato si ---'
\echo 'Hamburguesa clasica sola:'
SELECT fn_producto_disponible_cantidad((SELECT producto_id FROM productos WHERE codigo='FUE-HAMCLA'), 1) AS alcanza;
\echo 'Hamburguesa doble sola:'
SELECT fn_producto_disponible_cantidad((SELECT producto_id FROM productos WHERE codigo='FUE-HAMDOB'), 1) AS alcanza;
\echo 'Las dos en el mismo pedido:'
SELECT * FROM fn_pedido_faltantes(
    (SELECT jsonb_agg(jsonb_build_object('producto_id', producto_id, 'cantidad', 1))
     FROM productos WHERE codigo IN ('FUE-HAMCLA','FUE-HAMDOB')));

\echo ''
\echo '--- HU-35: cuentas disponibles para cobrar en caja ---'
SELECT p.numero_pedido, m.numero_mesa, p.estado,
       p.subtotal, p.impuestos,
       EXISTS (SELECT 1 FROM pagos pg WHERE pg.pedido_id = p.pedido_id AND pg.estado='PAGADO') AS pagada
FROM pedidos p
LEFT JOIN mesas m ON m.id_mesa = p.mesa_id
WHERE p.estado <> 'CANCELADO'
ORDER BY p.numero_pedido;

\echo ''
\echo '--- Id del cajero para arrancar la aplicacion ---'
SELECT u.id_usuario AS cajero_id, u.nombre || ' ' || u.apellido AS cajero
FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol
WHERE UPPER(r.nombre_rol) = 'CAJERO' AND u.is_active = 1;
