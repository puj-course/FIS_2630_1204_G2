-----------------------------------------------------------------------------------
--- FUNCIONES Y TRIGGERS ACTUALIZADOS (GastroFlow) --------------------------------
-----------------------------------------------------------------------------------

-- ===== 1. Limpieza automática de notas al liberar una mesa =====
CREATE OR REPLACE FUNCTION limpiar_nota_al_liberar_mesa()
RETURNS TRIGGER AS $$
DECLARE
v_codigo_nuevo VARCHAR(20);
BEGIN
SELECT codigo_estado INTO v_codigo_nuevo
FROM estados_mesa
WHERE id_estado_mesa = NEW.id_estado_mesa;

IF v_codigo_nuevo = 'DISPONIBLE' THEN
DELETE FROM nota_mesa WHERE id_mesa = NEW.id_mesa;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Recrear el trigger de mesas de forma segura
DROP TRIGGER IF EXISTS trg_limpiar_nota_al_liberar_mesa ON mesas;
CREATE TRIGGER trg_limpiar_nota_al_liberar_mesa
    AFTER UPDATE OF id_estado_mesa ON mesas
    FOR EACH ROW
    EXECUTE FUNCTION limpiar_nota_al_liberar_mesa();


-- ===== 2. Disponibilidad de productos basada en producto_ingrediente =====
CREATE OR REPLACE FUNCTION fn_producto_disponible_cantidad(
    p_producto_id BIGINT,
    p_cantidad    INTEGER
)
RETURNS BOOLEAN
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
v_estado       VARCHAR(15);
    v_insuficiente INTEGER;
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

    -- Verifica si hay ingredientes asociados que no cumplan con el stock actual requerido
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


-- ===== 3. Generación de motivo de faltantes para la receta =====
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


-- ===== 4. Recalcular estado individual de producto =====
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

    -- INACTIVO es una decisión administrativa y no se altera automáticamente
    IF v_estado_actual = 'INACTIVO' THEN
        RETURN 'INACTIVO';
END IF;

    v_estado_nuevo := CASE
        WHEN fn_producto_disponible_cantidad(p_producto_id, 1) THEN 'DISPONIBLE'
        ELSE 'AGOTADO'
END;

    IF v_estado_nuevo IS DISTINCT FROM v_estado_actual THEN
UPDATE productos
SET estado = fecha_actualizacion = CURRENT_TIMESTAMP,
    estado = v_estado_nuevo
WHERE producto_id = p_producto_id;
END IF;

RETURN v_estado_nuevo;
END;
$$;


-- ===== 5. Recalcular estado de todos los productos activos =====
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


-- ===== 6. Validación de faltantes de stock por pedido =====
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
    -- 1. Productos inexistentes o marcados como inactivos en el menú
    invalidos AS (
        SELECT COALESCE(p.nombre, 'producto #' || li.producto_id)::VARCHAR AS faltante,
               li.cantidad AS requerido,
               0::NUMERIC  AS disponible
          FROM lineas li
          LEFT JOIN productos p ON p.producto_id = li.producto_id
         WHERE p.producto_id IS NULL
            OR p.estado = 'INACTIVO'
    ),
    -- 2. Consumo acumulado de ingredientes según las cantidades de los productos del pedido
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
    -- 3. Ingredientes cuyo stock actual sea insuficiente o no se encuentren activos
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