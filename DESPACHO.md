# Despacho, unidades y Otros servicios — Josué

Implementado en Java 21, JavaFX/FXML, Hibernate y PostgreSQL, sin dependencias nuevas.

## Preparación y uso

Para una base existente, ejecutar una sola vez `database/migrations/003_despacho_servicios.sql`, después de las migraciones anteriores (001 de integración y 002 de ubicación). Para una base vacía, `database/schema.sql` ya incluye estas tablas: no ejecutar también la migración 003. Se conserva el historial de `asignacion`; no se inventan horarios para registros antiguos.

Se mantiene `hibernate.hbm2ddl.auto=validate`. La conexión acepta las propiedades locales de `hibernate.properties`; las claves `db.*` de `config.properties` tienen prioridad y las variables `BCBM_DB_*` tienen prioridad sobre ambas. La contraseña vacía se admite si PostgreSQL local lo permite. No subir credenciales.

El administrador y el despachador tienen acceso a **Despacho y movimientos** y **Otros servicios**. Los permisos se comprueban nuevamente en cada operación y consulta usando el usuario vigente de la base, no solo ocultando botones.

1. Registrar el aviso en Emergencias.
2. Abrir Despacho y movimientos → Nueva salida. Seleccionar emergencia, unidad, operador, jefe opcional, otros participantes y fecha/hora.
3. Agregar refuerzos repitiendo la salida sobre la misma emergencia con otra unidad.
4. Seleccionar cada salida para registrar llegada y retorno. Solo la unidad que retornó vuelve a estar disponible.
5. En Otros servicios, registrar escuadra/departamento, tipo, detalle, unidad, operador, jefe opcional, participantes y salida. Registrar entrada al terminar.

La selección de participantes permite Ctrl/clic. Operador y jefe se incluyen automáticamente, una sola vez, en la tripulación. El usuario que digitó se conserva por separado. Las unidades ocupadas se identifican en el selector y el servicio rechaza su despacho. El estado compartido existente `EN_EMERGENCIA` significa unidad ocupada tanto por emergencias como por otros servicios; el selector muestra «Ocupada».

El filtro mensual utiliza la fecha completa de salida. **Unidades fuera** consulta salidas abiertas de todos los meses, para encontrar retornos pendientes aunque la actividad empezara el mes anterior.

## API para Julián

Crear `new DespachoService(factory)` con la fábrica compartida y pasar la `SesionUsuario` autenticada.

- `catalogos(sesion)`: unidades con su estado, personal vigente y emergencias abiertas. La referencia actual del acta es el ID; la numeración oficial pertenece al módulo de Julián.
- `asignarUnidad(sesion, emergenciaId, tripulacion)`: devuelve el ID de la salida. Usar también para refuerzos.
- `marcarLlegada(sesion, salidaId, fechaHora)`.
- `registrarRetorno(sesion, salidaId, fechaHora)`: para Otros servicios representa la entrada.
- `consultarSalidas(sesion, emergenciaId)`: devuelve todas las salidas de esa emergencia, con sus horarios, operador, jefe y participantes, incluso después del retorno.
- `cerrarEmergencia(sesion, emergenciaId)`: exige que todas las unidades hayan retornado. Admite cerrar una emergencia sin salidas. Se conectó al adaptador `CierreEmergencia` del menú; la descripción final y el resultado siguen correspondiendo a Julián.

`Tripulacion` recibe `(unidadId, operadorId, jefeId, participantesIds, fechaHoraSalida)`. Jefe puede ser null; operador es obligatorio. Se rechazan participantes inexistentes o de baja. Las fechas no pueden ser futuras, preceder al movimiento anterior ni registrar una nueva salida anterior al último retorno de la unidad.

La ficha actual de Emergencias consulta también la tripulación de las nuevas salidas. Julián puede conectar su formulario y las columnas mensuales a `consultarSalidas` para mostrar los horarios y los roles de cada salida.

## Datos para Rafael/Nazario

- `listarMes(sesion, YearMonth, otrosServicios)`: false para emergencias, true para Otros servicios.
- `listarActivas(sesion, otrosServicios)`: salidas sin retorno, independientemente del mes.
- Cada registro incluye `id` de salida, `emergenciaId` o `servicioId`, `unidadId`, operador y jefe con sus IDs, lista de participantes con IDs, `registradoPor` y horarios completos.
- No contar filas de tripulación como emergencias. Para actividades, contar IDs de acta/servicio distintos. Para movimientos, contar IDs de salida. Para participación por persona y actividad, deduplicar `(emergenciaId, bomberoId)` o `(servicioId, bomberoId)`, incluso si una persona figura en varios refuerzos.
- Operador es un bombero de la tripulación; `registradoPor` es el usuario que digitó, no el operador de la unidad.

## Reglas que deben confirmar con el puesto de mando

Se usa provisionalmente el mes de la salida, aunque el retorno ocurra en el mes siguiente. Las consultas entregan las fechas completas para ajustar el criterio cuando lo confirmen. La regla de conteo por persona/actividad anterior es la propuesta del ejemplo compartido, no una política impuesta por este módulo. La numeración de actas y su reinicio se mantienen fuera de este cambio.

Una salida pertenece a una única emergencia o servicio. El bloqueo de la fila de unidad, la escritura transaccional y un índice único parcial impiden dos salidas abiertas de la misma unidad. Los retornos conservan el historial. Las asignaciones antiguas no se convierten automáticamente: una emergencia con registros antiguos exige revisión antes de utilizar el nuevo cierre.

## Comprobaciones

`mvn test`: pruebas de secuencia, fechas y la integración existente.

`mvn -Dbcbm.pgtest=true test`: agrega pruebas reales en PostgreSQL local (`bomberos_db`, usuario `postgres`, contraseña vacía), en esquemas temporales aleatorios, sin insertar datos de prueba en public. Verifica dos unidades, retornos independientes, disponibilidad compartida, reversión ante errores, permisos y despachos simultáneos.

`mvn -Dbcbm.pgtest=true -Dbcbm.fxtest=true -Dtest=DespachoPostgresTest test`: agrega la carga y consulta de ambas pantallas JavaFX. Ejecutar por separado porque JavaFX no permite reiniciar su plataforma dentro del mismo proceso de pruebas.

Prueba conjunta: abrir un accidente con dos unidades y cuatro bomberos distintos; retornar ambas; registrar una cobertura con una unidad y uno de esos bomberos. En el mismo mes: una emergencia, un otro servicio, tres salidas; ese bombero tiene una participación en cada actividad.
