# GastroFlow — descripción técnica de la aplicación

## 1. Propósito

GastroFlow es una aplicación de escritorio para apoyar la operación de un restaurante. Reúne funcionalidades de autenticación, inventario, disponibilidad de productos, administración de mesas, comandas de cocina y pagos. La aplicación está desarrollada con Java 21 y JavaFX, y persiste la información en PostgreSQL mediante JDBC.

Este documento describe los módulos y flujos que se identifican en el código de la versión revisada. La presencia de una entidad, tabla o clase de servicio no implica por sí sola que exista una pantalla terminada o accesible desde la navegación principal.

## 2. Módulos y pantallas identificados

| Módulo / vista | Rol configurado | Responsabilidad observada | Vista |
|---|---|---|---|
| Acceso al sistema | Todos | Validación de credenciales y apertura de sesión. | `login.fxml` |
| Disponibilidad de platos | Administrador | Consulta y actualización de disponibilidad según ingredientes. | `admin-disponibilidad.fxml` |
| Inventario | Administrador | Consulta de ingredientes, existencias y alertas. | `inventario.fxml` |
| Mapa de salón | Mesero | Consulta de mesas, estados, ocupación y detalles. | `MapaSalon.fxml` |
| Tablero de comandas | Cocina | Consulta de pedidos para preparación y avance de estado. | `cocina-view.fxml` |
| Pago de cuentas | Cajero | Consulta de cuentas y registro de descuentos, propinas y pagos. | `cajero-pago-view.fxml` |

### Vista de menú del mesero

El repositorio también contiene `menu-mesero.fxml` y `MenuMeseroController`. Sin embargo, esta vista no está incluida en la lista `MODULOS` de `Main.java` en la versión revisada. Debe considerarse una vista existente en el código, pero no una opción confirmada de la navegación principal.

## 3. Procesos principales

### 3.1 Inicio de sesión

1. El usuario ingresa su identificador y contraseña en la vista de acceso.
2. `LoginController` llama a `AutenticacionService`.
3. El servicio consulta el usuario mediante `UsuarioRepository`.
4. Se valida el estado del usuario y la contraseña almacenada.
5. Si las credenciales son correctas, se crea la sesión mediante `SesionUsuario`.
6. `Main` presenta los módulos habilitados para el rol del usuario.

`AutenticacionService` reconoce hashes BCrypt y un formato heredado SHA-256 con salt. Cuando un usuario válido inicia sesión con el formato heredado, el servicio actualiza el hash a BCrypt.

### 3.2 Disponibilidad de productos

El módulo de disponibilidad consulta el catálogo y verifica si los ingredientes necesarios alcanzan para preparar cada producto. El DAO `DisponibilidadDAO` invoca funciones almacenadas de PostgreSQL para recalcular estados, consultar disponibilidad y obtener información de ingredientes faltantes. Los estados contemplados en el DDL de productos incluyen `DISPONIBLE`, `AGOTADO` e `INACTIVO`.

Las funciones SQL asociadas a este proceso se encuentran en `db/functions/hu28_disponibilidad.sql`.

### 3.3 Inventario y alertas

El sistema contiene servicios y repositorios para consultar existencias, registrar entradas y salidas, descontar ingredientes y revertir descuentos asociados a pedidos. También contiene entidades y repositorios para alertas de inventario, con estados de seguimiento.

Las clases principales incluyen `InventarioService`, `InventarioRepository`, `EntradaInventarioService`, `EntradaInventarioRepository` y `AlertaInventarioRepository`.

### 3.4 Mesas y salón

El mapa de salón presenta las mesas y sus estados. Las operaciones de consulta y modificación se distribuyen entre `MapaSalonController`, `MesaService` y `MesaRepository`. El modelo de datos contempla zonas, estados de mesa, historial de cambios, notas, reservas y asignaciones.

### 3.5 Pedidos y cocina

El proyecto incluye servicios y repositorios para crear, consultar y actualizar pedidos. `PedidoFacade` coordina validaciones y confirmación del pedido; `PedidoService` concentra operaciones del ciclo de vida del pedido. Para la vista de cocina se encuentran `CocinaController`, `ComandaCocina` y `PedidoComandaAdapter`.

### 3.6 Pagos, propinas y descuentos

El módulo de caja utiliza `CajeroPagoController`, `PagoDAO` y objetos DTO como `CuentaPago`. El flujo permite consultar cuentas, calcular importes y registrar el cierre del pago. Las estrategias de descuento se encuentran en `service/descuento`, con implementaciones para monto fijo y porcentaje.

## 4. Organización del código

```text
src/main/java/
├── Main.java
├── ConexionBD/
├── Interface/
├── controller/
├── dao/
├── dto/
├── entity/
├── enums/
├── exceptions/
├── metricas/
├── repository/
├── scheduler/
├── service/
│   ├── alerta/
│   ├── cocina/
│   └── descuento/
├── session/
└── util/

src/main/resources/
├── css/
├── static/
└── views/
```

## 5. Recursos de interfaz

- `src/main/resources/views/`: archivos FXML de las ventanas.
- `src/main/resources/css/`: hojas de estilo de JavaFX.
- `src/main/resources/static/`: recursos estáticos adicionales.

## 6. Consideraciones de alcance

- La aplicación es de escritorio; no se documentan endpoints REST porque no se identificó una API HTTP activa.
- Las clases `entity`, `repository` y las tablas de base de datos describen el modelo y la persistencia, pero no garantizan que todas las operaciones tengan una pantalla terminada.
- La navegación principal se debe contrastar con `Main.java`, que es la fuente de verdad para los módulos visibles por rol.
