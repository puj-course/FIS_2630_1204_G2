-- Zona de prueba para HU-088.
-- Idempotente: correrlo dos veces no duplica la fila.
INSERT INTO zonas (nombre_zona, descripcion, is_active)
SELECT 'Salón principal', 'Zona de prueba para HU-088', 1
WHERE NOT EXISTS (
    SELECT 1 FROM zonas WHERE nombre_zona = 'Salón principal'
);
