CREATE TABLE usuarios (
    id_usuario         INTEGER GENERATED ALWAYS AS IDENTITY,
    codigo_empleado    VARCHAR(20) NOT NULL,
    id_tipo_documento  INTEGER NOT NULL,
    numero_documento   VARCHAR(20) NOT NULL,
    nombre             VARCHAR(100) NOT NULL,
    apellido           VARCHAR(100) NOT NULL,
    correo             VARCHAR(150) NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    pin_acceso_hash    VARCHAR(255),
    id_rol             INTEGER NOT NULL,
    is_on_shift        SMALLINT DEFAULT 0 NOT NULL,
    is_active          SMALLINT DEFAULT 1 NOT NULL,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by         VARCHAR(50),
    CONSTRAINT pk_usuarios PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuarios_codigo_emp UNIQUE (codigo_empleado),
    CONSTRAINT uk_usuarios_num_doc UNIQUE (numero_documento),
    CONSTRAINT uk_usuarios_correo UNIQUE (correo),
    CONSTRAINT fk_usuarios_tipo_doc FOREIGN KEY (id_tipo_documento)
        REFERENCES tipos_documento(id_tipo_documento),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (id_rol)
        REFERENCES roles(id_rol),
    CONSTRAINT chk_usuarios_shift CHECK (is_on_shift IN (0, 1)),
    CONSTRAINT chk_usuarios_active CHECK (is_active IN (0, 1))
);

-- Acelera la búsqueda por tipo y número de documento (ej. inicio de sesión / verificación)
CREATE INDEX ix_usuarios_documento ON usuarios (tipo_documento_id, numero_documento);

-- Acelera la carga de usuarios según su rol (ej. listar todos los meseros)
CREATE INDEX ix_usuarios_rol ON usuarios (rol_id);


