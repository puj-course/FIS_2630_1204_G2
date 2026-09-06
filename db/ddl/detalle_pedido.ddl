CREATE TABLE detalle_pedido (
                                id              BIGSERIAL PRIMARY KEY,
                                pedido_id       BIGINT NOT NULL,
                                plato_id        BIGINT NOT NULL, -- Corresponde a producto_id (BIGINT)
                                cantidad        NUMERIC(12,4) NOT NULL,
                                precio_unitario NUMERIC(12,2) NOT NULL, -- Requerido por HU-021 para congelar tarifa
                                subtotal        NUMERIC(12,2) NOT NULL, -- Subtotal calculado por línea

    -- Restricciones de Clave Foránea (Integridad)
                                CONSTRAINT fk_detalle_pedido_pedido
                                    FOREIGN KEY (pedido_id) REFERENCES pedidos(pedido_id) ON DELETE CASCADE,
                                CONSTRAINT fk_detalle_pedido_plato
                                    FOREIGN KEY (plato_id) REFERENCES productos(producto_id),

    -- Validaciones de importes
                                CONSTRAINT chk_detalle_pedido_cantidad CHECK (cantidad > 0),
                                CONSTRAINT chk_detalle_pedido_precio CHECK (precio_unitario >= 0),
                                CONSTRAINT chk_detalle_pedido_subtotal CHECK (subtotal >= 0)
);

-- Índices de optimización
CREATE INDEX idx_detalle_pedido_pedido ON detalle_pedido(pedido_id);
CREATE INDEX idx_detalle_pedido_plato ON detalle_pedido(plato_id);
