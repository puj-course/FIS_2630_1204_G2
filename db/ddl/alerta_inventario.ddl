CREATE TABLE alerta_inventario (
id                 BIGSERIAL PRIMARY KEY,
ingrediente_id     BIGINT NOT NULL,
stock_actual       NUMERIC(12,4) NOT NULL,
stock_minimo       NUMERIC(12,4) NOT NULL,
cantidad_sugerida  NUMERIC(12,4) NOT NULL,
estado             VARCHAR(20) DEFAULT 'PENDIENTE' NOT NULL,
created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
resolved_at        TIMESTAMP,

CONSTRAINT fk_alerta_ingrediente
FOREIGN KEY (ingrediente_id) REFERENCES ingredientes(ingrediente_id),

CONSTRAINT chk_alerta_estado
CHECK (estado IN ('PENDIENTE', 'RESUELTA', 'IGNORADA'))
);
