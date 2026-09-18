CREATE TABLE roles (
    id_rol      INTEGER GENERATED ALWAYS AS IDENTITY,
    nombre_rol  VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255),
    CONSTRAINT pk_roles PRIMARY KEY (id_rol),
    CONSTRAINT uk_roles_nombre UNIQUE (nombre_rol)
);
