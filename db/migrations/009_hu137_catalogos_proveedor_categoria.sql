-- HU-137 · Catálogos proveedor y categoría de insumo
-- Idempotente: se puede ejecutar más de una vez.

-- Columnas de referencia en ingredientes (el texto proveedor/categoria se conserva)
ALTER TABLE ingredientes
    ADD COLUMN IF NOT EXISTS proveedor_id BIGINT;

ALTER TABLE ingredientes
    ADD COLUMN IF NOT EXISTS categoria_ingrediente_id BIGINT;

-- FKs (solo si no existen)
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_ingredientes_proveedor'
        ) THEN
            ALTER TABLE ingredientes
                ADD CONSTRAINT fk_ingredientes_proveedor
                    FOREIGN KEY (proveedor_id) REFERENCES proveedores(proveedor_id);
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_ingredientes_categoria_ingrediente'
        ) THEN
            ALTER TABLE ingredientes
                ADD CONSTRAINT fk_ingredientes_categoria_ingrediente
                    FOREIGN KEY (categoria_ingrediente_id)
                        REFERENCES categoria_ingrediente(categoria_ingrediente_id);
        END IF;
    END $$;

-- Columna de referencia en reservas (nombre_cliente se conserva; FK admite NULL)
ALTER TABLE reservas
    ADD COLUMN IF NOT EXISTS id_cliente BIGINT;

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_reservas_cliente'
        ) THEN
            ALTER TABLE reservas
                ADD CONSTRAINT fk_reservas_cliente
                    FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente);
        END IF;
    END $$;