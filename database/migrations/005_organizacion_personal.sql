BEGIN;
ALTER TABLE bombero ADD COLUMN compania VARCHAR(80), ADD COLUMN escuadra VARCHAR(1) CHECK (escuadra IN ('A','B'));
ALTER TABLE bombero DROP CONSTRAINT bombero_estado_check;
ALTER TABLE bombero ADD CONSTRAINT bombero_estado_check CHECK (estado IN ('DISPONIBLE','EN_EMERGENCIA','EN_SERVICIO','DE_BAJA'));
UPDATE bombero b SET estado = CASE WHEN EXISTS (
    SELECT 1 FROM salida_participante p JOIN salida_unidad s ON s.id=p.salida_id
    WHERE p.bombero_id=b.id AND s.retorno IS NULL AND s.emergencia_id IS NOT NULL
) THEN 'EN_EMERGENCIA' ELSE 'EN_SERVICIO' END
WHERE b.estado='DISPONIBLE' AND EXISTS (
    SELECT 1 FROM salida_participante p JOIN salida_unidad s ON s.id=p.salida_id
    WHERE p.bombero_id=b.id AND s.retorno IS NULL
);
COMMIT;
