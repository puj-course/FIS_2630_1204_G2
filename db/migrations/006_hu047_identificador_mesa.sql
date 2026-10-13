-- HU-047: identificador visible y unico para cada mesa.
--
-- `codigo_mesa` ya era UNIQUE pero admitia NULL, y `numero_mesa` solo es unico
-- dentro de su zona (uk_mesas_num_zona), asi que dos mesas de zonas distintas
-- podian terminar mostrandose con el mismo identificador en el mapa de salon.
-- Esta migracion completa el codigo donde falta y lo vuelve obligatorio, para
-- que el identificador que ve el mesero sea unico por construccion.
--
-- Requiere que la tabla `mesas` ya exista.

-- Asigna 'M-<zona>-<numero>' a las mesas sin codigo. Si ese valor ya estuviera
-- ocupado por un codigo cargado a mano, se desempata con el id de la mesa.
DO $$
DECLARE
    fila     RECORD;
    base     TEXT;
    asignado TEXT;
BEGIN
    FOR fila IN
        SELECT id_mesa, id_zona, numero_mesa
          FROM mesas
         WHERE codigo_mesa IS NULL OR btrim(codigo_mesa) = ''
         ORDER BY id_mesa
    LOOP
        base := 'M-' || LPAD(fila.id_zona::text, 2, '0')
                     || '-' || LPAD(fila.numero_mesa::text, 2, '0');
        asignado := base;

        IF EXISTS (SELECT 1 FROM mesas WHERE codigo_mesa = asignado) THEN
            asignado := base || '-' || fila.id_mesa;
        END IF;

        UPDATE mesas SET codigo_mesa = asignado WHERE id_mesa = fila.id_mesa;
    END LOOP;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_mesas_codigo_no_vacio'
    ) THEN
        ALTER TABLE mesas
            ADD CONSTRAINT ck_mesas_codigo_no_vacio
            CHECK (btrim(codigo_mesa) <> '');
    END IF;
END
$$;

-- Se deja NOT NULL solo si ya no quedan mesas sin codigo.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM mesas WHERE codigo_mesa IS NULL) THEN
        ALTER TABLE mesas
            ALTER COLUMN codigo_mesa SET NOT NULL;
    ELSE
        RAISE NOTICE 'Hay mesas sin codigo_mesa: asignelos y luego aplique SET NOT NULL.';
    END IF;
END
$$;

COMMENT ON COLUMN mesas.codigo_mesa IS
    'Identificador unico que se muestra al mesero en el mapa de salon.';
