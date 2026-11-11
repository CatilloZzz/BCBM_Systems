# Módulo de emergencias

La pantalla permite registrar y consultar emergencias, editar las que siguen abiertas y eliminar las pendientes sin asignaciones. El catálogo permite agregar y renombrar tipos, así como eliminar los que no estén en uso. El listado combina filtros por estado, tipo y fechas de registro. Ambos días del intervalo se incluyen.

## Integración

1. Configure PostgreSQL con las credenciales locales del proyecto. Cree primero la tabla `usuario` del módulo de usuarios y ejecute `database/emergencias.sql` sobre la misma base. El script crea las tablas del módulo y sus claves foráneas.
2. En la configuración compartida, registre las clases anotadas `Emergencia` y `TipoEmergencia` antes de construir la `SessionFactory`, junto con las entidades de los demás módulos. La rama de Julián contiene el archivo de ejemplo de credenciales. La fábrica y su ciclo de vida corresponden al arranque común.
3. Abra la vista desde el menú de la sesión con `EmergenciasController.cargar(factory, () -> sesion.id(), cierre)`. Si se reutiliza la inclusión de `hello-view.fxml`, el `FXMLLoader` expone el controlador como `emergenciasController` en su espacio de nombres. Llame a `conectar(factory, proveedorUsuarioId, cierre)` en ese controlador. El proveedor debe leer el identificador de la sesión autenticada y retornar `null` si no hay sesión.
4. Pase una referencia al servicio transaccional de Josué adaptada a `CierreEmergencia.cerrar(Integer)`. El servicio debe finalizar el commit antes de retornar y propagar los fallos. Si su API retorna un indicador de éxito, el adaptador debe lanzar una excepción cuando el indicador sea falso. Mientras falte el servicio, pase `null` para mantener deshabilitado el botón.

El registro inicia en `PENDIENTE`. Se admite pasar de `PENDIENTE` a `EN_PROCESO` al despachar y de cualquiera de esos dos estados a `CERRADA` mediante el cierre transaccional. Los servicios de despacho y cierre pueden llamar a `Emergencia.cambiarEstado` sobre la entidad administrada dentro de su transacción. La pantalla vuelve a consultar la base después de solicitar el cierre y muestra la confirmación si el servicio retornó correctamente y el estado persistido es `CERRADA`.

## Consultas HQL y demostración

- En el listado, seleccione un estado y pulse **Filtrar**. La consulta usa `join fetch e.tipo` y `join Usuario u on u.id = e.usuarioId`. La relación con el autor se guarda en `usuario_id` con una clave foránea. La consulta usa las propiedades `id`, `username` y, para el registro, `activo` de la entidad de la rama de Rafael.
- Abra **Ver detalle** sobre una emergencia despachada. La consulta usa `Asignacion.emergencia.id` y las asociaciones `Asignacion.unidad` y `Asignacion.bombero`. Muestra `Unidad.codigo` y `Bombero.nombre` y `apellido`. La entidad de Josué debe estar registrada como `Asignacion` con esas propiedades y su tabla debe tener una clave foránea a `emergencia(id)` que impida borrar una emergencia referenciada.
- Para demostrar el cierre, registre una emergencia con una sesión activa, despáchela desde el módulo correspondiente y cierre la emergencia desde el listado. Quite el filtro de estado para ver la fila cerrada y compruebe en despacho que los recursos estén disponibles.

La rama de Julián puede abrir la vista de arranque, que se habilita al recibir la fábrica compartida. La consulta de asignaciones muestra un aviso mientras falte su mapeo.
