BEGIN;
CREATE TABLE dotacion_equipo (
 id SERIAL PRIMARY KEY,
 equipo_id INTEGER NOT NULL REFERENCES equipo(id) ON DELETE CASCADE,
 unidad_id INTEGER NOT NULL REFERENCES unidad(id),
 cantidad INTEGER NOT NULL CHECK (cantidad > 0),
 CONSTRAINT uq_dotacion_equipo_unidad UNIQUE (equipo_id,unidad_id)
);
-- Conserva la dotación existente como asignación inicial de la cantidad completa.
INSERT INTO dotacion_equipo(equipo_id,unidad_id,cantidad)
SELECT id,unidad_id,cantidad FROM equipo WHERE unidad_id IS NOT NULL AND cantidad>0;
-- La cantidad de equipo ahora representa existencias totales del inventario.
UPDATE equipo SET unidad_id=NULL WHERE unidad_id IS NOT NULL;
CREATE INDEX idx_dotacion_equipo_unidad ON dotacion_equipo(unidad_id);
COMMIT;
