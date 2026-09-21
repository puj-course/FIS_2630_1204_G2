-- =============================================================================
-- TABLA: reservas
-- HU-054: visualizar mesas reservadas con hora, cliente y cantidad de personas
-- =============================================================================

CREATE TABLE reservas (
    id_reserva          BIGSERIAL PRIMARY KEY,
    id_mesa             BIGINT NOT NULL REFERENCES mesas(id_mesa),
    fecha_hora_reserva  TIMESTAMP NOT NULL,
    nombre_cliente      VARCHAR(100),
    cantidad_personas   INTEGER,
    is_active           SMALLINT NOT NULL DEFAULT 1,  -- 1: vigente, 0: archivada/borrado lógico
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reservas_active CHECK (is_active IN (0, 1))
);

CREATE INDEX idx_reservas_mesa ON reservas(id_mesa);
CREATE INDEX idx_reservas_fecha ON reservas(fecha_hora_reserva);
