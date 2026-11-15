# Acceso a datos y persistencia

## 1. Mecanismo de conexión

La aplicación utiliza JDBC para conectarse directamente a PostgreSQL. La clase de conexión identificada es `ConexionBD.ConexionBD`, ubicada en `src/main/java/ConexionBD/ConexionBD.java`.

Esta clase configura una URL JDBC, usuario y contraseña como constantes y obtiene conexiones mediante `DriverManager.getConnection(...)`. Las clases de servicio, DAO y repositorio utilizan esta conexión para ejecutar operaciones SQL.

## 2. DAO y repositorios

El código contiene dos paquetes para acceso a datos:

### `dao`

Contiene clases orientadas a operaciones específicas de algunos módulos:

- `DisponibilidadDAO`: recalcula estados y consulta disponibilidad de productos.
- `ProductoDAO`: consulta el menú agrupado por categorías y verifica disponibilidad.
- `PagoDAO`: consulta cuentas y registra el cierre de pagos.

### `repository`

Contiene la mayor parte de las operaciones de persistencia. Algunos ejemplos:

- `UsuarioRepository`: búsqueda de usuarios y actualización de hashes.
- `MesaRepository`: consulta y actualización de mesas.
- `PedidoRepository`: creación, consulta y actualización de pedidos.
- `IngredienteRepository`: consulta de ingredientes y actualización de existencias.
- `InventarioRepository`: recetas, consulta bloqueante de stock, descuentos, devoluciones y movimientos.
- `AlertaInventarioRepository`: consulta, creación y resolución de alertas.

Tanto DAO como repositorios usan JDBC. La separación por nombre refleja la organización histórica del código y no una diferencia tecnológica.

## 3. Responsabilidades

- Los controladores no deberían construir SQL ni administrar directamente conexiones.
- Los servicios coordinan reglas de negocio y llaman a los componentes de persistencia.
- Los DAO y repositorios encapsulan consultas SQL y mapeo de resultados.
- Las entidades representan datos del dominio y los DTO transportan datos para casos de uso concretos.

## 4. Transacciones

Algunas operaciones reciben una conexión JDBC existente para ejecutar varias sentencias dentro de una misma transacción. Esto permite coordinar cambios relacionados, por ejemplo, la actualización de un pedido y sus detalles o el descuento y reversión de inventario.

Cuando se modifica una operación transaccional, se debe comprobar que todas las sentencias relacionadas utilicen la misma conexión y que el código gestione correctamente `commit` y `rollback`.

## 5. Seguridad de credenciales

En la versión revisada, `ConexionBD.java` contiene los parámetros de conexión como constantes. La configuración de `PGHOST`, `PGPORT`, `PGUSER` y `PGPASSWORD` utilizada por `scripts/setup.sh` corresponde a las herramientas de PostgreSQL y no cambia automáticamente las constantes de la conexión JDBC de la aplicación.

Antes de compartir o desplegar el proyecto, se recomienda mover las credenciales de la aplicación a variables de entorno o a un archivo local excluido de Git. No se deben guardar contraseñas reales en el repositorio.

## 6. Sin API REST

No se identificó una API REST activa en la configuración de compilación ni en el punto de entrada de la aplicación. Las operaciones de acceso a datos se realizan desde la aplicación JavaFX mediante JDBC. Por este motivo, los contratos relevantes se documentan como métodos de servicios, DAO y repositorios, no como rutas HTTP.
