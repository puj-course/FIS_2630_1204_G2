-- HU-137 / HU-181
-- Se conserva la relacion entre reservas y clientes.
-- Los catalogos de proveedores y categorias de ingredientes
-- fueron retirados mediante HU-181.

-- Columna de referencia en reservas.
ALTER TABLE reservas
    ADD COLUMN IF NOT EXISTS id_cliente BIGINT;

-- FK de reservas hacia clientes.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_reservas_cliente'
    ) THEN
ALTER TABLE reservas
    ADD CONSTRAINT fk_reservas_cliente
        FOREIGN KEY (id_cliente)
            REFERENCES clientes(id_cliente);
END IF;
END $$;