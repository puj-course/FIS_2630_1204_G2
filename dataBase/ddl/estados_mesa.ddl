CREATE TABLE estados_mesa (
    id_estado_mesa INTEGER GENERATED ALWAYS AS IDENTITY,
    codigo_estado  VARCHAR(20) NOT NULL,
    descripcion    VARCHAR(100),
    CONSTRAINT pk_estados_mesa PRIMARY KEY (id_estado_mesa),
    CONSTRAINT uk_estados_mesa_codigo UNIQUE (codigo_estado)
);
