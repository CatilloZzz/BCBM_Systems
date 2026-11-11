# Base de datos de BCBM Systems

## Conexión local preparada

La base `bomberos_db` está creada en PostgreSQL local, puerto `5432`. El sistema usa el usuario dedicado `bcbm_app`, que es propietario de esta base y no tiene permisos de superusuario, crear bases ni crear roles.

La conexión está en `config.properties`, en la raíz del proyecto, junto a `pom.xml`. `HibernateUtil` lee ese archivo; no utiliza el archivo de ejemplo ubicado en `src/main/resources`. La contraseña de PostgreSQL es local y está excluida de Git, al igual que la carpeta `.local/`. El archivo `config.properties.example` puede compartirse con el equipo.

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

`database/schema.sql` contiene las siete tablas del esquema compartido y la octava tabla de mantenimiento, junto con restricciones de fechas de cierre. `database/datos_prueba.sql` conserva los datos ficticios compartidos, corrige los hashes de las tres cuentas y agrega tres mantenimientos ficticios. Las fechas de asignación se alinearon con la fecha de cada emergencia.

Los 15 registros de `equipo` representan grupos con su propia cantidad, no un total de 15 piezas.

## Uso de los scripts en otro equipo

1. Crear una base vacía llamada `bomberos_db` en PostgreSQL usando la cuenta local correspondiente.
2. Con esa base seleccionada, ejecutar `database/schema.sql` una vez.
3. Ejecutar `database/datos_prueba.sql` una vez.
4. Copiar `config.properties.example` como `config.properties` en la raíz y escribir las credenciales locales.
5. Iniciar el sistema con Java 21.

En este equipo los scripts ya fueron ejecutados. El script de datos rechaza cargar registros si las tablas ya tienen información; no incluye el TRUNCATE del archivo original y no reinicia los datos.

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
