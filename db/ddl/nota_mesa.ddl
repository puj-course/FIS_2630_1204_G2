-- =============================================================================
-- TABLA: nota_mesa
-- HU-69: nota rápida / etiqueta descriptiva sobre una mesa
-- =============================================================================

CREATE TABLE nota_mesa (
    id_nota       BIGSERIAL PRIMARY KEY,
    id_mesa       BIGINT NOT NULL REFERENCES mesas(id_mesa),
    texto_nota    VARCHAR(140) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Solo una nota activa por mesa a la vez (se edita la existente en vez de apilar notas)
CREATE UNIQUE INDEX uq_nota_mesa_por_mesa ON nota_mesa(id_mesa);

CREATE INDEX idx_nota_mesa_mesa ON nota_mesa(id_mesa);

-- =============================================================================
-- Limpieza automática: cuando una mesa cambia a DISPONIBLE, se borran sus notas.
-- Funciona sin importar si el cambio de estado lo hizo la API Spring Boot
-- o la app de escritorio, porque ambas actualizan la misma tabla "mesas".
-- =============================================================================

CREATE OR REPLACE FUNCTION limpiar_nota_al_liberar_mesa()
RETURNS TRIGGER AS $$
DECLARE
    codigo_nuevo VARCHAR(20);
BEGIN
    SELECT codigo_estado INTO codigo_nuevo
    FROM estados_mesa
    WHERE id_estado_mesa = NEW.id_estado_mesa;

    IF codigo_nuevo = 'DISPONIBLE' THEN
        DELETE FROM nota_mesa WHERE id_mesa = NEW.id_mesa;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_limpiar_nota_al_liberar_mesa
    AFTER UPDATE OF id_estado_mesa ON mesas
    FOR EACH ROW
    EXECUTE FUNCTION limpiar_nota_al_liberar_mesa();
