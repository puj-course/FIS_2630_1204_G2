CREATE TABLE zonas (
    id_zona     INTEGER GENERATED ALWAYS AS IDENTITY,
    nombre_zona VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255),
    is_active   SMALLINT DEFAULT 1 NOT NULL,
    CONSTRAINT pk_zonas PRIMARY KEY (id_zona),
    CONSTRAINT uk_zonas_nombre UNIQUE (nombre_zona),
    CONSTRAINT chk_zonas_active CHECK (is_active IN (0, 1))
);


