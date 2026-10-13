-- =============================================================================
-- AJUSTE: tabla reservas
-- HU-055: se necesita registrar cuándo se canceló una reserva para auditoría
-- =============================================================================

-- IF NOT EXISTS para que la migracion se pueda correr dos veces sin fallar,
-- como dice el README que son todas.
ALTER TABLE reservas ADD COLUMN IF NOT EXISTS fecha_cancelacion TIMESTAMP;
