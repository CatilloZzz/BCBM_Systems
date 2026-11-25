-- Solo para una base local/de demostración que ya tenga ejecutados schema.sql y datos_prueba.sql.
-- Los nuevos registros son ficticios; los identificadores DEMO no pertenecen a personas reales.
-- Script aditivo e idempotente: no borra ni modifica los datos existentes.
BEGIN;

DO $seed$
BEGIN
    IF to_regclass('public.bombero') IS NULL OR to_regclass('public.usuario') IS NULL
       OR NOT EXISTS (SELECT 1 FROM usuario WHERE username = 'admin')
       OR NOT EXISTS (SELECT 1 FROM tipo_emergencia) THEN
        RAISE EXCEPTION 'Ejecute primero database/schema.sql y database/datos_prueba.sql en una base de demostración.';
    END IF;
END $seed$;

-- Personal ficticio; no se incluyen teléfonos ni cédulas de personas reales.
INSERT INTO bombero (nombre, apellido, cedula, rango, fecha_ingreso, estado, compania, escuadra) VALUES
('Laura',    'Morales',  'DEMO-B001', 'Capitán',  CURRENT_DATE - 2600, 'DISPONIBLE', 'Compañía Central', 'A'),
('Rafael',   'Ruiz',     'DEMO-B002', 'Teniente', CURRENT_DATE - 2100, 'DISPONIBLE', 'Compañía Central', 'B'),
('Valeria',  'Castillo', 'DEMO-B003', 'Bombero',  CURRENT_DATE - 1300, 'DISPONIBLE', 'Compañía Central', 'A'),
('Andrés',   'Medina',   'DEMO-B004', 'Bombero',  CURRENT_DATE - 1000, 'DISPONIBLE', 'Compañía Central', 'B'),
('Mauricio', 'López',    'DEMO-B005', 'Sargento', CURRENT_DATE - 1700, 'DISPONIBLE', 'Compañía Norte',   'A'),
('Teresa',   'Navarro',  'DEMO-B006', 'Bombero',  CURRENT_DATE - 800,  'DISPONIBLE', 'Compañía Norte',   'B')
ON CONFLICT (cedula) DO NOTHING;

-- Cuentas exclusivas para demostración. Contraseñas en README_BASE_DATOS.md; aquí solo se guardan hashes BCrypt.
WITH cuentas(username, password_hash, rol, cedula) AS (
    VALUES
    ('admin.demo',   '$2a$12$fJvX5r/GkuAglHYy6K/cpu5mUEz4pTBWw0Ch.GXT0mDuLyLR2nvtK', 'ADMIN',       NULL::varchar),
    ('despacho.a',   '$2a$12$oYr8ydCNYU5IX73QZYXwuOtwLJqdH4tWWq2g3qrtnT4c5SboeVtG.', 'DESPACHADOR', 'DEMO-B001'),
    ('despacho.b',   '$2a$12$4su9uQiJFYz9pVh2pqcjsOeduNcNdGQ48BD82uhyc9DSSuS8hInSW', 'DESPACHADOR', 'DEMO-B002'),
    ('bombero2',     '$2a$12$agbPpNBrU2GKNLZPLIjIfuphk6GVMm9jZwxet06YRow8dSCHjrRl2', 'BOMBERO',     'DEMO-B003'),
    ('bombero3',     '$2a$12$dqH8uGJx8cTluwA35WpzLuxQ1brmdUszlDbWs5YCvCsznrNeARJ3e', 'BOMBERO',     'DEMO-B004')
)
INSERT INTO usuario (username, password_hash, rol, bombero_id)
SELECT c.username, c.password_hash, c.rol, b.id
FROM cuentas c LEFT JOIN bombero b ON b.cedula = c.cedula
ON CONFLICT DO NOTHING;

-- Los operadores de despacho de la semilla pueden conducir cualquier unidad de demostración.
UPDATE bombero SET operador_todas = TRUE WHERE cedula IN ('DEMO-B001', 'DEMO-B002');

INSERT INTO unidad (codigo, tipo, estado) VALUES
('CAM-04', 'Camión cisterna',  'DISPONIBLE'),
('AMB-02', 'Ambulancia',       'DISPONIBLE'),
('RES-02', 'Unidad de rescate','DISPONIBLE'),
('APO-01', 'Unidad de apoyo',  'MANTENIMIENTO')
ON CONFLICT (codigo) DO NOTHING;

-- Actas ficticias distribuidas durante la semana para poblar el resumen y sus gráficas.
INSERT INTO emergencia (numero_acta, tipo_id, direccion, descripcion, prioridad, estado,
                        fecha_hora, fecha_cierre, registrado_por, escuadra, solicitante,
                        descripcion_final, resultado)
SELECT d.numero_acta, t.id, d.direccion, d.descripcion, d.prioridad, d.estado,
       d.fecha_hora, d.fecha_cierre, u.id, d.escuadra, 'Contacto de demostración',
       d.descripcion_final, d.resultado
FROM (VALUES
    ('DEMO-2026-004', 'Incendio estructural',  'Sector de demostración 1, Managua', 'Simulación: conato de incendio en vivienda.',       'ALTA',     'CERRADA',   NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days' + INTERVAL '1 hour', 'Compañía Central', 'Control simulado y área asegurada.', 'ATENDIDA'),
    ('DEMO-2026-005', 'Rescate',                'Sector de demostración 2, Managua', 'Simulación: rescate preventivo en altura.',       'MEDIA',    'CERRADA',   NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days' + INTERVAL '2 hours', 'Compañía Norte',   'Persona localizada y entregada a familiares.', 'ATENDIDA'),
    ('DEMO-2026-006', 'Accidente de tránsito',  'Sector de demostración 3, Managua', 'Simulación: colisión vehicular sin atrapados.',  'ALTA',     'CERRADA',   NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days' + INTERVAL '90 minutes', 'Compañía Central', 'Escena segura y atención preventiva.', 'ATENDIDA'),
    ('DEMO-2026-007', 'Emergencia médica',       'Sector de demostración 4, Managua', 'Simulación: atención prehospitalaria.',          'MEDIA',    'REPORTADA', NOW() - INTERVAL '3 days', NULL::timestamp,                                      'Compañía Norte',   NULL, NULL),
    ('DEMO-2026-008', 'Incendio forestal',      'Sector de demostración 5, Managua', 'Simulación: quema de maleza controlada.',        'CRITICA',  'CERRADA',   NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '3 hours', 'Compañía Central', 'Foco extinguido y perímetro revisado.', 'ATENDIDA'),
    ('DEMO-2026-009', 'Incendio estructural',   'Sector de demostración 6, Managua', 'Simulación: reporte de humo sin fuego visible.', 'BAJA',     'REPORTADA', NOW() - INTERVAL '1 day',  NULL::timestamp,                                      'Compañía Norte',   NULL, NULL),
    ('DEMO-2026-010', 'Rescate',                'Sector de demostración 7, Managua', 'Simulación: búsqueda preventiva en cauce.',      'MEDIA',    'CERRADA',   NOW() - INTERVAL '8 hours',NOW() - INTERVAL '7 hours',                    'Compañía Central', 'Búsqueda finalizada sin personas afectadas.', 'ATENDIDA')
) AS d(numero_acta, tipo, direccion, descripcion, prioridad, estado, fecha_hora, fecha_cierre,
       escuadra, descripcion_final, resultado)
JOIN tipo_emergencia t ON t.nombre = d.tipo
JOIN usuario u ON u.username = 'despachador'
ON CONFLICT DO NOTHING;

-- Asignaciones históricas asociadas únicamente a las actas de ejemplo ya cerradas.
INSERT INTO asignacion (emergencia_id, unidad_id, bombero_id, fecha_asignacion)
SELECT e.id, un.id, b.id, e.fecha_hora
FROM (VALUES
    ('DEMO-2026-004', 'CAM-04', 'DEMO-B001'),
    ('DEMO-2026-005', 'RES-02', 'DEMO-B003'),
    ('DEMO-2026-006', 'CAM-02', 'DEMO-B002'),
    ('DEMO-2026-008', 'CAM-04', 'DEMO-B004'),
    ('DEMO-2026-010', 'AMB-02', 'DEMO-B001')
) AS d(numero_acta, codigo_unidad, cedula)
JOIN emergencia e ON e.numero_acta = d.numero_acta
JOIN unidad un ON un.codigo = d.codigo_unidad
JOIN bombero b ON b.cedula = d.cedula
ON CONFLICT (emergencia_id, bombero_id) DO NOTHING;

-- Grupos adicionales de equipo. Las cantidades son piezas físicas; las dotaciones son asignaciones parciales.
INSERT INTO equipo (nombre, categoria, cantidad, estado, ultimo_mantenimiento, unidad_id)
SELECT d.nombre, d.categoria, d.cantidad, d.estado, CURRENT_DATE - d.dias_mantenimiento, NULL
FROM (VALUES
    ('Radio portátil de comunicaciones', 'Comunicación', 12, 'OPERATIVO',     18),
    ('Linterna recargable',              'Iluminación',  14, 'OPERATIVO',     25),
    ('Cámara térmica',                   'Rescate',       2, 'OPERATIVO',     35),
    ('Kit de extracción vehicular',      'Rescate',       3, 'EN_REPARACION', 120),
    ('Casco forestal',                   'Protección',    8, 'OPERATIVO',     22),
    ('Máscara de repuesto',              'Protección',    6, 'OPERATIVO',     40),
    ('Generador portátil',               'Energía',       2, 'OPERATIVO',     45),
    ('Botiquín vehicular',               'Médico',        5, 'OPERATIVO',     15)
) AS d(nombre, categoria, cantidad, estado, dias_mantenimiento)
WHERE NOT EXISTS (SELECT 1 FROM equipo e WHERE e.nombre = d.nombre);

INSERT INTO dotacion_equipo (equipo_id, unidad_id, cantidad)
SELECT e.id, u.id, d.cantidad
FROM (VALUES
    ('Radio portátil de comunicaciones', 'CAM-04', 3),
    ('Radio portátil de comunicaciones', 'AMB-02', 2),
    ('Radio portátil de comunicaciones', 'RES-02', 2),
    ('Linterna recargable',              'CAM-04', 2),
    ('Cámara térmica',                   'RES-02', 1),
    ('Botiquín vehicular',               'AMB-02', 2),
    ('Generador portátil',               'CAM-04', 1)
) AS d(nombre_equipo, codigo_unidad, cantidad)
JOIN equipo e ON e.nombre = d.nombre_equipo
JOIN unidad u ON u.codigo = d.codigo_unidad
ON CONFLICT (equipo_id, unidad_id) DO NOTHING;

-- Próximas revisiones ficticias para que el panel muestre alertas útiles.
INSERT INTO mantenimiento_equipo (equipo_id, fecha, descripcion, proxima_fecha, registrado_por)
SELECT e.id, CURRENT_DATE - d.dias_desde_revision, d.descripcion,
       CURRENT_DATE + d.dias_hasta_revision, u.id
FROM (VALUES
    ('Radio portátil de comunicaciones', 'DEMO: revisión preventiva de radios de comunicación.', 20, 7),
    ('Linterna recargable',              'DEMO: prueba de carga de linternas recargables.',     30, 12),
    ('Cámara térmica',                   'DEMO: calibración de cámara térmica.',               25, 18),
    ('Generador portátil',               'DEMO: inspección de generador portátil.',            40, 24),
    ('Botiquín vehicular',               'DEMO: reposición y control de botiquines.',           15, 5)
) AS d(nombre_equipo, descripcion, dias_desde_revision, dias_hasta_revision)
JOIN equipo e ON e.nombre = d.nombre_equipo
JOIN usuario u ON u.username = 'admin.demo'
WHERE NOT EXISTS (
    SELECT 1 FROM mantenimiento_equipo m
    WHERE m.equipo_id = e.id AND m.descripcion = d.descripcion
);

COMMIT;
