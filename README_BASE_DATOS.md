# Base de datos de BCBM Systems

## Conexión local preparada

La base `bomberos_db` está creada en PostgreSQL local, puerto `5432`. El usuario de conexión depende de la configuración local de cada integrante.

La conexión puede configurarse en `config.properties`, en la raíz, o en el archivo local `src/main/resources/hibernate.properties`. Las variables `BCBM_DB_*` tienen prioridad, seguidas de `db.*` en `config.properties` y de las propiedades Hibernate. Los archivos de credenciales están excluidos de Git. El archivo `config.properties.example` puede compartirse con el equipo. Se admite contraseña vacía cuando PostgreSQL local lo permite.

Para abrir el sistema usa JDK 21 y ejecuta desde esta carpeta:

```powershell
.\mvnw.cmd javafx:run
```

También puedes ejecutar el objetivo `javafx:run` desde el panel Maven de IntelliJ, con JDK 21 en Maven Runner y esta carpeta como directorio de trabajo.

## Cuentas de demostración

| Usuario | Contraseña de la aplicación | Rol |
|---|---|---|
| admin | admin123 | ADMIN |
| despachador | despacho123 | DESPACHADOR |
| bombero1 | bombero123 | BOMBERO |

Estas contraseñas sirven para la pantalla de login del sistema. La conexión a PostgreSQL utiliza una contraseña diferente, guardada en la configuración local. Las contraseñas de demostración se guardan como hashes BCrypt; se verificó el login con el servicio real de la aplicación.

Como ya existen usuarios, no hace falta pulsar «Crear primer administrador».

## Estructura y datos cargados

| Tabla | Registros iniciales |
|---|---:|
| bombero | 10 |
| usuario | 3 |
| tipo_emergencia | 5 |
| emergencia | 3 |
| unidad | 5 |
| asignacion | 4 |
| equipo | 15 |
| mantenimiento_equipo | 3 |

`database/schema.sql` contiene las tablas del esquema compartido, mantenimiento, ubicación y las tres tablas de despacho y otros servicios (`otro_servicio`, `salida_unidad`, `salida_participante`). `database/datos_prueba.sql` conserva los datos ficticios compartidos, corrige los hashes de las tres cuentas y agrega tres mantenimientos ficticios. Las fechas de asignación se alinearon con la fecha de cada emergencia.

Los 15 registros de `equipo` representan grupos con su propia cantidad, no un total de 15 piezas.

## Uso de los scripts en otro equipo

1. Crear una base vacía llamada `bomberos_db` en PostgreSQL usando la cuenta local correspondiente.
2. Con esa base seleccionada, ejecutar `database/schema.sql` una vez.
3. Ejecutar `database/datos_prueba.sql` una vez.
4. Copiar `config.properties.example` como `config.properties` en la raíz y escribir las credenciales locales.
5. Iniciar el sistema con Java 21.

En este equipo los scripts ya fueron ejecutados. El script de datos rechaza cargar registros si las tablas ya tienen información; no incluye el TRUNCATE del archivo original y no reinicia los datos.

Para actualizar una base existente, aplicar en orden todas las migraciones pendientes de `database/migrations` hasta la 008. La 008 traslada las asignaciones existentes a `dotacion_equipo` y permite distribuir parte del inventario a cada unidad. Aplicar cada script una sola vez y mantener `hibernate.hbm2ddl.auto=validate`. Los reportes aceptan fechas inclusivas, exportan un libro `.xlsx` con tres hojas y pueden generar PDF con JasperReports.

En pgAdmin, actualiza el nodo Databases para ver `bomberos_db`. Sus tablas están en Schemas → public → Tables. Puedes utilizar tu conexión habitual de administrador para inspeccionar la base.

## Validación realizada

- Compilación del proyecto local correcta con JDK 21.
- Conexión real de Hibernate a `jdbc:postgresql://localhost:5432/bomberos_db`.
- Validación de las cinco entidades que actualmente registra el módulo de Rafael; las ocho tablas están creadas para la integración grupal.
- Login correcto de las tres cuentas y comprobación de sus roles.
- Lectura de personal, usuarios, equipos, unidades e historial de mantenimiento desde los servicios.
- La cuenta BOMBERO consulta únicamente su ficha de personal.
- Archivo de credenciales excluido de Git.

La integración de las entidades y pantallas de emergencias y asignaciones corresponde al trabajo conjunto con los otros módulos.


## Integración de develops con Rafael y Julián

Para una base existente, ejecutar en Query Tool de `bomberos_db` las migraciones pendientes de `database/migrations/` en orden hasta la 008. La 004 añade campos del acta; la 005 amplía los datos del personal; la 006 amplía los servicios y estados de unidad; la 007 incorpora catálogos; la 008 crea dotaciones parciales por unidad y convierte las asignaciones anteriores. Los scripts conservan los registros existentes. No ejecutar de nuevo `schema.sql` sobre esa base.

Para una instalación nueva, `database/schema.sql` ya incluye esas columnas. La configuración local sigue en `config.properties`, tomando como guía `config.properties.example`.

El proyecto conserva Java 21 y el arranque modular con `module-info.java`. El menú incluye Emergencias para administrador y despachador, además de personal, inventario, usuarios y mantenimiento. Se unificaron los mapeos con las columnas `tipo_id`, `registrado_por` y `fecha_hora` del esquema compartido. Los estados Java PENDIENTE y EN_PROCESO corresponden a REPORTADA y EN_CURSO en PostgreSQL.

La ficha consulta las asignaciones antiguas y usa el servicio de despacho compartido para las nuevas salidas. El cierre guarda la descripción final y el resultado después de comprobar que todas las unidades hayan retornado. Consulta [Emergencias del mes](EMERGENCIAS.md) para ver el recorrido de prueba.
