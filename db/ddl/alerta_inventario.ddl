CREATE TABLE alerta_inventario (
id                 BIGSERIAL PRIMARY KEY,
ingrediente_id     BIGINT,
producto_id        BIGINT,
stock_actual       NUMERIC(12,4) NOT NULL,
stock_minimo       NUMERIC(12,4) NOT NULL,
cantidad_sugerida  NUMERIC(12,4) NOT NULL,
estado             VARCHAR(20) DEFAULT 'PENDIENTE' NOT NULL,
created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
resolved_at        TIMESTAMP,

CONSTRAINT fk_alerta_ingrediente
FOREIGN KEY (ingrediente_id) REFERENCES ingredientes(ingrediente_id),

CONSTRAINT fk_alerta_producto
        FOREIGN KEY (producto_id) REFERENCES productos(producto_id),

CONSTRAINT chk_alerta_item_unico CHECK (
        (ingrediente_id IS NOT NULL AND producto_id IS NULL) OR
        (ingrediente_id IS NULL AND producto_id IS NOT NULL)),

-- Búsqueda de alertas pendientes/sin resolver ordenadas por fecha para el dashboard
CREATE INDEX ix_alerta_inventario_estado ON alerta_inventario (estado, created_at);
