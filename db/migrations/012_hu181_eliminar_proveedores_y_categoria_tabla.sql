
BEGIN;

-- 1. Eliminar la relacion con proveedores.
ALTER TABLE ingredientes
DROP CONSTRAINT IF EXISTS fk_ingredientes_proveedor;

ALTER TABLE ingredientes
DROP COLUMN IF EXISTS proveedor_id;

ALTER TABLE ingredientes
DROP COLUMN IF EXISTS proveedor;

-- 2. Eliminar la relacion con el catalogo de categorias.
ALTER TABLE ingredientes
DROP CONSTRAINT IF EXISTS fk_ingredientes_categoria;

ALTER TABLE ingredientes
DROP COLUMN IF EXISTS categoria_ingrediente_id;

-- 3. Eliminar completamente la categoria de ingredientes.
ALTER TABLE ingredientes
DROP COLUMN IF EXISTS categoria;

-- 4. Eliminar los catalogos que ya no se utilizan.
DROP TABLE IF EXISTS proveedores;
DROP TABLE IF EXISTS categoria_ingrediente;

COMMIT;
