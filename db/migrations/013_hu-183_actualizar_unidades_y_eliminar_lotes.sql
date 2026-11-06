-- HU-183: conservar la unidad de medida como texto libre
-- y eliminar las tablas y columnas que ya no utiliza la aplicación.

BEGIN;

-- 1. Asegurar que exista la columna de texto.
ALTER TABLE ingredientes
    ADD COLUMN IF NOT EXISTS unidad_medida VARCHAR(30);

-- 2. Recuperar las abreviaturas del catálogo anterior,
-- únicamente si todavía existen la tabla y la columna relacionadas.
DO $$
BEGIN
    IF to_regclass('public.unidades_medida') IS NOT NULL
       AND EXISTS (
           SELECT 1
           FROM information_schema.columns
           WHERE table_schema = 'public'
             AND table_name = 'ingredientes'
             AND column_name = 'unidad_medida_id'
       ) THEN

        EXECUTE $sql$
UPDATE ingredientes i
SET unidad_medida = u.abreviatura
    FROM unidades_medida u
WHERE i.unidad_medida_id = u.unidad_medida_id
  AND (
    i.unidad_medida IS NULL
   OR BTRIM(i.unidad_medida) = ''
    )
    $sql$;

END IF;
END
$$;

-- 3. Verificar que todos los ingredientes tengan una unidad.
DO $$
DECLARE
ingredientes_sin_unidad TEXT;
BEGIN
SELECT STRING_AGG(
               ingrediente_id || ' - ' || nombre,
               ', '
       )
INTO ingredientes_sin_unidad
FROM ingredientes
WHERE unidad_medida IS NULL
   OR BTRIM(unidad_medida) = '';

IF ingredientes_sin_unidad IS NOT NULL THEN
        RAISE EXCEPTION
            'Hay ingredientes sin unidad de medida: %',
            ingredientes_sin_unidad;
END IF;
END
$$;

-- 4. Establecer la unidad como obligatoria.
ALTER TABLE ingredientes
    ALTER COLUMN unidad_medida SET NOT NULL;

-- 5. Eliminar la relación con el catálogo anterior.
ALTER TABLE ingredientes
DROP CONSTRAINT IF EXISTS fk_ingredientes_unidad;

ALTER TABLE ingredientes
DROP COLUMN IF EXISTS unidad_medida_id;

-- 6. Eliminar las tablas que ya no se utilizarán.
DROP TABLE IF EXISTS lote_ingrediente CASCADE;
DROP TABLE IF EXISTS unidades_medida CASCADE;

-- 7. Eliminar los campos relacionados con lotes y vencimientos.
ALTER TABLE ingredientes
DROP COLUMN IF EXISTS lote;

ALTER TABLE ingredientes
DROP COLUMN IF EXISTS fecha_vencimiento;

-- 8. Eliminar el historial de precios no integrado.
DROP TABLE IF EXISTS precios_producto CASCADE;

COMMIT;