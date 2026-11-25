-- Datos de demostración creados desde Configuración y Otros servicios.
-- Recrea catálogos, una inspección preventiva y la salida que alimenta
-- el reporte de cabina. Ejecutar solo en la base local bomberos_db que
-- ya tenga schema.sql, datos_prueba.sql y las migraciones correspondientes.
-- La salida queda ABIERTA, igual que el registro original: CAM-02 y sus
-- participantes permanecerán EN_SERVICIO hasta registrar su retorno.

BEGIN;

DO $guard$
BEGIN
    IF current_database() <> 'bomberos_db' THEN
        RAISE EXCEPTION 'Este script solo se puede ejecutar en bomberos_db.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM usuario WHERE username = 'admin')
       OR NOT EXISTS (SELECT 1 FROM unidad WHERE codigo = 'CAM-02')
       OR NOT EXISTS (SELECT 1 FROM bombero WHERE cedula = '001-220490-0002B')
       OR NOT EXISTS (SELECT 1 FROM bombero WHERE cedula = '281-250597-0009J') THEN
        RAISE EXCEPTION 'Faltan los datos base de demostración requeridos.';
    END IF;
END
$guard$;

-- Catálogos creados en Configuración general.
INSERT INTO opcion_catalogo (grupo, nombre) VALUES
    ('DEPARTAMENTO', 'Prevención'),
    ('DEPARTAMENTO', 'Atención prehospitalaria'),
    ('DEPARTAMENTO', 'Materiales peligrosos'),
    ('DEPARTAMENTO', 'Logística y apoyo'),
    ('TIPO_SERVICIO', 'Inspección preventiva'),
    ('TIPO_SERVICIO', 'Capacitación comunitaria'),
    ('TIPO_SERVICIO', 'Cobertura de evento'),
    ('TIPO_SERVICIO', 'Traslado de agua'),
    ('COMPANIA', 'Compañía Sur'),
    ('COMPANIA', 'Compañía Central'),
    ('COMPANIA', 'Compañía Oriental'),
    ('COMPANIA', 'Compañía Occidental')
ON CONFLICT DO NOTHING;

-- Servicio registrado desde el módulo Otros servicios.
INSERT INTO otro_servicio (escuadra_departamento, tipo, detalle)
SELECT 'Prevención', 'Inspección preventiva',
       'Inspeccion del area en busca de posibles riesgos.'
WHERE NOT EXISTS (
    SELECT 1 FROM otro_servicio
    WHERE escuadra_departamento = 'Prevención'
      AND tipo = 'Inspección preventiva'
      AND detalle = 'Inspeccion del area en busca de posibles riesgos.'
);

-- Salida que aparece en Reportes de cabina al consultar noviembre de 2026.
INSERT INTO salida_unidad
    (otro_servicio_id, unidad_id, operador_id, jefe_id, registrado_por, salida)
SELECT servicio.id, unidad.id, operador.id, jefe.id, usuario.id,
       TIMESTAMP '2026-11-25 00:38:22.354639'
FROM otro_servicio servicio
JOIN unidad ON unidad.codigo = 'CAM-02'
JOIN bombero operador ON operador.cedula = '001-220490-0002B'
JOIN bombero jefe ON jefe.cedula = '001-220490-0002B'
JOIN usuario ON usuario.username = 'admin'
WHERE servicio.escuadra_departamento = 'Prevención'
  AND servicio.tipo = 'Inspección preventiva'
  AND servicio.detalle = 'Inspeccion del area en busca de posibles riesgos.'
  AND NOT EXISTS (
      SELECT 1 FROM salida_unidad salida
      WHERE salida.otro_servicio_id = servicio.id
        AND salida.unidad_id = unidad.id
        AND salida.operador_id = operador.id
        AND salida.salida = TIMESTAMP '2026-11-25 00:38:22.354639'
  );

-- Tripulación de la salida anterior.
INSERT INTO salida_participante (salida_id, bombero_id)
SELECT salida.id, bombero.id
FROM salida_unidad salida
JOIN otro_servicio servicio ON servicio.id = salida.otro_servicio_id
JOIN bombero ON bombero.cedula IN ('001-220490-0002B', '281-250597-0009J')
WHERE servicio.escuadra_departamento = 'Prevención'
  AND servicio.tipo = 'Inspección preventiva'
  AND servicio.detalle = 'Inspeccion del area en busca de posibles riesgos.'
  AND salida.salida = TIMESTAMP '2026-11-25 00:38:22.354639'
ON CONFLICT DO NOTHING;

-- Estados equivalentes a una salida de servicio aún pendiente de retorno.
UPDATE unidad
SET estado = 'EN_SERVICIO'
WHERE codigo = 'CAM-02'
  AND EXISTS (
      SELECT 1 FROM salida_unidad salida
      JOIN otro_servicio servicio ON servicio.id = salida.otro_servicio_id
      WHERE salida.unidad_id = unidad.id
        AND salida.retorno IS NULL
        AND servicio.detalle = 'Inspeccion del area en busca de posibles riesgos.'
  );

UPDATE bombero
SET estado = 'EN_SERVICIO'
WHERE cedula IN ('001-220490-0002B', '281-250597-0009J')
  AND EXISTS (
      SELECT 1 FROM salida_participante participante
      JOIN salida_unidad salida ON salida.id = participante.salida_id
      JOIN otro_servicio servicio ON servicio.id = salida.otro_servicio_id
      WHERE participante.bombero_id = bombero.id
        AND salida.retorno IS NULL
        AND servicio.detalle = 'Inspeccion del area en busca de posibles riesgos.'
  );

COMMIT;
