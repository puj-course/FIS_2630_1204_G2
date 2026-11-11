-- HU-171: eliminar las columnas que ninguna consulta usa.
--
-- Se revisaron las 221 columnas del esquema contra los archivos .java del
-- proyecto. Treinta y una no aparecen en ninguna consulta, pero la mayoria no
-- se puede borrar: son llaves primarias o foraneas de tablas que todavia no
-- estan conectadas al codigo, o columnas que la propia base llena con un valor
-- por defecto, como pagos.fecha_pago.
--
-- Esta migracion borra solo las seis que no cumplen ninguna funcion: no son
-- llave de nada, nadie las escribe, nadie las lee y no tienen un valor por
-- defecto que valga la pena conservar.
BEGIN;

-- Columnas de auditoria que nunca se llenaron. Son VARCHAR sueltos, no apuntan
-- a usuarios, y ninguna consulta del proyecto las escribe ni las lee.
ALTER TABLE usuarios DROP COLUMN IF EXISTS created_by;
ALTER TABLE clientes DROP COLUMN IF EXISTS created_by;
ALTER TABLE mesas    DROP COLUMN IF EXISTS created_by;

-- Codigo QR por mesa: se penso para que el cliente consultara la carta desde
-- el telefono. Nunca se implemento y no hay nada en el codigo que lo genere ni
-- lo muestre.
ALTER TABLE mesas DROP COLUMN IF EXISTS codigo_qr;

-- Marca de turno del usuario. Quedo en 0 para todos; el control de turnos
-- nunca se construyo.
ALTER TABLE usuarios DROP COLUMN IF EXISTS is_on_shift;

-- Segundo metodo de acceso por PIN. La autenticacion del sistema usa
-- password_hash; esta columna quedo vacia y sin codigo que la use.
ALTER TABLE usuarios DROP COLUMN IF EXISTS pin_acceso_hash;

COMMIT;
