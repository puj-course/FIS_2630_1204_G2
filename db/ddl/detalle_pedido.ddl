CREATE TABLE detalle_pedido (
id              BIGSERIAL PRIMARY KEY,
pedido_id       BIGINT NOT NULL,
plato_id        BIGINT NOT NULL, 
cantidad        NUMERIC(12,4) NOT NULL,
precio_unitario NUMERIC(12,2) NOT NULL,
subtotal        NUMERIC(12,2) NOT NULL,

--Restricciones de Clave Foránea (Integridad)
CONSTRAINT fk_detalle_pedido_pedido
FOREIGN KEY (pedido_id) REFERENCES pedidos(pedido_id) ON DELETE CASCADE,
CONSTRAINT fk_detalle_pedido_plato
FOREIGN KEY (plato_id) REFERENCES productos(producto_id),

CONSTRAINT chk_detalle_pedido_cantidad CHECK (cantidad > 0),
CONSTRAINT chk_detalle_pedido_precio CHECK (precio_unitario >= 0),
CONSTRAINT chk_detalle_pedido_subtotal CHECK (subtotal >= 0)
);


