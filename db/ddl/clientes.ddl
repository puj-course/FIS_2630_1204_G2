CREATE TABLE clientes (
    id_cliente        INTEGER GENERATED ALWAYS AS IDENTITY,
    id_tipo_documento INTEGER NOT NULL,
    numero_documento  VARCHAR(20) NOT NULL,
    nombre            VARCHAR(100) NOT NULL,
    apellido          VARCHAR(100) NOT NULL,
    correo            VARCHAR(150) NOT NULL,
    telefono          VARCHAR(20),
    id_usuario_cuenta INTEGER,
    is_active         SMALLINT DEFAULT 1 NOT NULL,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by        VARCHAR(50),
    CONSTRAINT pk_clientes PRIMARY KEY (id_cliente),
    CONSTRAINT uk_clientes_num_doc UNIQUE (numero_documento),
    CONSTRAINT uk_clientes_correo UNIQUE (correo),
    CONSTRAINT fk_clientes_tipo_doc FOREIGN KEY (id_tipo_documento)
        REFERENCES tipos_documento(id_tipo_documento),
    CONSTRAINT fk_clientes_usuario_cuenta FOREIGN KEY (id_usuario_cuenta)
        REFERENCES usuarios(id_usuario),
    CONSTRAINT chk_clientes_active CHECK (is_active IN (0, 1))
);
