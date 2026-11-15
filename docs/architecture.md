# Arquitectura de GastroFlow

## 1. Decisión arquitectónica

GastroFlow está implementado como una **aplicación de escritorio JavaFX**, desarrollada en Java y conectada directamente a PostgreSQL mediante **JDBC**. La aplicación se ejecuta en el equipo del usuario; no se identifica un backend HTTP independiente ni una API REST como parte de la arquitectura actual.

La arquitectura utiliza separación por responsabilidades: interfaz gráfica, controladores, servicios de negocio y acceso a datos. Las clases del dominio y los objetos de transferencia apoyan el intercambio de información entre estas capas.

## 2. Tecnologías principales

| Elemento | Tecnología / versión configurada | Responsabilidad |
|---|---|---|
| Lenguaje | Java 21 | Lógica de la aplicación. |
| Interfaz de escritorio | JavaFX 21.0.5 | Ventanas y controles gráficos. |
| Vistas | FXML | Declaración de las interfaces. |
| Estilos | CSS | Apariencia de las vistas JavaFX. |
| Acceso a datos | JDBC | Ejecución de consultas SQL y transacciones. |
| Base de datos | PostgreSQL 16 | Persistencia de la información. |
| Driver | PostgreSQL JDBC 42.7.5 | Conexión Java–PostgreSQL. |
| Construcción | Maven | Dependencias y ciclo de construcción. |
| Pruebas | JUnit 5.11.4 | Pruebas automatizadas. |

Las versiones se toman de la configuración de `pom.xml` y de la documentación de instalación del repositorio.

## 3. Vista lógica por capas

```text
Usuario
  |
  v
Interfaz JavaFX (FXML + CSS)
  |
  v
Controladores (controller)
  |
  v
Servicios (service)
  |
  v
Acceso a datos (repository / dao)
  |
  v
Conexión JDBC (ConexionBD.ConexionBD)
  |
  v
PostgreSQL (gastroflow)
```

### Interfaz gráfica

Las vistas se encuentran en `src/main/resources/views/` y se declaran mediante archivos FXML. Los estilos se encuentran en `src/main/resources/css/`. La clase `Main` extiende `javafx.application.Application`, inicia la ventana principal y coordina la navegación hacia las vistas.

### Controladores

El paquete `controller` contiene los controladores JavaFX. Reciben las acciones realizadas en la interfaz, validan datos de entrada a nivel de presentación y delegan las operaciones a los servicios correspondientes.

### Servicios

El paquete `service` concentra reglas y procesos del negocio. Entre sus responsabilidades se encuentran autenticación, inventario, disponibilidad de productos, pedidos, mesas, reservas, cocina y descuentos. Existen subpaquetes para alertas, cocina y estrategias de descuento.

### Acceso a datos

La persistencia se implementa con JDBC. El repositorio contiene la mayor parte de las clases de consulta y modificación; el paquete `dao` contiene clases específicas para disponibilidad, productos y pagos. Ambos ejecutan SQL mediante conexiones JDBC; no representan dos motores de persistencia diferentes.

### Dominio y objetos auxiliares

- `entity`: objetos que representan conceptos del dominio, por ejemplo `Ingrediente`, `Producto`, `Pedido`, `Mesa` y `UsuarioAutenticable`.
- `dto`: objetos que transportan datos entre partes de la aplicación, por ejemplo `CuentaPago`, `ItemPedido` e `IngredienteStockDTO`.
- `enums`: estados y tipos controlados, como `EstadoMesa`, `EstadoPedido`, `MetodoPago` y `TipoMovimiento`.
- `exceptions`: excepciones específicas del dominio.
- `session`: contiene `SesionUsuario`, que administra la sesión activa.
- `util`: funciones auxiliares compartidas.
- `Interface`: contratos de interfaces; actualmente incluye `InsumoObserver`.

## 4. Inicio y navegación

El punto de entrada configurado en `pom.xml` es `Main`. Al iniciar, presenta el formulario de acceso. `LoginController` delega la validación de credenciales a `AutenticacionService`; este consulta `UsuarioRepository` y, si el acceso es válido, crea la sesión mediante `SesionUsuario`.

Después del inicio de sesión, `Main` muestra los módulos asociados al rol:

| Rol | Módulos incluidos en la navegación principal |
|---|---|
| `ADMINISTRADOR` | Disponibilidad de platos e Inventario. |
| `MESERO` | Mapa de salón. |
| `COCINA` | Tablero de comandas. |
| `CAJERO` | Pago de cuentas. |

La vista `menu-mesero.fxml` y su controlador existen en el repositorio, pero no aparecen registrados en la lista de módulos de `Main` en la versión revisada. Por eso no se documentan como una opción actualmente accesible desde la navegación principal.

## 5. Comunicación y persistencia

La aplicación se conecta directamente a PostgreSQL mediante el driver JDBC. La clase de conexión encontrada es `ConexionBD.ConexionBD`, que utiliza `DriverManager` y entrega objetos `Connection` a servicios, DAO y repositorios.

No se identificaron controladores REST ni rutas HTTP en la arquitectura activa. Las operaciones deben documentarse como métodos de servicios, DAO y repositorios, y no como endpoints web.

## 6. Decisiones y aspectos por revisar

- La arquitectura activa es JavaFX + JDBC + PostgreSQL. No se utiliza Spring Boot ni Spring Data JPA como mecanismo de arranque o persistencia en el `pom.xml` actual.
- `pom.xml` contiene exclusiones de compilación para varios archivos Java. Antes de retirar esas exclusiones, el equipo debe revisar cada clase y comprobar si ya puede incorporarse al build.
- `src/main/resources/application.properties` conserva propiedades con nombres de Spring (`spring.datasource`, `spring.jpa`, `server.port`), pero no forman parte de la configuración de la aplicación JavaFX/JDBC descrita aquí.
- La clase `ConexionBD.ConexionBD` contiene actualmente los datos de conexión como constantes en el código. Conviene externalizar las credenciales y evitar guardar contraseñas reales en el repositorio.
- Hay dos convenciones de acceso a datos (`dao` y `repository`). Ambas usan JDBC; una unificación futura podría reducir duplicidad y facilitar el mantenimiento.

## 7. Archivos de referencia

- `pom.xml`: dependencias, versiones y configuración de Maven.
- `src/main/java/Main.java`: punto de entrada y navegación.
- `src/main/java/ConexionBD/ConexionBD.java`: conexión JDBC.
- `src/main/java/controller/`: controladores de interfaz.
- `src/main/java/service/`: lógica de negocio.
- `src/main/java/repository/` y `src/main/java/dao/`: persistencia.
- `src/main/resources/views/` y `src/main/resources/css/`: interfaz gráfica.
