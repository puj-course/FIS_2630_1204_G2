# GastroFlow

Sistema de gestión restaurantera desarrollado como una aplicación de escritorio. GastroFlow reúne en una misma aplicación funcionalidades para administrar mesas, pedidos, comandas, productos, ingredientes, inventario y pagos, con acceso según el rol de cada usuario.

El proyecto busca facilitar la coordinación de las actividades del restaurante y mantener organizada la información necesaria para su operación. En particular, contempla el seguimiento de los pedidos desde su registro hasta la preparación, la consulta de la disponibilidad de los platos y el control de las existencias de ingredientes.

---

## Objetivo del proyecto

Desarrollar un sistema de información que permita gestionar de manera integrada las operaciones principales de un restaurante, facilitando la administración de sus recursos, el seguimiento de los pedidos, el control del inventario y la gestión de pagos mediante una aplicación de escritorio.

## Problema que busca solucionar

La operación de un restaurante requiere coordinar actividades como la atención de las mesas, el registro de pedidos, la preparación de alimentos, el control de ingredientes y el cobro de las cuentas. Cuando estos procesos se manejan de forma manual o mediante herramientas separadas, pueden presentarse errores en los pedidos, dificultades de comunicación entre el personal, información desactualizada sobre el inventario y poca trazabilidad de las operaciones.

GastroFlow busca atender estas dificultades centralizando la información y ofreciendo funcionalidades que apoyan el trabajo de los diferentes roles del restaurante. De esta manera, el personal puede consultar y actualizar la información desde una misma aplicación, de acuerdo con los permisos que tenga asignados.

---

## Equipo del proyecto

| Nombre | Rol | GitHub |
|---|---|---|
| Mariana Niño | Scrum Master / Sprint Planner | [mariananvv16](https://github.com/mariananvv16) |
| Gabriel Quiroga | Product Owner | [Quirogaaaa](https://github.com/Quirogaaaa) |
| Samuel Zeudec | Configuration Manager | [szml-PUJ](https://github.com/szml-PUJ) |
| Julian Parra | QA Lead | [JulianLeal12](https://github.com/JulianLeal12) |
| Nassin Suz | DevOps Engineer | [zeuznnss](https://github.com/zeuznnss) |

---

## Funcionalidades y módulos principales

GastroFlow organiza sus funcionalidades en los siguientes módulos. La disponibilidad de cada opción depende del rol del usuario autenticado.

| Módulo | Descripción general |
|---|---|
| Acceso al sistema | Permite identificar al usuario e iniciar una sesión para acceder a las funcionalidades correspondientes. |
| Mapa de salón y mesas | Presenta las mesas del restaurante, su estado y datos relevantes como capacidad y ocupación. Permite filtrar las mesas por estado y consultar el detalle de una mesa. |
| Menú de mesero | Permite consultar los productos organizados por categorías, buscar por nombre o descripción y filtrar los resultados. |
| Disponibilidad de platos | Comprueba si existen ingredientes suficientes para preparar los productos. Los productos sin existencias suficientes se muestran como agotados y no se pueden agregar a un pedido. |
| Inventario y alertas | Permite consultar ingredientes, existencias y niveles mínimos, así como identificar situaciones de inventario bajo. |
| Cocina y comandas | Permite al personal de cocina consultar las comandas y actualizar su estado durante el proceso de preparación. |
| Pagos | Permite consultar cuentas asociadas a pedidos, revisar sus valores y gestionar descuentos, motivos de descuento y propinas antes del cierre del pago. |

### Roles de usuario

El sistema contempla los siguientes roles principales:

- **Administrador:** supervisa la operación y cuenta con funcionalidades de administración, entre ellas la consulta de inventario y la gestión de disponibilidad de los platos.
- **Mesero:** consulta el mapa de salón y el menú, y participa en la gestión de mesas y pedidos.
- **Personal de cocina:** consulta las comandas y actualiza el estado de preparación de los pedidos.
- **Cajero:** consulta las cuentas y realiza las operaciones relacionadas con descuentos, propinas y cierre de pagos.

Las acciones disponibles para cada rol están determinadas por los permisos y las validaciones implementadas en el sistema.

---

## Tecnologías

| Área | Tecnología | Uso en el proyecto |
|---|---|---|
| Lenguaje | Java 21 | Desarrollo de la lógica de la aplicación. |
| Interfaz de escritorio | JavaFX 21.0.5 | Construcción de las ventanas y controles de la aplicación. |
| Vistas | FXML y CSS | Definición de la estructura visual y los estilos de la interfaz. |
| Acceso a datos | JDBC | Comunicación entre la aplicación y la base de datos. |
| Base de datos | PostgreSQL 16 | Almacenamiento de la información del restaurante. |
| Driver de base de datos | PostgreSQL JDBC 42.7.5 | Conexión de Java con PostgreSQL. |
| Construcción y dependencias | Maven | Compilación y administración de dependencias. |
| Pruebas | JUnit 5.11.4 | Ejecución de pruebas automatizadas. |
| Seguridad de contraseñas | jBCrypt 0.4 | Hash de contraseñas. |
| Control de versiones | Git y GitHub | Seguimiento de cambios y colaboración del equipo. |

GastroFlow utiliza una arquitectura de aplicación de escritorio JavaFX con acceso a datos mediante JDBC y PostgreSQL. No utiliza Spring Boot ni Spring Data JPA como arquitectura de ejecución.

---

## Estructura del repositorio

La estructura principal del repositorio está organizada de la siguiente manera:

```text
FIS_2630_1204_G2/
├── .github/
│   └── workflows/             # Flujos de integración continua
├── .mvn/                      # Configuración del Maven Wrapper
├── conf/                      # Archivos de configuración auxiliares
├── db/
│   ├── ddl/                   # Definición de tablas y esquema de la BD
│   ├── functions/             # Funciones y scripts de datos de prueba
│   └── migrations/            # Migraciones de la base de datos
├── docs/                      # Documentación y diagramas del proyecto
│   ├── database/              # Documentación y modelo de la BD
│   └── diagramas-fuente/      # Fuentes editables de diagramas
├── scripts/                   # Instalación, ejecución y pruebas
├── src/
│   ├── main/
│   │   ├── java/              # Código fuente Java
│   │   │   ├── Main.java      # Punto de entrada de JavaFX
│   │   │   ├── ConexionBD/    # Clases de conexión a la BD
│   │   │   ├── Interface/     # Interfaces utilizadas por la aplicación
│   │   │   ├── controller/    # Controladores de las vistas JavaFX
│   │   │   ├── dao/           # Acceso a datos
│   │   │   ├── dto/           # Objetos de transferencia de datos
│   │   │   ├── entity/        # Entidades del dominio
│   │   │   ├── enums/         # Enumeraciones
│   │   │   ├── exceptions/    # Excepciones propias
│   │   │   ├── metricas/      # Componentes relacionados con métricas
│   │   │   ├── repository/    # Consultas y operaciones de persistencia
│   │   │   ├── scheduler/     # Tareas programadas
│   │   │   ├── service/       # Lógica y reglas de negocio
│   │   │   ├── session/       # Información de sesión del usuario
│   │   │   └── util/          # Clases auxiliares
│   │   └── resources/
│   │       ├── css/           # Hojas de estilo
│   │       ├── static/        # Recursos estáticos
│   │       └── views/         # Vistas FXML
│   └── test/
│       └── java/              # Pruebas automatizadas
├── BOILERPLATE_template.md    # Guía de estructura y convenciones
├── CONTRIBUTING.md            # Guía de colaboración
├── LICENSE                    # Licencia del proyecto
├── README.md                  # Descripción general del proyecto
├── pom.xml                    # Configuración de Maven
├── mvnw                       # Maven Wrapper para Linux/macOS
└── mvnw.cmd                   # Maven Wrapper para Windows
```

La organización del código separa la interfaz, los controladores, la lógica de negocio y el acceso a datos. Las carpetas `dao` y `repository` contienen componentes relacionados con persistencia; su uso concreto depende del módulo. Para una descripción más detallada de la arquitectura y los paquetes, consultar [`docs/architecture.md`](docs/architecture.md) y [`docs/packages.md`](docs/packages.md).

---

## Requisitos

Para compilar y ejecutar el proyecto se requiere:

- JDK 21.
- Maven 3.9 o superior, o utilizar el Maven Wrapper incluido.
- PostgreSQL 16.
- Git para clonar el repositorio.

También se necesita que el servidor de PostgreSQL esté instalado y en ejecución para preparar y utilizar la base de datos.

---

## Configuración de la base de datos

El proyecto incluye scripts SQL y un script de instalación para preparar la base de datos.

Desde la raíz del repositorio, se puede crear el esquema y cargar los datos de prueba con:

```bash
./scripts/setup.sh --con-datos-prueba
```

En Windows, este script puede ejecutarse desde Git Bash, siempre que las herramientas de PostgreSQL (`psql`, `createdb` y `dropdb`) estén disponibles en el `PATH`.

El script utiliza las variables de entorno estándar de PostgreSQL. Si no se definen, emplea `localhost`, el puerto `5432` y el usuario `postgres`. La contraseña se proporciona mediante `PGPASSWORD` cuando sea necesaria.

Para consultar las opciones disponibles:

```bash
./scripts/setup.sh --help
```

La documentación detallada del esquema, las tablas y las migraciones se encuentra en [`db/ddl/README.md`](db/ddl/README.md). La configuración específica de las conexiones de cada módulo está descrita en [`docs/aplicacion.md`](docs/aplicacion.md).

---

## Compilación y ejecución

Clonar el repositorio y ubicarse en su carpeta raíz. Después, compilar el proyecto:

```bash
mvn clean compile
```

Para ejecutar la aplicación JavaFX:

```bash
mvn javafx:run
```

En Windows también se puede utilizar el Maven Wrapper incluido:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd javafx:run
```

Antes de iniciar la aplicación, debe estar creada y configurada la base de datos PostgreSQL. Para los valores de conexión y las variables de entorno requeridas por los módulos, consultar [`docs/aplicacion.md`](docs/aplicacion.md).

---

## Pruebas

El proyecto utiliza JUnit 5 para las pruebas automatizadas. Para ejecutar la suite de pruebas desde la raíz del repositorio:

```bash
mvn test
```

Para generar el informe de resultados de pruebas:

```bash
mvn surefire-report:report-only
```

El proyecto también incluye configuración de JaCoCo para generar información de cobertura durante la ejecución de las pruebas. Los informes generados se almacenan dentro de `target/` y no necesitan versionarse en el repositorio.

---

## Documentación adicional

- [Decisiones de arquitectura](docs/architecture.md)
- [Organización de paquetes](docs/packages.md)
- [Descripción de la aplicación y módulos](docs/aplicacion.md)
- [Documentación de la base de datos](db/ddl/README.md)
- [Plan de pruebas](docs/planDePruebas.md)
- [Diagramas y documentación de arquitectura](docs/)

---

## Contexto académico

- **Asignatura:** Fundamentos de Ingeniería de Software.
- **Docente:** Luis Gabriel Moreno Sandoval, PhD.
- **Institución:** Pontificia Universidad Javeriana.

---

## Contacto

Proyecto desarrollado por estudiantes de Ingeniería de Sistemas de la Pontificia Universidad Javeriana.

| Integrante | Correo |
|---|---|
| Gabriel Quiroga | quirogarg@javeriana.edu.co |
| Julian Parra | julians_parra@javeriana.edu.co |
| Samuel Zeudec | malaverl-sz@javeriana.edu.co |
| Nassin Suz | suz.nassinn@javeriana.edu.co |
| Mariana Niño | ninov.mariana@javeriana.edu.co |

---

## Licencia

Proyecto desarrollado con fines académicos. Consultar el archivo [LICENSE](LICENSE).
