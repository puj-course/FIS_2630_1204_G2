CREATE TABLE mesas (
    id_mesa        INTEGER GENERATED ALWAYS AS IDENTITY,
    numero_mesa    NUMERIC(4) NOT NULL,
    codigo_mesa    VARCHAR(20),
    capacidad      NUMERIC(2) DEFAULT 2 NOT NULL,
    id_zona        INTEGER NOT NULL,
    id_estado_mesa INTEGER NOT NULL,
    codigo_qr      VARCHAR(255),
    is_active      SMALLINT DEFAULT 1 NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by     VARCHAR(50),
    CONSTRAINT pk_mesas PRIMARY KEY (id_mesa),
    CONSTRAINT uk_mesas_num_zona UNIQUE (numero_mesa, id_zona),
    CONSTRAINT uk_mesas_codigo UNIQUE (codigo_mesa),
    CONSTRAINT fk_mesas_zona FOREIGN KEY (id_zona)
        REFERENCES zonas(id_zona),
    CONSTRAINT fk_mesas_estado FOREIGN KEY (id_estado_mesa)
        REFERENCES estados_mesa(id_estado_mesa),
    CONSTRAINT chk_mesas_capacidad CHECK (capacidad > 0),
    CONSTRAINT chk_mesas_active CHECK (is_active IN (0, 1))
);

-- Búsqueda rápida de mesas por zona y por su estado actual (ej. ver mesas 'LIBRES' en 'Terraza')
CREATE INDEX ix_mesas_zona_estado ON mesas (id_zona, id_estado_mesa);
