-- =============================================================================
-- HU-28 — Disponibilidad automática de platos
-- =============================================================================
-- Funciones que invoca DisponibilidadDAO:
--
--   fn_recalcular_estados_productos()                  -> INTEGER  (productos modificados)
--   fn_recalcular_estado_producto(producto_id)         -> VARCHAR  (estado resultante)
--   fn_producto_disponible_cantidad(producto_id, cant) -> BOOLEAN
--   fn_motivo_receta(ingredientes)                     -> TEXT     (NULL si no falta nada)
--
-- Reglas (según GastroFlow_HU28/README.md):
--   - AGOTADO   si al menos un ingrediente de la receta no tiene stock suficiente.
--   - DISPONIBLE si todos los ingredientes alcanzan.
--   - INACTIVO  es decisión administrativa: nunca se modifica automáticamente.
--
-- Formato esperado de productos.ingredientes:
--   [{"ingrediente_id": 1, "cantidad": 0.250}, {"ingrediente_id": 4, "cantidad": 2}]
--   La cantidad corresponde a UNA unidad del producto.
--   Receta NULL o [] => el producto no depende de inventario de ingredientes y se
--   evalúa por su propio productos.stock_actual.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- fn_motivo_receta: nombres de los ingredientes que faltan, o NULL si no falta ninguno.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_motivo_receta(p_receta JSONB)
RETURNS TEXT
LANGUAGE sql
STABLE
AS $$
    SELECT CASE
             WHEN COUNT(*) = 0 THEN NULL
             ELSE 'Falta: ' || string_agg(nombre, ', ' ORDER BY nombre)
           END
    FROM (
        SELECT COALESCE(i.nombre, 'ingrediente #' || (r->>'ingrediente_id')) AS nombre
        FROM jsonb_array_elements(
                 CASE WHEN jsonb_typeof(p_receta) = 'array' THEN p_receta ELSE '[]'::jsonb END
             ) AS r
        LEFT JOIN ingredientes i
               ON i.ingrediente_id = (r->>'ingrediente_id')::BIGINT
        WHERE i.ingrediente_id IS NULL                        -- ingrediente inexistente
           OR i.estado = 'INACTIVO'                           -- ingrediente dado de baja
           OR i.stock_actual < (r->>'cantidad')::NUMERIC      -- sin stock suficiente
    ) faltantes;
$$;


-- -----------------------------------------------------------------------------
-- fn_producto_disponible_cantidad: ¿alcanza el inventario para N unidades?
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
    v_estado  VARCHAR(15);
    v_receta  JSONB;
    v_stock   NUMERIC(12,4);
    v_faltan  INTEGER;
BEGIN
    IF p_cantidad IS NULL OR p_cantidad <= 0 THEN
        RETURN FALSE;
    END IF;

    SELECT estado, ingredientes, stock_actual
      INTO v_estado, v_receta, v_stock
      FROM productos
     WHERE producto_id = p_producto_id;

    IF NOT FOUND OR v_estado = 'INACTIVO' THEN
        RETURN FALSE;
    END IF;

    -- Producto sin receta: se rige por su propio stock.
    IF v_receta IS NULL
       OR jsonb_typeof(v_receta) <> 'array'
       OR jsonb_array_length(v_receta) = 0 THEN
        RETURN v_stock >= p_cantidad;
    END IF;

    SELECT COUNT(*)
      INTO v_faltan
      FROM jsonb_array_elements(v_receta) AS r
      LEFT JOIN ingredientes i
             ON i.ingrediente_id = (r->>'ingrediente_id')::BIGINT
     WHERE i.ingrediente_id IS NULL
        OR i.estado = 'INACTIVO'
        OR i.stock_actual < (r->>'cantidad')::NUMERIC * p_cantidad;

    RETURN v_faltan = 0;
END;
$$;


-- -----------------------------------------------------------------------------
-- fn_recalcular_estado_producto: ajusta el estado de un producto y lo devuelve.
-- Nunca toca los productos INACTIVO.
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

    -- INACTIVO es una decisión administrativa.
    IF v_estado_actual = 'INACTIVO' THEN
        RETURN 'INACTIVO';
    END IF;

    v_estado_nuevo := CASE
                        WHEN fn_producto_disponible_cantidad(p_producto_id, 1)
                        THEN 'DISPONIBLE'
                        ELSE 'AGOTADO'
                      END;

    IF v_estado_nuevo <> v_estado_actual THEN
        UPDATE productos
           SET estado              = v_estado_nuevo,
               fecha_actualizacion = CURRENT_TIMESTAMP
         WHERE producto_id = p_producto_id;
    END IF;

    RETURN v_estado_nuevo;
END;
$$;


-- -----------------------------------------------------------------------------
-- fn_recalcular_estados_productos: recalcula todo el catálogo.
-- Devuelve cuántos productos cambiaron de estado.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_recalcular_estados_productos()
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_producto      RECORD;
    v_estado_nuevo  VARCHAR(15);
    v_cambiados     INTEGER := 0;
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
