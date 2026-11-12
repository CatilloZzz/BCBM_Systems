-- Datos ficticios de demostración. Ejecutar DESPUÉS de schema.sql.
-- Rechaza la carga si ya hay datos: no elimina ni reinicia registros.
-- Credenciales públicas de prueba; se generaron hashes compatibles con jBCrypt.
BEGIN;
DO $seed$
BEGIN
    IF EXISTS (SELECT 1 FROM bombero) OR
       EXISTS (SELECT 1 FROM usuario) OR
       EXISTS (SELECT 1 FROM tipo_emergencia) OR
       EXISTS (SELECT 1 FROM unidad) OR
       EXISTS (SELECT 1 FROM emergencia) OR
       EXISTS (SELECT 1 FROM asignacion) OR
       EXISTS (SELECT 1 FROM equipo) OR
       EXISTS (SELECT 1 FROM mantenimiento_equipo) THEN
        RAISE EXCEPTION 'La base ya contiene registros. No se cargaron ni borraron datos.';
    END IF;
END $seed$;

-- ---------- Bomberos (ids 1 a 10) ----------
INSERT INTO bombero (nombre, apellido, cedula, rango, telefono, fecha_ingreso, estado) VALUES
('Carlos',  'Mendoza',    '001-150385-0001A', 'Comandante', '8801-1001', '2008-03-10', 'DISPONIBLE'),
('María',   'Gutiérrez',  '001-220490-0002B', 'Capitán',    '8801-1002', '2012-06-01', 'DISPONIBLE'),
('Luis',    'Ramírez',    '281-110892-0003C', 'Teniente',   '8801-1003', '2015-01-20', 'DISPONIBLE'),
('Ana',     'López',      '001-050791-0004D', 'Sargento',   '8801-1004', '2016-09-05', 'EN_EMERGENCIA'),
('José',    'Martínez',   '001-300993-0005E', 'Bombero',    '8801-1005', '2019-02-14', 'EN_EMERGENCIA'),
('Sofía',   'Hernández',  '281-171095-0006F', 'Bombero',    '8801-1006', '2020-07-01', 'DISPONIBLE'),
('Pedro',   'Vargas',     '001-080294-0007G', 'Bombero',    '8801-1007', '2021-03-15', 'DISPONIBLE'),
('Daniela', 'Ortiz',      '001-120896-0008H', 'Bombero',    '8801-1008', '2022-05-10', 'DISPONIBLE'),
('Miguel',  'Torres',     '281-250597-0009J', 'Bombero',    '8801-1009', '2023-01-09', 'DISPONIBLE'),
('Elena',   'Sánchez',    '001-091190-0010K', 'Bombero',    '8801-1010', '2018-11-22', 'DE_BAJA');

-- ---------- Usuarios (ids 1 a 3) ----------
-- Contraseñas de prueba:  admin / admin123
--                         despachador / despacho123
--                         bombero1 / bombero123
INSERT INTO usuario (username, password_hash, rol, bombero_id) VALUES
('admin',       '$2a$12$Us5WTJXNYTq0.gyLWY8kMeS4dsYQP6etPSpllPlZJFSTBpQhlc1qC', 'ADMIN',       NULL),
('despachador', '$2a$12$T86oZWohh.kKYgab1Bnz1eR9T5GswVgXaqwXZNntlUM2rnPe2M2Sm', 'DESPACHADOR', 2),
('bombero1',    '$2a$12$K9phCRk9zADWP.6u0WwYpOHEz20nh1rWWpArGYB4B1XHqsOm/29zC', 'BOMBERO',     3);

-- ---------- Tipos de emergencia (ids 1 a 5) ----------
INSERT INTO tipo_emergencia (nombre) VALUES
('Incendio estructural'),
('Incendio forestal'),
('Rescate'),
('Accidente de tránsito'),
('Emergencia médica');

-- ---------- Unidades (ids 1 a 5) ----------
INSERT INTO unidad (codigo, tipo, estado) VALUES
('CAM-01', 'Camión cisterna',  'EN_EMERGENCIA'),
('CAM-02', 'Camión de escalera', 'DISPONIBLE'),
('AMB-01', 'Ambulancia',       'DISPONIBLE'),
('RES-01', 'Unidad de rescate', 'DISPONIBLE'),
('CAM-03', 'Camión forestal',  'MANTENIMIENTO');

-- ---------- Emergencias (ids 1 a 3) ----------
INSERT INTO emergencia (tipo_id, direccion, descripcion, prioridad, estado, fecha_hora, fecha_cierre, registrado_por) VALUES
(1, 'Calle Principal, Barrio El Progreso, casa 15',
    'Incendio en vivienda de dos plantas, sin víctimas',
    'ALTA', 'CERRADA',
    NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days' + INTERVAL '2 hours', 2),
(4, 'Carretera Norte, km 12',
    'Colisión entre dos vehículos con personas atrapadas',
    'ALTA', 'EN_CURSO',
    NOW() - INTERVAL '30 minutes', NULL, 2),
(5, 'Residencial Los Álamos, casa 8',
    'Persona con dificultad respiratoria',
    'MEDIA', 'REPORTADA',
    NOW() - INTERVAL '5 minutes', NULL, 2);

-- ---------- Asignaciones ----------
-- Emergencia 1 (cerrada): CAM-02 con los bomberos 2 y 3
-- Emergencia 2 (en curso): CAM-01 con los bomberos 4 y 5
INSERT INTO asignacion (emergencia_id, unidad_id, bombero_id) VALUES
(1, 2, 2),
(1, 2, 3),
(2, 1, 4),
(2, 1, 5);

-- ---------- Equipo (15 registros; cada cantidad representa un grupo) ----------
INSERT INTO equipo (nombre, categoria, cantidad, estado, ultimo_mantenimiento, unidad_id) VALUES
('Manguera de 2.5 pulgadas',          'Extinción',    12, 'OPERATIVO',     CURRENT_DATE - 40,  1),
('Motobomba portátil',                'Extinción',     2, 'OPERATIVO',     CURRENT_DATE - 100, 1),
('Extintor PQS 20 lb',                'Extinción',    10, 'OPERATIVO',     CURRENT_DATE - 60,  NULL),
('Extintor CO2',                      'Extinción',     6, 'OPERATIVO',     CURRENT_DATE - 75,  NULL),
('Equipo de respiración autónoma',    'Protección',    8, 'OPERATIVO',     CURRENT_DATE - 20,  NULL),
('Casco de bombero',                  'Protección',   15, 'OPERATIVO',     CURRENT_DATE - 200, NULL),
('Chaqueta de intervención',          'Protección',   15, 'OPERATIVO',     CURRENT_DATE - 90,  NULL),
('Botas de bombero',                  'Protección',   15, 'OPERATIVO',     CURRENT_DATE - 90,  NULL),
('Guantes de intervención',           'Protección',   20, 'OPERATIVO',     CURRENT_DATE - 90,  NULL),
('Hacha de bombero',                  'Herramientas',  6, 'OPERATIVO',     CURRENT_DATE - 45,  2),
('Escalera extensible',               'Herramientas',  3, 'EN_REPARACION', CURRENT_DATE - 250, 2),
('Herramienta hidráulica de rescate', 'Rescate',       2, 'OPERATIVO',     CURRENT_DATE - 30,  4),
('Cuerda de rescate',                 'Rescate',       5, 'OPERATIVO',     CURRENT_DATE - 50,  4),
('Camilla rígida',                    'Médico',        4, 'OPERATIVO',     CURRENT_DATE - 35,  3),
('Botiquín de primeros auxilios',     'Médico',        6, 'OPERATIVO',     CURRENT_DATE - 15,  3);

-- Fechas de asignación coherentes con las emergencias del ejemplo.
UPDATE asignacion a SET fecha_asignacion = e.fecha_hora
FROM emergencia e WHERE e.id = a.emergencia_id;

-- Historial ficticio compatible con ultimo_mantenimiento de los equipos anteriores.
INSERT INTO mantenimiento_equipo (equipo_id,fecha,descripcion,proxima_fecha,registrado_por) VALUES
(1,CURRENT_DATE - 40,'Dato de prueba: revisión del grupo de mangueras.',CURRENT_DATE + 50,1),
(5,CURRENT_DATE - 20,'Dato de prueba: revisión de equipos de respiración.',CURRENT_DATE + 70,1),
(12,CURRENT_DATE - 30,'Dato de prueba: revisión de herramienta hidráulica.',CURRENT_DATE + 60,1);
COMMIT;
