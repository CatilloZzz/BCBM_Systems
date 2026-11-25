-- Completa los catálogos obligatorios del formulario de Otros servicios.
-- Aditivo e idempotente: conserva opciones existentes, incluso las inactivas.

BEGIN;

WITH candidatos(grupo, nombre) AS (
    SELECT 'DEPARTAMENTO', compania FROM bombero WHERE compania IS NOT NULL AND btrim(compania) <> ''
    UNION ALL
    SELECT 'DEPARTAMENTO', escuadra_departamento FROM otro_servicio
      WHERE escuadra_departamento IS NOT NULL AND btrim(escuadra_departamento) <> ''
    UNION ALL
    SELECT 'TIPO_SERVICIO', tipo FROM otro_servicio WHERE tipo IS NOT NULL AND btrim(tipo) <> ''
    UNION ALL
    SELECT 'TIPO_SERVICIO', nombre FROM (VALUES
        ('COBERTURA_EVENTOS'), ('GESTION_ADMINISTRATIVA'), ('TALLER'), ('CAPACITACION'),
        ('GESTION_DPC'), ('CAPACITACION_PERSONAL'), ('REUNION_OPERATIVA'), ('GESTIONES_VARIAS'),
        ('LABOR_PREVENTIVA'), ('INFORME'), ('PROTECCION_SERVICIOS'), ('SERVICIO_ESPECIAL'),
        ('SERVICIO_FUNEBRE'), ('SERVICIO_OPERATIVO'), ('SIMULACRO'), ('SUMINISTRO_COMBUSTIBLE'),
        ('SUMINISTRO_AGUA'), ('TRASLADO_BOMBEROS'), ('TRASLADO_PACIENTES'), ('VIAJE_AGUA'),
        ('RECORRIDO'), ('OTRO')
    ) AS tipos(nombre)
),
unicas AS (
    SELECT grupo, min(btrim(nombre)) AS nombre
    FROM candidatos
    WHERE btrim(nombre) <> ''
    GROUP BY grupo, lower(btrim(nombre))
)
INSERT INTO opcion_catalogo (grupo, nombre)
SELECT u.grupo, u.nombre
FROM unicas u
WHERE NOT EXISTS (
    SELECT 1
    FROM opcion_catalogo o
    WHERE o.grupo = u.grupo
      AND lower(btrim(o.nombre)) = lower(u.nombre)
);

COMMIT;
