# Interfaz BCBM

Las pantallas de personal, usuarios, equipos y mantenimiento utilizan FXML, con el logo del Benemérito Cuerpo de Bomberos de Managua y colores institucionales rojo, blanco y dorado.

## Organización

Dentro de `src/main/resources/edu/ni/uam/proyectofinalferrogest/`:

```text
fxml/
  login.fxml           Inicio de sesión
  primer-admin.fxml    Creación del primer administrador
  menu.fxml            Menú lateral y contenedor de pantallas
  inicio.fxml          Resumen de personal e inventario
  personal.fxml        Consulta y gestión de bomberos
  usuarios.fxml        Cuentas y roles
  equipos.fxml         Inventario
  mantenimientos.fxml  Registro e historial de mantenimiento
css/
  bcbm.css             Colores, tipografía, botones, tablas y formularios
images/
  logo-bcbm.png         Logo institucional
```

En `src/main/java/edu/ni/uam/proyectofinalferrogest/`, `controller/` contiene los controladores de cada pantalla y `util/Vistas.java` centraliza la carga de los FXML y recursos. `MainApp` configura la ventana y el cambio entre sesión y menú.

Los FXML definen la distribución, los controles, sus identificadores y los eventos. Los controladores reciben la sesión, conectan los eventos con los servicios existentes y presentan los resultados. Los servicios y DAO mantienen las validaciones, permisos y persistencia con Hibernate.

## Edición visual

Abrir un archivo de `fxml/` con Scene Builder. Cada vista referencia `../css/bcbm.css`, por lo que sus estilos también están disponibles al diseñarla. Conservar los `fx:id`, `fx:controller` y manejadores `onAction` para mantener la conexión con el controlador.

El logo se utiliza desde `images/logo-bcbm.png`; no depende de una ruta del escritorio. Los colores y tamaños comunes se modifican en `css/bcbm.css`.

## Comportamiento

- Inicio muestra conteos reales de personal, disponibilidad y equipos. El alcance del personal depende del rol; no representa estadísticas de emergencias o despacho.
- Las tablas permiten búsqueda sin distinguir mayúsculas ni tildes, ordenación y selección para consultar un registro.
- Los formularios desplazan sus campos y mantienen visibles las acciones principales cuando se reduce la ventana.
- Administrador puede gestionar los módulos existentes. Los demás roles conservan los permisos de consulta definidos por los servicios; la gestión de usuarios está reservada al administrador.
- Las consultas se ejecutan fuera del hilo visual y muestran un indicador de carga.
- Los errores y las confirmaciones se presentan en español. Las bajas de personal conservan el historial.

## Ejecución

Usar JDK 21 y configurar PostgreSQL según [README_BASE_DATOS.md](README_BASE_DATOS.md). Desde la raíz del proyecto:

```powershell
.\mvnw.cmd clean javafx:run
```

`config.properties` contiene la configuración local y debe permanecer fuera de Git. Los archivos `.example` sirven de plantilla.

## Verificación del rediseño

Se comprobó la compilación con Java 21 y la carga de las ocho vistas FXML. También se verificaron navegación, consulta de datos existentes, selección de filas, búsqueda con tildes, consulta del historial y visibilidad según rol. Se revisó la distribución a 1320 × 860 y 1120 × 760. Las comprobaciones visuales no modificaron registros de la base de datos.

Se reemplazaron `hello-view.fxml` y el archivo vacío `CSS.css` por las vistas y estilos de este módulo.
