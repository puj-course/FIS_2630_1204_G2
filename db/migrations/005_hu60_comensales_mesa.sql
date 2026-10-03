-- HU-60: advertencia cuando los comensales superan la capacidad de la mesa.
-- Agrega `cantidad_comensales` a `mesas` para que la asignacion quede guardada,
-- y deja la regla de capacidad tambien en la base: la interfaz advierte, pero el
-- CHECK impide que una asignacion invalida llegue a persistirse por otra via.
--
-- Requiere que la tabla `mesas` ya exista.

ALTER TABLE mesas
    ADD COLUMN IF NOT EXISTS cantidad_comensales SMALLINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_mesas_comensales_capacidad'
    ) THEN
        ALTER TABLE mesas
            ADD CONSTRAINT ck_mesas_comensales_capacidad
            CHECK (
                cantidad_comensales IS NULL
                OR (cantidad_comensales > 0 AND cantidad_comensales <= capacidad)
            );
    END IF;
END
$$;

COMMENT ON COLUMN mesas.cantidad_comensales IS
    'Comensales de la atencion en curso. NULL cuando la mesa no esta ocupada.';
