CREATE TABLE tipos_documento (
    id_tipo_documento INTEGER GENERATED ALWAYS AS IDENTITY,
    codigo            VARCHAR(10) NOT NULL,
    nombre            VARCHAR(50) NOT NULL,
    CONSTRAINT pk_tipos_documento PRIMARY KEY (id_tipo_documento),
    CONSTRAINT uk_tipos_doc_codigo UNIQUE (codigo)
);


