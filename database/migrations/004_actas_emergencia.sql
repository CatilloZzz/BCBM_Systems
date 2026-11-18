BEGIN;
ALTER TABLE emergencia
    ADD COLUMN IF NOT EXISTS numero_acta varchar(30),
    ADD COLUMN IF NOT EXISTS escuadra varchar(100),
    ADD COLUMN IF NOT EXISTS solicitante varchar(120),
    ADD COLUMN IF NOT EXISTS telefono varchar(30),
    ADD COLUMN IF NOT EXISTS descripcion_final text,
    ADD COLUMN IF NOT EXISTS resultado varchar(24),
    ALTER COLUMN direccion DROP NOT NULL,
    ALTER COLUMN tipo_id DROP NOT NULL,
    ALTER COLUMN prioridad DROP NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS emergencia_numero_acta_idx ON emergencia (lower(numero_acta));
COMMIT;
