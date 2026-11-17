-- Receta normalizada: cuantos ingredientes consume cada plato.
-- Columnas y nombres tomados de repository/ProductoIngredienteRepository.java, donde
-- "plato" es una fila de productos e "insumo" una fila de ingredientes.
--
-- ATENCION: esta tabla se solapa con productos.ingredientes (JSONB), que es donde
-- las funciones de HU-28 leen hoy la receta. Quedan dos fuentes para el mismo dato.
-- Ver "Choques de nombre pendientes" en el README de esta carpeta.

CREATE TABLE producto_ingrediente (
    producto_id         BIGINT NOT NULL,
    ingrediente_id      BIGINT NOT NULL,
    cantidad_necesaria  NUMERIC(10,2) NOT NULL,

    CONSTRAINT pk_producto_ingrediente PRIMARY KEY (producto_id, ingrediente_id),
    CONSTRAINT fk_producto_ingrediente_producto FOREIGN KEY (producto_id)
        REFERENCES productos(producto_id) ON DELETE CASCADE,
    CONSTRAINT fk_producto_ingrediente_ingrediente FOREIGN KEY (ingrediente_id)
        REFERENCES ingredientes(ingrediente_id) ON DELETE CASCADE,
    CONSTRAINT ck_producto_ingrediente_cantidad CHECK (cantidad_necesaria > 0)
);

CREATE INDEX ix_producto_ingrediente_ingrediente
    ON producto_ingrediente (ingrediente_id);
