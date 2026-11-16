-- Aplicar después de 002_ubicacion_emergencia.sql. Conserva las asignaciones antiguas.
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
