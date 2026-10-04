-- =============================================================================
-- AJUSTE: tabla reservas
-- HU-055: se necesita registrar cuándo se canceló una reserva para auditoría
-- =============================================================================

ALTER TABLE reservas ADD COLUMN fecha_cancelacion TIMESTAMP;
