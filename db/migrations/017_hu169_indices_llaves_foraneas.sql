-- HU-169: indexar las llaves foraneas que no tienen indice.
--
-- PostgreSQL crea un indice automatico para la llave primaria y para las
-- restricciones UNIQUE, pero no para las llaves foraneas. Sin el, cada borrado
-- o actualizacion en la tabla padre tiene que recorrer la tabla hija entera
-- para comprobar que no queden filas huerfanas, y los cruces entre las dos
-- tablas se resuelven mas lento de lo necesario.
--
-- De las 37 llaves foraneas del esquema, 18 ya estaban indexadas y 19 no.
-- Esta migracion crea las 19 que faltaban.
--
-- Es idempotente: IF NOT EXISTS permite volver a correr el script sobre una
-- base que ya los tiene.
BEGIN;

-- detalle_pedido: el cruce mas frecuente del sistema. Se recorre cada vez que
-- se abre un pedido o se arma una cuenta.
CREATE INDEX IF NOT EXISTS ix_detalle_pedido_pedido_id
    ON detalle_pedido (pedido_id);
CREATE INDEX IF NOT EXISTS ix_detalle_pedido_plato_id
    ON detalle_pedido (plato_id);

-- pedidos
CREATE INDEX IF NOT EXISTS ix_pedidos_mesa_id
    ON pedidos (mesa_id);
CREATE INDEX IF NOT EXISTS ix_pedidos_cliente_id
    ON pedidos (cliente_id);
CREATE INDEX IF NOT EXISTS ix_pedidos_usuario_id
    ON pedidos (usuario_id);
CREATE INDEX IF NOT EXISTS ix_pedidos_cancelado_por
    ON pedidos (cancelado_por);

-- pagos
CREATE INDEX IF NOT EXISTS ix_pagos_usuario_id
    ON pagos (usuario_id);

-- detalle_pedido_adicional
CREATE INDEX IF NOT EXISTS ix_detalle_pedido_adicional_adicional_id
    ON detalle_pedido_adicional (adicional_id);

-- alerta_inventario
CREATE INDEX IF NOT EXISTS ix_alerta_inventario_ingrediente_id
    ON alerta_inventario (ingrediente_id);
CREATE INDEX IF NOT EXISTS ix_alerta_inventario_producto_id
    ON alerta_inventario (producto_id);

-- historial_estado_mesa
CREATE INDEX IF NOT EXISTS ix_historial_estado_mesa_estado_anterior_id
    ON historial_estado_mesa (estado_anterior_id);
CREATE INDEX IF NOT EXISTS ix_historial_estado_mesa_estado_nuevo_id
    ON historial_estado_mesa (estado_nuevo_id);

-- mesas
CREATE INDEX IF NOT EXISTS ix_mesas_id_estado_mesa
    ON mesas (id_estado_mesa);

-- ingredientes
CREATE INDEX IF NOT EXISTS ix_ingredientes_unidad_medida_id
    ON ingredientes (unidad_medida_id);
CREATE INDEX IF NOT EXISTS ix_ingredientes_proveedor_id
    ON ingredientes (proveedor_id);
CREATE INDEX IF NOT EXISTS ix_ingredientes_categoria_ingrediente_id
    ON ingredientes (categoria_ingrediente_id);

-- plato_insumo
CREATE INDEX IF NOT EXISTS ix_plato_insumo_insumo_id
    ON plato_insumo (insumo_id);

-- clientes
CREATE INDEX IF NOT EXISTS ix_clientes_id_usuario_cuenta
    ON clientes (id_usuario_cuenta);

-- reservas
CREATE INDEX IF NOT EXISTS ix_reservas_id_cliente
    ON reservas (id_cliente);

COMMIT;
