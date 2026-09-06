CREATE TABLE alerta_inventario (
                                   id                 BIGSERIAL PRIMARY KEY,
                                   ingrediente_id     BIGINT NOT NULL,
                                   stock_actual       NUMERIC(12,4) NOT NULL,
                                   stock_minimo       NUMERIC(12,4) NOT NULL,
                                   cantidad_sugerida  NUMERIC(12,4) NOT NULL,
                                   estado             VARCHAR(20) DEFAULT 'PENDIENTE' NOT NULL, -- Manejo de estados (HU-023)
                                   created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                   resolved_at        TIMESTAMP,

    -- Clave foránea explícita
                                   CONSTRAINT fk_alerta_ingrediente
                                       FOREIGN KEY (ingrediente_id) REFERENCES ingredientes(ingrediente_id),

    -- Validación de estado
                                   CONSTRAINT chk_alerta_estado
                                       CHECK (estado IN ('PENDIENTE', 'RESUELTA', 'IGNORADA'))
);

-- Índice para consultar alertas pendientes rápidamente
CREATE INDEX idx_alerta_inventario_estado ON alerta_inventario(estado);