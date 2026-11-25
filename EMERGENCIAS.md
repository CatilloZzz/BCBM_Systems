# Emergencias del mes

En el menú, entra a **Emergencias** como administrador o despachador. Puedes abrir un acta con el número y la fecha del reporte, completar después el tipo, la escuadra, el solicitante, el teléfono, la dirección, la prioridad y la descripción, y asociar una ubicación pública opcional con el buscador existente.

El listado filtra por mes y año del reporte, número exacto de acta, escuadra, tipo y estado. Muestra una fila por acta, aunque no tenga salidas o tenga varias. Las columnas de movimientos identifican cada salida por su ID para relacionar la unidad con el operador, el jefe y los horarios. Abre **Ver detalle** para consultar la tripulación completa, despachar una unidad disponible o de refuerzo y registrar llegadas y retornos independientes.

En la ficha, **Cerrar acta** solicita la descripción final y el resultado, que puede ser atendida, falsa alarma o no atendida. La descripción inicial se conserva. El servicio compartido guarda el resultado en la misma transacción que el cierre y exige que todas las unidades hayan retornado. En un acta sin salidas también se puede guardar el resultado. Las asignaciones antiguas se muestran con sus datos disponibles y requieren revisión antes del cierre, conforme a la regla de despacho existente.

## Base de datos y ejecución

- Base nueva. Ejecuta `database/schema.sql` y, para una demostración local, `database/datos_prueba.sql`.
- Base existente. Aplica las migraciones pendientes de `database/migrations/` en orden. La migración `004_actas_emergencia.sql` agrega campos opcionales y un índice único para el número de acta. Conserva registros y asignaciones.
- En IntelliJ, usa JDK 21, el `config.properties` local junto a `pom.xml` y **Run MainApp**. También puedes ejecutar `mvnw.cmd javafx:run`.
- Para las pruebas, ejecuta `mvnw.cmd test`. Para incluir PostgreSQL usa `mvnw.cmd -Dbcbm.pgtest=true test` con la configuración local y permisos para crear un esquema temporal. La verificación del menú JavaFX y el login de demostración se ejecuta con `mvnw.cmd -Dtest=DespachoPostgresTest -Dbcbm.pgtest=true -Dbcbm.fxtest=true test`.

## Acuerdos y compatibilidad

- El operador ingresa un número único sin distinguir mayúsculas. No hay generación ni reinicio automático. Los registros anteriores que carecen de número se pueden localizar con su referencia `#ID`.
- En las ramas revisadas la escuadra no tiene un catálogo compartido. El formulario permite reutilizar valores de actas previas o escribir una nueva escuadra. Queda pendiente acordar el catálogo común.
- El mes de un acta se determina por la fecha del reporte. Las consultas del servicio de despacho siguen tomando el mes de la salida a partir de su fecha de inicio. Los tres horarios conservan fecha y hora completas.
- Despacho identifica cada refuerzo por su salida independiente y conserva el detalle de participantes por salida. Para un consolidado se deben distinguir actas, salidas y personas por actividad. La puesta en común del criterio queda a cargo del equipo.
- La ficha reutiliza `DespachoService` y su vista para disponibilidad, asignación, tripulación, llegadas, retornos y cierre. Los permisos de escritura se validan con la cuenta activa y el rol real en PostgreSQL.

## Revisión y demostración

1. Abre un acta con solo número y fecha. Búscala por su número y completa solicitante, tipo y escuadra.
2. En la ficha, registra dos salidas con unidades y personal distintos. Vuelve al listado y verifica una sola fila con ambas salidas.
3. Marca llegada y retorno de una unidad. Confirma que la otra sigue fuera y que el cierre queda bloqueado hasta registrar su retorno.
4. Cierra con descripción final y resultado. Reabre la ficha para verificar que se conservan la descripción inicial y los movimientos.
5. Abre otra acta y ciérrala como falsa alarma o no atendida sin asignarle unidades.

Commit sugerido `feat: agregar actas y listado mensual de emergencias`. El PR hacia `develops` debe mencionar la migración 004, los filtros, el flujo de cierre y la reutilización del despacho compartido.

## Reporte de emergencias

En **Reportes de cabina**, el botón **Emergencias** abre el reporte individual con el período elegido en la pantalla. Filtra las actas por fecha de apertura, tipo, prioridad y estado, y muestra el número de acta, la dirección y el resultado. Las pestañas **Por tipo**, **Por prioridad**, **Por estado** y **Por resultado** indican cuántas actas hubo de cada categoría, su porcentaje y cuántas siguen abiertas (pendientes o en proceso). El resumen señala el tipo más frecuente y las actas abiertas de fechas anteriores al período.

**Vista previa · PDF y Excel** usa la vista previa compartida de reportes. La plantilla es `reports/reporte-emergencias.jrxml` y el Excel incluye las hojas Resumen, Por tipo, Por prioridad, Por estado, Por resultado y Actas. La prueba `ReporteEmergenciasExportacionTest` verifica ambos formatos sin PostgreSQL.
