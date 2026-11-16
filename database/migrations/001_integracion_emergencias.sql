-- Ejecutar en bomberos_db existente antes de iniciar la versión integrada.
-- Conserva los datos; agrega control de edición simultánea de Julián.
BEGIN;
ALTER TABLE tipo_emergencia ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE emergencia ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
COMMIT;
