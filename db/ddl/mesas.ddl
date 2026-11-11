CREATE TABLE mesas (
    id_mesa        INTEGER GENERATED ALWAYS AS IDENTITY,
    numero_mesa    NUMERIC(4) NOT NULL,
    codigo_mesa    VARCHAR(20),
    capacidad      NUMERIC(2) DEFAULT 2 NOT NULL,
    id_zona        INTEGER NOT NULL,
    id_estado_mesa INTEGER NOT NULL,
    cantidad_comensales INTEGER,
    is_active      SMALLINT DEFAULT 1 NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT pk_mesas PRIMARY KEY (id_mesa),
    CONSTRAINT uk_mesas_num_zona UNIQUE (numero_mesa, id_zona),
    CONSTRAINT uk_mesas_codigo UNIQUE (codigo_mesa),
    CONSTRAINT fk_mesas_zona FOREIGN KEY (id_zona)
        REFERENCES zonas(id_zona),
    CONSTRAINT fk_mesas_estado FOREIGN KEY (id_estado_mesa)
        REFERENCES estados_mesa(id_estado_mesa),
    CONSTRAINT chk_mesas_capacidad CHECK (capacidad > 0),
    CONSTRAINT chk_mesas_active CHECK (is_active IN (0, 1)),
    CONSTRAINT ck_mesas_codigo_no_vacio CHECK (btrim(codigo_mesa) <> ''),
    CONSTRAINT ck_mesas_comensales_capacidad CHECK (
        cantidad_comensales IS NULL
            OR (cantidad_comensales > 0 AND cantidad_comensales <= capacidad)
        )
);

CREATE INDEX ix_mesas_zona_estado ON mesas (id_zona, id_estado_mesa);
CREATE INDEX ix_mesas_id_estado_mesa ON mesas (id_estado_mesa);

COMMENT ON COLUMN mesas.codigo_mesa IS
    'Identificador unico que se muestra al mesero en el mapa de salon.';
COMMENT ON COLUMN mesas.cantidad_comensales IS
    'Comensales de la atencion en curso. NULL cuando la mesa no esta ocupada.';

