-- BCBM: esquema PostgreSQL basado en las consultas compartidas.
-- Ejecutar una vez en bomberos_db VACÍA; incluye las ocho tablas.
BEGIN;
CREATE TABLE bombero (
 id SERIAL PRIMARY KEY, nombre VARCHAR(100) NOT NULL, apellido VARCHAR(100) NOT NULL,
 cedula VARCHAR(20) NOT NULL UNIQUE, rango VARCHAR(40) NOT NULL, telefono VARCHAR(20),
 fecha_ingreso DATE NOT NULL DEFAULT CURRENT_DATE,
 estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE' CHECK (estado IN ('DISPONIBLE','EN_EMERGENCIA','DE_BAJA'))
);
CREATE TABLE usuario (
 id SERIAL PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, password_hash VARCHAR(100) NOT NULL,
 rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMIN','DESPACHADOR','BOMBERO')),
 bombero_id INT UNIQUE REFERENCES bombero(id), activo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE tipo_emergencia (id SERIAL PRIMARY KEY, version BIGINT NOT NULL DEFAULT 0, nombre VARCHAR(60) NOT NULL UNIQUE);
CREATE TABLE emergencia (
 id SERIAL PRIMARY KEY, version BIGINT NOT NULL DEFAULT 0, tipo_id INT REFERENCES tipo_emergencia(id), direccion VARCHAR(200),
 descripcion TEXT, prioridad VARCHAR(10) CHECK (prioridad IN ('BAJA','MEDIA','ALTA','CRITICA')),
 estado VARCHAR(20) NOT NULL DEFAULT 'REPORTADA' CHECK (estado IN ('REPORTADA','EN_CURSO','CERRADA')),
 fecha_hora TIMESTAMP NOT NULL DEFAULT NOW(), fecha_cierre TIMESTAMP, registrado_por INT REFERENCES usuario(id),
 numero_acta VARCHAR(30), escuadra VARCHAR(100), solicitante VARCHAR(120), telefono VARCHAR(30),
 descripcion_final TEXT, resultado VARCHAR(24)
);
CREATE UNIQUE INDEX emergencia_numero_acta_idx ON emergencia (lower(numero_acta));
CREATE TABLE unidad (
 id SERIAL PRIMARY KEY, codigo VARCHAR(20) NOT NULL UNIQUE, tipo VARCHAR(30) NOT NULL,
 estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE' CHECK (estado IN ('DISPONIBLE','EN_EMERGENCIA','MANTENIMIENTO'))
);
CREATE TABLE asignacion (
 id SERIAL PRIMARY KEY, emergencia_id INT NOT NULL REFERENCES emergencia(id) ON DELETE CASCADE,
 unidad_id INT NOT NULL REFERENCES unidad(id), bombero_id INT NOT NULL REFERENCES bombero(id),
 fecha_asignacion TIMESTAMP NOT NULL DEFAULT NOW(), UNIQUE (emergencia_id,bombero_id)
);
CREATE TABLE equipo (
 id SERIAL PRIMARY KEY, nombre VARCHAR(100) NOT NULL, categoria VARCHAR(40) NOT NULL,
 cantidad INT NOT NULL DEFAULT 1 CHECK (cantidad >= 0),
 estado VARCHAR(20) NOT NULL DEFAULT 'OPERATIVO' CHECK (estado IN ('OPERATIVO','EN_REPARACION','BAJA')),
 ultimo_mantenimiento DATE, unidad_id INT REFERENCES unidad(id)
);
CREATE INDEX idx_emergencia_estado ON emergencia(estado);
CREATE INDEX idx_emergencia_fecha ON emergencia(fecha_hora);
CREATE INDEX idx_asignacion_emergencia ON asignacion(emergencia_id);
CREATE INDEX idx_asignacion_bombero ON asignacion(bombero_id);

CREATE TABLE mantenimiento_equipo (
    id SERIAL PRIMARY KEY,
    equipo_id INT NOT NULL REFERENCES equipo(id),
    fecha DATE NOT NULL,
    descripcion TEXT NOT NULL CHECK (length(trim(descripcion)) > 0),
    proxima_fecha DATE,
    registrado_por INT NOT NULL REFERENCES usuario(id),
    CONSTRAINT ck_mantenimiento_proxima CHECK (proxima_fecha IS NULL OR proxima_fecha >= fecha)
);
CREATE INDEX idx_mantenimiento_equipo_fecha ON mantenimiento_equipo(equipo_id, fecha DESC);

ALTER TABLE emergencia ADD CONSTRAINT ck_emergencia_fecha_cierre
    CHECK (fecha_cierre IS NULL OR fecha_cierre >= fecha_hora);
ALTER TABLE emergencia ADD CONSTRAINT ck_emergencia_cerrada_fecha
    CHECK (estado <> 'CERRADA' OR fecha_cierre IS NOT NULL);
ALTER TABLE emergencia ADD COLUMN latitud DOUBLE PRECISION, ADD COLUMN longitud DOUBLE PRECISION;
COMMIT;

-- Tablas del módulo de despacho y otros servicios (migración 003).
BEGIN;
CREATE TABLE otro_servicio (
 id SERIAL PRIMARY KEY, escuadra_departamento VARCHAR(100) NOT NULL,
 tipo VARCHAR(40) NOT NULL CHECK (tipo IN ('COBERTURA_EVENTOS','GESTION_ADMINISTRATIVA','TALLER','CAPACITACION','OTRO')),
 detalle TEXT NOT NULL CHECK (length(trim(detalle)) BETWEEN 1 AND 4000)
);
CREATE TABLE salida_unidad (
 id SERIAL PRIMARY KEY, emergencia_id INTEGER REFERENCES emergencia(id),
 otro_servicio_id INTEGER REFERENCES otro_servicio(id), unidad_id INTEGER NOT NULL REFERENCES unidad(id),
 operador_id INTEGER NOT NULL REFERENCES bombero(id), jefe_id INTEGER REFERENCES bombero(id),
 registrado_por INTEGER NOT NULL REFERENCES usuario(id), salida TIMESTAMP NOT NULL, llegada TIMESTAMP, retorno TIMESTAMP,
 CONSTRAINT ck_salida_destino CHECK ((emergencia_id IS NOT NULL) <> (otro_servicio_id IS NOT NULL)),
 CONSTRAINT ck_salida_llegada CHECK (llegada IS NULL OR (emergencia_id IS NOT NULL AND llegada >= salida)),
 CONSTRAINT ck_salida_retorno CHECK (retorno IS NULL OR (retorno >= salida AND (llegada IS NULL OR retorno >= llegada))),
 CONSTRAINT ck_salida_secuencia CHECK (emergencia_id IS NULL OR retorno IS NULL OR llegada IS NOT NULL)
);
CREATE TABLE salida_participante (
 salida_id INTEGER NOT NULL REFERENCES salida_unidad(id),
 bombero_id INTEGER NOT NULL REFERENCES bombero(id), PRIMARY KEY (salida_id,bombero_id)
);
CREATE UNIQUE INDEX uq_unidad_salida_abierta ON salida_unidad(unidad_id) WHERE retorno IS NULL;
CREATE INDEX idx_salida_emergencia ON salida_unidad(emergencia_id);
CREATE INDEX idx_salida_servicio ON salida_unidad(otro_servicio_id);
CREATE INDEX idx_salida_mes ON salida_unidad(salida);
CREATE INDEX idx_participante_bombero ON salida_participante(bombero_id);
COMMIT;
