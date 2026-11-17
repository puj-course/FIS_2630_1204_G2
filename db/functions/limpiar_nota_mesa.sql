-- =============================================================================
-- Trigger: Limpieza automática de notas al liberar una mesa
-- =============================================================================

CREATE OR REPLACE FUNCTION limpiar_nota_al_liberar_mesa()
RETURNS TRIGGER AS $$
DECLARE
v_codigo_nuevo VARCHAR(20);
BEGIN
SELECT codigo_estado INTO v_codigo_nuevo
FROM estados_mesa
WHERE id_estado_mesa = NEW.id_estado_mesa;

IF v_codigo_nuevo = 'DISPONIBLE' THEN
DELETE FROM nota_mesa WHERE id_mesa = NEW.id_mesa;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_limpiar_nota_al_liberar_mesa ON mesas;
CREATE TRIGGER trg_limpiar_nota_al_liberar_mesa
    AFTER UPDATE OF id_estado_mesa ON mesas
    FOR EACH ROW
    EXECUTE FUNCTION limpiar_nota_al_liberar_mesa();