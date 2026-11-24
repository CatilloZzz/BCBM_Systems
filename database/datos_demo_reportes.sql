-- Carga de salidas y tripulaciones DEMO para probar Reportes de cabina.
-- Todas las identidades son ficticias y llevan el prefijo DEM; no son cédulas reales.
-- Seguro para volver a ejecutar: no borra ni reemplaza filas existentes.
-- Usar únicamente en la base local de demostración bomberos_db.
BEGIN;

DO $guard$
BEGIN
    IF current_database() <> 'bomberos_db' THEN
        RAISE EXCEPTION 'Este script solo se puede ejecutar en bomberos_db.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM usuario WHERE username = 'admin.demo')
       OR NOT EXISTS (SELECT 1 FROM emergencia WHERE numero_acta = 'DEMO-2026-010')
       OR NOT EXISTS (SELECT 1 FROM unidad WHERE codigo = 'CAM-04') THEN
        RAISE EXCEPTION 'Faltan datos de demostración. Ejecuta primero database/datos_demo_adicionales.sql.';
    END IF;
END
$guard$;

-- Bomberos ficticios con identificadores DEMO que conservan el segmento de fecha.
-- DEM evita confundir estas pruebas con cédulas oficiales.
INSERT INTO bombero
    (nombre, apellido, cedula, rango, telefono, fecha_ingreso, estado, compania, escuadra)
VALUES
    ('Alma',     'Herrera',  'DEM-001-850412-0001A', 'Bombero', NULL, CURRENT_DATE - 2400, 'DISPONIBLE', 'Compañía Central', 'A'),
    ('Bruno',    'Pineda',   'DEM-001-900728-0002B', 'Bombero', NULL, CURRENT_DATE - 1800, 'DISPONIBLE', 'Compañía Norte',   'B'),
    ('Gabriela', 'Rivas',    'DEM-281-820219-0003C', 'Bombero', NULL, CURRENT_DATE - 3000, 'DISPONIBLE', 'Compañía Central', 'B'),
    ('Mateo',    'Castillo', 'DEM-001-880511-0004D', 'Bombero', NULL, CURRENT_DATE - 2200, 'DISPONIBLE', 'Compañía Norte',   'A')
ON CONFLICT (cedula) DO NOTHING;

-- Servicios ficticios para que el Excel/PDF tenga también actividad de otros servicios.
INSERT INTO otro_servicio (escuadra_departamento, tipo, detalle)
SELECT d.escuadra, d.tipo, d.detalle
FROM (VALUES
    ('Compañía Central', 'CAPACITACION_PERSONAL', 'DEMO-REPORTES-CAP-01 | Práctica de primeros auxilios'),
    ('Compañía Norte',   'RECORRIDO',              'DEMO-REPORTES-REC-01 | Recorrido preventivo de sectores'),
    ('Compañía Central', 'SUMINISTRO_AGUA',        'DEMO-REPORTES-AGU-01 | Traslado simulado de agua')
) AS d(escuadra, tipo, detalle)
WHERE NOT EXISTS (
    SELECT 1 FROM otro_servicio o WHERE o.detalle = d.detalle
);

-- Cinco salidas a actas DEMO cerradas. Los tiempos se derivan de la apertura del acta.
INSERT INTO salida_unidad
    (emergencia_id, otro_servicio_id, unidad_id, operador_id, jefe_id,
     registrado_por, salida, llegada, retorno)
SELECT e.id, NULL, u.id, operador.id, jefe.id, usr.id,
       e.fecha_hora + d.min_salida * INTERVAL '1 minute',
       e.fecha_hora + d.min_llegada * INTERVAL '1 minute',
       e.fecha_hora + d.min_retorno * INTERVAL '1 minute'
FROM (VALUES
    ('DEMO-2026-004', 'CAM-04', 'DEMO-B001', 'DEMO-B002',  5, 25, 55),
    ('DEMO-2026-005', 'RES-02', 'DEMO-B002', 'DEMO-B003',  8, 42, 110),
    ('DEMO-2026-006', 'CAM-02', 'DEMO-B001', 'DEMO-B004',  6, 20, 75),
    ('DEMO-2026-008', 'CAM-04', 'DEMO-B002', 'DEMO-B005', 12, 32, 160),
    ('DEMO-2026-010', 'AMB-02', 'DEMO-B001', 'DEMO-B006',  4, 18, 55)
) AS d(acta, unidad, operador_cedula, jefe_cedula, min_salida, min_llegada, min_retorno)
JOIN emergencia e ON e.numero_acta = d.acta
JOIN unidad u ON u.codigo = d.unidad
JOIN bombero operador ON operador.cedula = d.operador_cedula
JOIN bombero jefe ON jefe.cedula = d.jefe_cedula
JOIN usuario usr ON usr.username = 'admin.demo'
WHERE NOT EXISTS (
    SELECT 1 FROM salida_unidad s
    WHERE s.emergencia_id = e.id
      AND s.unidad_id = u.id
      AND s.operador_id = operador.id
      AND s.salida = e.fecha_hora + d.min_salida * INTERVAL '1 minute'
);

-- Tres salidas para otros servicios, también con fecha dentro del mes actual.
INSERT INTO salida_unidad
    (emergencia_id, otro_servicio_id, unidad_id, operador_id, jefe_id,
     registrado_por, salida, llegada, retorno)
SELECT NULL, o.id, u.id, operador.id, jefe.id, usr.id, d.salida, NULL, d.retorno
FROM (VALUES
    ('DEMO-REPORTES-CAP-01 | Práctica de primeros auxilios', 'CAM-04', 'DEMO-B001', 'DEMO-B003',
        CURRENT_DATE - 12 + TIME '08:00', CURRENT_DATE - 12 + TIME '10:00'),
    ('DEMO-REPORTES-REC-01 | Recorrido preventivo de sectores', 'RES-02', 'DEMO-B002', 'DEMO-B004',
        CURRENT_DATE -  8 + TIME '18:00', CURRENT_DATE -  8 + TIME '20:00'),
    ('DEMO-REPORTES-AGU-01 | Traslado simulado de agua', 'CAM-02', 'DEMO-B001', 'DEMO-B005',
        CURRENT_DATE -  3 + TIME '09:00', CURRENT_DATE -  3 + TIME '11:30')
) AS d(detalle, unidad, operador_cedula, jefe_cedula, salida, retorno)
JOIN otro_servicio o ON o.detalle = d.detalle
JOIN unidad u ON u.codigo = d.unidad
JOIN bombero operador ON operador.cedula = d.operador_cedula
JOIN bombero jefe ON jefe.cedula = d.jefe_cedula
JOIN usuario usr ON usr.username = 'admin.demo'
WHERE NOT EXISTS (
    SELECT 1 FROM salida_unidad s
    WHERE s.otro_servicio_id = o.id
      AND s.unidad_id = u.id
      AND s.operador_id = operador.id
      AND s.salida = d.salida
);

-- Asociar operadores, jefes y tripulación a las cinco salidas de emergencias.
WITH tripulacion(acta, unidad, operador_cedula, min_salida, participante_cedula) AS (
    VALUES
    ('DEMO-2026-004', 'CAM-04', 'DEMO-B001',  5, 'DEMO-B001'),
    ('DEMO-2026-004', 'CAM-04', 'DEMO-B001',  5, 'DEMO-B002'),
    ('DEMO-2026-004', 'CAM-04', 'DEMO-B001',  5, 'DEM-001-850412-0001A'),
    ('DEMO-2026-004', 'CAM-04', 'DEMO-B001',  5, 'DEM-001-900728-0002B'),
    ('DEMO-2026-005', 'RES-02', 'DEMO-B002',  8, 'DEMO-B002'),
    ('DEMO-2026-005', 'RES-02', 'DEMO-B002',  8, 'DEMO-B003'),
    ('DEMO-2026-005', 'RES-02', 'DEMO-B002',  8, 'DEM-001-900728-0002B'),
    ('DEMO-2026-005', 'RES-02', 'DEMO-B002',  8, 'DEM-281-820219-0003C'),
    ('DEMO-2026-006', 'CAM-02', 'DEMO-B001',  6, 'DEMO-B001'),
    ('DEMO-2026-006', 'CAM-02', 'DEMO-B001',  6, 'DEMO-B004'),
    ('DEMO-2026-006', 'CAM-02', 'DEMO-B001',  6, 'DEM-001-850412-0001A'),
    ('DEMO-2026-006', 'CAM-02', 'DEMO-B001',  6, 'DEM-001-880511-0004D'),
    ('DEMO-2026-008', 'CAM-04', 'DEMO-B002', 12, 'DEMO-B002'),
    ('DEMO-2026-008', 'CAM-04', 'DEMO-B002', 12, 'DEMO-B005'),
    ('DEMO-2026-008', 'CAM-04', 'DEMO-B002', 12, 'DEM-001-900728-0002B'),
    ('DEMO-2026-008', 'CAM-04', 'DEMO-B002', 12, 'DEM-001-880511-0004D'),
    ('DEMO-2026-010', 'AMB-02', 'DEMO-B001',  4, 'DEMO-B001'),
    ('DEMO-2026-010', 'AMB-02', 'DEMO-B001',  4, 'DEMO-B006'),
    ('DEMO-2026-010', 'AMB-02', 'DEMO-B001',  4, 'DEM-001-850412-0001A'),
    ('DEMO-2026-010', 'AMB-02', 'DEMO-B001',  4, 'DEM-281-820219-0003C')
)
INSERT INTO salida_participante(salida_id, bombero_id)
SELECT s.id, b.id
FROM tripulacion t
JOIN emergencia e ON e.numero_acta = t.acta
JOIN unidad u ON u.codigo = t.unidad
JOIN bombero operador ON operador.cedula = t.operador_cedula
JOIN usuario usr ON usr.username = 'admin.demo'
JOIN salida_unidad s ON s.emergencia_id = e.id
    AND s.unidad_id = u.id
    AND s.operador_id = operador.id
    AND s.registrado_por = usr.id
    AND s.salida = e.fecha_hora + t.min_salida * INTERVAL '1 minute'
JOIN bombero b ON b.cedula = t.participante_cedula
ON CONFLICT DO NOTHING;

-- Tripulaciones de los tres otros servicios.
WITH tripulacion(detalle, unidad, operador_cedula, salida, participante_cedula) AS (
    VALUES
    ('DEMO-REPORTES-CAP-01 | Práctica de primeros auxilios', 'CAM-04', 'DEMO-B001',
        CURRENT_DATE - 12 + TIME '08:00', 'DEMO-B001'),
    ('DEMO-REPORTES-CAP-01 | Práctica de primeros auxilios', 'CAM-04', 'DEMO-B001',
        CURRENT_DATE - 12 + TIME '08:00', 'DEM-001-850412-0001A'),
    ('DEMO-REPORTES-CAP-01 | Práctica de primeros auxilios', 'CAM-04', 'DEMO-B001',
        CURRENT_DATE - 12 + TIME '08:00', 'DEM-001-900728-0002B'),
    ('DEMO-REPORTES-REC-01 | Recorrido preventivo de sectores', 'RES-02', 'DEMO-B002',
        CURRENT_DATE - 8 + TIME '18:00', 'DEMO-B002'),
    ('DEMO-REPORTES-REC-01 | Recorrido preventivo de sectores', 'RES-02', 'DEMO-B002',
        CURRENT_DATE - 8 + TIME '18:00', 'DEM-281-820219-0003C'),
    ('DEMO-REPORTES-REC-01 | Recorrido preventivo de sectores', 'RES-02', 'DEMO-B002',
        CURRENT_DATE - 8 + TIME '18:00', 'DEM-001-880511-0004D'),
    ('DEMO-REPORTES-AGU-01 | Traslado simulado de agua', 'CAM-02', 'DEMO-B001',
        CURRENT_DATE - 3 + TIME '09:00', 'DEMO-B001'),
    ('DEMO-REPORTES-AGU-01 | Traslado simulado de agua', 'CAM-02', 'DEMO-B001',
        CURRENT_DATE - 3 + TIME '09:00', 'DEM-001-850412-0001A'),
    ('DEMO-REPORTES-AGU-01 | Traslado simulado de agua', 'CAM-02', 'DEMO-B001',
        CURRENT_DATE - 3 + TIME '09:00', 'DEM-001-880511-0004D')
)
INSERT INTO salida_participante(salida_id, bombero_id)
SELECT s.id, b.id
FROM tripulacion t
JOIN otro_servicio o ON o.detalle = t.detalle
JOIN unidad u ON u.codigo = t.unidad
JOIN bombero operador ON operador.cedula = t.operador_cedula
JOIN usuario usr ON usr.username = 'admin.demo'
JOIN salida_unidad s ON s.otro_servicio_id = o.id
    AND s.unidad_id = u.id
    AND s.operador_id = operador.id
    AND s.registrado_por = usr.id
    AND s.salida = t.salida
JOIN bombero b ON b.cedula = t.participante_cedula
ON CONFLICT DO NOTHING;

COMMIT;

