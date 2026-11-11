-- =============================================================================
-- HU-28 / HU-134: Disponibilidad automática de productos.
-- Fuente única de recetas: plato_insumo.
-- productos.ingredientes se conserva temporalmente por compatibilidad.
-- =============================================================================

-- Cambiar la firma de la función de motivos:
-- ahora recibe el ID del producto, no una receta JSONB.
DROP FUNCTION IF EXISTS fn_motivo_receta(JSONB);

-- -----------------------------------------------------------------------------
-- fn_motivo_receta: devuelve los ingredientes que impiden preparar el producto.
-- Devuelve NULL cuando no hay faltantes.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_motivo_receta(p_producto_id BIGINT)
RETURNS TEXT
LANGUAGE sql
STABLE
AS $$
SELECT CASE
           WHEN COUNT(*) = 0 THEN NULL
           ELSE 'Falta: ' || string_agg(nombre, ', ' ORDER BY nombre)
           END
FROM (
         SELECT DISTINCT
             COALESCE(i.nombre, 'ingrediente #' || pi.ingrediente_id) AS nombre
         FROM producto_ingrediente pi
                  LEFT JOIN ingredientes i
                            ON i.ingrediente_id = pi.ingrediente_id
         WHERE pi.producto_id = p_producto_id
           AND (
             i.ingrediente_id IS NULL
                 OR i.estado = 'INACTIVO'
                 OR i.stock_actual < pi.cantidad_necesaria
             )
     ) faltantes;
$$;

-- -----------------------------------------------------------------------------
-- fn_producto_disponible_cantidad:
-- comprueba si existe stock para la cantidad solicitada.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_producto_disponible_cantidad(
    p_producto_id BIGINT,
    p_cantidad    INTEGER
)
RETURNS BOOLEAN
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
v_estado        VARCHAR(15);
    v_insuficiente  INTEGER;
BEGIN
    IF p_cantidad IS NULL OR p_cantidad <= 0 THEN
        RETURN FALSE;
END IF;

SELECT estado
INTO v_estado
FROM productos
WHERE producto_id = p_producto_id;

IF NOT FOUND OR v_estado = 'INACTIVO' THEN
        RETURN FALSE;
END IF;

SELECT COUNT(*)
INTO v_insuficiente
FROM producto_ingrediente pi
         JOIN ingredientes i ON i.ingrediente_id = pi.ingrediente_id
WHERE pi.producto_id = p_producto_id
  AND (
    i.estado = 'INACTIVO'
        OR i.stock_actual < (pi.cantidad_necesaria * p_cantidad)
    );

RETURN v_insuficiente = 0;
END;
$$;

-- -----------------------------------------------------------------------------
-- fn_recalcular_estado_producto:
-- nunca modifica productos INACTIVO.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_recalcular_estado_producto(p_producto_id BIGINT)
RETURNS VARCHAR
LANGUAGE plpgsql
AS $$
DECLARE
v_estado_actual VARCHAR(15);
    v_estado_nuevo  VARCHAR(15);
BEGIN
SELECT estado
INTO v_estado_actual
FROM productos
WHERE producto_id = p_producto_id
    FOR UPDATE;

IF NOT FOUND THEN
        RETURN NULL;
END IF;

    IF v_estado_actual = 'INACTIVO' THEN
        RETURN 'INACTIVO';
END IF;

    v_estado_nuevo := CASE
        WHEN fn_producto_disponible_cantidad(p_producto_id, 1) THEN 'DISPONIBLE'
        ELSE 'AGOTADO'
END;

    IF v_estado_nuevo IS DISTINCT FROM v_estado_actual THEN
UPDATE productos
SET estado = v_estado_nuevo,
    fecha_actualizacion = CURRENT_TIMESTAMP
WHERE producto_id = p_producto_id;
END IF;

RETURN v_estado_nuevo;
END;
$$;

-- -----------------------------------------------------------------------------
-- fn_recalcular_estados_productos:
-- devuelve el número de productos cuyo estado cambió.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_recalcular_estados_productos()
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
v_producto     RECORD;
    v_estado_nuevo VARCHAR(15);
    v_cambiados    INTEGER := 0;
BEGIN
FOR v_producto IN
SELECT producto_id, estado
FROM productos
WHERE estado <> 'INACTIVO'
ORDER BY producto_id
    LOOP
        v_estado_nuevo := fn_recalcular_estado_producto(v_producto.producto_id);

IF v_estado_nuevo IS DISTINCT FROM v_producto.estado THEN
            v_cambiados := v_cambiados + 1;
END IF;
END LOOP;

RETURN v_cambiados;
END;
$$;

-- -----------------------------------------------------------------------------
-- fn_pedido_faltantes:
-- valida todas las líneas del pedido y acumula el consumo compartido
-- de ingredientes entre productos.
--
-- Entrada:
-- [{"producto_id": 1, "cantidad": 2}, {"producto_id": 5, "cantidad": 1}]
--
-- Salida:
-- una fila por cada producto o ingrediente insuficiente.
-- Sin filas significa que el pedido puede prepararse.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_pedido_faltantes(p_pedido_id BIGINT)
RETURNS TABLE (
    faltante   VARCHAR,
    requerido  NUMERIC,
    disponible NUMERIC
)
LANGUAGE sql
STABLE
AS $$
    WITH lineas AS (
        SELECT dp.producto_id,
               dp.cantidad
          FROM detalle_pedido dp
         WHERE dp.pedido_id = p_pedido_id
    ),
    invalidos AS (
        SELECT COALESCE(p.nombre, 'producto #' || li.producto_id)::VARCHAR AS faltante,
               li.cantidad AS requerido,
               0::NUMERIC  AS disponible
          FROM lineas li
          LEFT JOIN productos p ON p.producto_id = li.producto_id
         WHERE p.producto_id IS NULL
            OR p.estado = 'INACTIVO'
    ),
    consumo AS (
        SELECT pi.ingrediente_id,
               SUM(pi.cantidad_necesaria * li.cantidad) AS requerido
          FROM lineas li
          JOIN productos p ON p.producto_id = li.producto_id
          JOIN producto_ingrediente pi ON pi.producto_id = p.producto_id
         WHERE p.estado <> 'INACTIVO'
           AND li.cantidad > 0
         GROUP BY pi.ingrediente_id
    ),
    faltantes_ingredientes AS (
        SELECT COALESCE(i.nombre, 'ingrediente #' || c.ingrediente_id)::VARCHAR AS faltante,
               c.requerido,
               COALESCE(i.stock_actual, 0)::NUMERIC AS disponible
          FROM consumo c
          LEFT JOIN ingredientes i ON i.ingrediente_id = c.ingrediente_id
         WHERE i.ingrediente_id IS NULL
            OR i.estado = 'INACTIVO'
            OR i.stock_actual < c.requerido
    )
SELECT * FROM invalidos
UNION ALL
SELECT * FROM faltantes_ingredientes;
$$;