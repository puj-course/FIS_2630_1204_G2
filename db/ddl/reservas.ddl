-- =============================================================================
-- TABLA: reservas
-- HU-054: visualizar mesas reservadas con hora, cliente y cantidad de personas
-- =============================================================================

CREATE TABLE reservas (
    id_reserva          BIGSERIAL PRIMARY KEY,
    id_mesa             BIGINT NOT NULL REFERENCES mesas(id_mesa),
    id_cliente          BIGINT,
    fecha_hora_reserva  TIMESTAMP NOT NULL,
    nombre_cliente      VARCHAR(100),
    cantidad_personas   INTEGER,
    is_active           SMALLINT NOT NULL DEFAULT 1,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cancelacion	TIMESTAMP,

    CONSTRAINT fk_reservas_mesa FOREIGN KEY (id_mesa)
        REFERENCES mesas(id_mesa),
    CONSTRAINT fk_reservas_cliente FOREIGN KEY (id_cliente)
        REFERENCES clientes(id_cliente),
    CONSTRAINT chk_reservas_active CHECK (is_active IN (0, 1)),
    CONSTRAINT chk_reservas_personas CHECK (
        cantidad_personas IS NULL OR cantidad_personas > 0)
);

CREATE INDEX idx_reservas_mesa ON reservas(id_mesa);
CREATE INDEX idx_reservas_fecha ON reservas(fecha_hora_reserva);
CREATE INDEX ix_reservas_id_cliente ON reservas(id_cliente);
