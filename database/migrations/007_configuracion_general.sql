BEGIN;
CREATE TABLE opcion_catalogo (
 id SERIAL PRIMARY KEY, version BIGINT NOT NULL DEFAULT 0, grupo VARCHAR(30) NOT NULL,
 nombre VARCHAR(100) NOT NULL CHECK (length(trim(nombre))>0), activo BOOLEAN NOT NULL DEFAULT TRUE,
 CHECK (grupo IN ('RANGO','COMPANIA','CATEGORIA_EQUIPO','TIPO_UNIDAD','DEPARTAMENTO','TIPO_SERVICIO'))
);
CREATE UNIQUE INDEX uq_catalogo_nombre ON opcion_catalogo(grupo,lower(nombre));
ALTER TABLE bombero ADD COLUMN operador_todas BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE operador_unidad (
 bombero_id INT NOT NULL REFERENCES bombero(id), unidad_id INT NOT NULL REFERENCES unidad(id),
 PRIMARY KEY(bombero_id,unidad_id)
);
ALTER TABLE otro_servicio DROP CONSTRAINT otro_servicio_tipo_check;
ALTER TABLE otro_servicio ALTER COLUMN tipo TYPE VARCHAR(100);
INSERT INTO opcion_catalogo(grupo,nombre)
SELECT grupo,min(trim(nombre)) FROM (
 SELECT 'RANGO' AS grupo,rango AS nombre FROM bombero
 UNION ALL SELECT 'COMPANIA',compania FROM bombero
 UNION ALL SELECT 'CATEGORIA_EQUIPO',categoria FROM equipo
 UNION ALL SELECT 'TIPO_UNIDAD',tipo FROM unidad
 UNION ALL SELECT 'DEPARTAMENTO',escuadra_departamento FROM otro_servicio
 UNION ALL SELECT 'TIPO_SERVICIO',tipo FROM otro_servicio
) existentes WHERE nombre IS NOT NULL AND trim(nombre)<>'' GROUP BY grupo,lower(trim(nombre));
-- Conservar únicamente las autorizaciones demostradas por salidas históricas.
INSERT INTO operador_unidad(bombero_id,unidad_id) SELECT DISTINCT operador_id,unidad_id FROM salida_unidad;
COMMIT;
