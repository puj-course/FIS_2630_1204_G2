# GastroFlow

## Descripción

GastroFlow es un sistema de software orientado a la gestión integral de restaurantes. Su objetivo es centralizar en una sola plataforma procesos como el control de inventario, gestión de mesas, registro de pedidos, envío de comandas a cocina, administración de productos, recetas, usuarios y generación de reportes.

El problema que busca solucionar es la falta de organización y trazabilidad que puede presentarse cuando estos procesos se manejan de forma manual o mediante herramientas separadas. Esto puede generar errores en los pedidos, desactualización del inventario, pérdida de información, dificultades en la comunicación entre meseros y cocina, y poca visibilidad sobre la operación del restaurante. El proyecto se plantea como una solución modular y escalable, de manera que puedan incorporarse nuevas funcionalidades a futuro sin afectar la estructura principal del sistema.

---

## Equipo del Proyecto

| Nombre | Rol | GitHub |
|---|---|---|
| Mariana Niño | Scrum Master / Sprint Planner | github.com/mariananvv16 |
| Gabriel Quiroga | Product Owner | github.com/Quirogaaaa |
| Samuel Zeudec | Configuration Manager | github.com/szml-PUJ |
| Julian Parra | QA Lead | github.com/JulianLeal12 |
| Nassin Suz | DevOps Engineer | github.com/zeuznnss |

---

## Tecnologías

| Capa | Tecnología |
|---|---|
| Interfaz | JavaFX 21.0.5 |
| Lógica de aplicación | Java 21, JDBC |
| Base de datos | PostgreSQL 16 (driver 42.7.5) |
| Construcción | Maven |
| Pruebas | JUnit 5.11.4 |
| Control de versiones | Git |

---

## Estructura del repositorio

```text
FIS_2630_1204_G2/
├── pom.xml                    Aplicación de escritorio (JavaFX + Maven)
├── src/
│   ├── main/java/
│   │   ├── Main.java          Menú principal: abre cada módulo en su ventana
│   │   ├── conf/              Conexión a PostgreSQL del módulo de salón
│   │   ├── controller/        Controladores de las vistas JavaFX
│   │   ├── dao/               Acceso a datos con JDBC
│   │   ├── database/          Conexión a PostgreSQL del resto de módulos
│   │   ├── dto/               Objetos que se pasan entre capas
│   │   ├── entity/            Entidades del dominio
│   │   ├── enums/             Enumeraciones (todavía sin contenido)
│   │   ├── exceptions/        Excepciones propias (todavía sin contenido)
│   │   ├── models/            Modelos del módulo de registro
│   │   ├── repositories/      Consultas del módulo de registro
│   │   ├── repository/        Consultas del módulo de salón
│   │   ├── service/           Reglas de negocio del módulo de salón
│   │   ├── services/          Reglas de negocio del módulo de registro
│   │   ├── session/           Usuario autenticado
│   │   └── util/              Utilidades sin dependencias
│   ├── main/resources/        Vistas FXML y hojas de estilo
│   ├── test/java/             Pruebas unitarias con JUnit 5
│   └── tests/                 Repositorios del módulo de datos
│
├── db/                        Base de datos
│   ├── ddl/                   Una tabla por archivo, con su README
│   ├── functions/             Funciones almacenadas
│   └── migrations/            Cambios sobre bases ya creadas
│
├── docs/
│   ├── aplicacion.md          Módulos implementados y cómo ejecutarlos
│   ├── Cliente.md             Documentación por módulo
│   ├── Inventario.md
│   ├── Mesas.md
│   ├── Pedido.md
│   ├── Usuario.md
│   └── database/              Diagrama entidad-relación y documentación del modelo
│
├── conf/                      Configuración común y script de carga de la base
├── BOILERPLATE_template.md    Plantilla de estructura para proyectos del curso
├── LICENSE
└── README.md
```

El proyecto Maven vive en la raíz del repositorio, así que `src/main/java`,
`src/main/resources` y `src/test/java` se detectan sin configuración adicional en el
`pom.xml`. Ninguna clase usa el prefijo `com.gastroflow` y `Main` queda en el paquete por
defecto.

Hoy conviven dos convenciones de paquetes, heredadas de módulos que se desarrollaron por
separado: paquetes planos (`controller`, `entity`, `repository`, `service`) y paquetes
anidados por historia (`models/RegistroCliente`, `repositories/RegistrarPlato`,
`services/ReglasDescuento`). Unificarlas está pendiente.

La documentación detallada está repartida donde corresponde:
[`docs/aplicacion.md`](docs/aplicacion.md) explica los módulos y cómo ejecutarlos, y
[`db/ddl/README.md`](db/ddl/README.md) documenta el esquema, el orden de ejecución y las
convenciones.

---

## Requisitos

- JDK 21
- Maven 3.9 o superior
- PostgreSQL 16

---

## Instalación

### 1. Clonar el repositorio

```bash
git clone https://github.com/puj-course/FIS_2630_1204_G2.git
cd FIS_2630_1204_G2
```

### 2. Crear la base de datos

```bash
createdb -U postgres gastroflow
cd db/ddl
```

Y ejecutar los archivos en el orden que indica [`db/ddl/README.md`](db/ddl/README.md), que
respeta las dependencias entre llaves foráneas.

### 3. Configurar la conexión

Hoy conviven dos clases de conexión, cada una con sus propias variables de entorno. Si la
variable no está definida, se usa el valor por defecto.

`database.ConexionBD`, que usan los módulos de disponibilidad, menú y caja:

| Variable | Valor por defecto |
|---|---|
| `GASTROFLOW_DB_URL` | `jdbc:postgresql://localhost:5432/gastroflow` |
| `GASTROFLOW_DB_USER` | `postgres` |
| `GASTROFLOW_DB_PASSWORD` | `postgres` |

`conf.ConexionDB`, que usa el mapa de salón:

| Variable | Valor por defecto |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/gastroflow` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | *(vacía)* |

Unificar las dos está pendiente. Mientras tanto, para correr todos los módulos contra la
misma base hay que definir los dos juegos de variables.

---

## Ejecución

```bash
mvn clean compile
mvn javafx:run
```

Se abre un menú desde el que se entra a cada módulo.

## Pruebas

```bash
mvn test
```

Son 19 pruebas repartidas en tres clases: `dto.CuentaPagoTest`, `dto.ItemPedidoTest` y
`util.TextoBusquedaTest`. Todas corren sin base de datos.

---

## Estado actual

Implementado:

- HU-04 modelo de datos de ingredientes y unidades de medida
- HU-05 modelo de datos de productos, categorías y precios
- HU-07 modelo de datos de pedidos
- HU-022 movimientos de inventario con trazabilidad por usuario
- HU-28 bloqueo automático de platos agotados
- HU-35 gestión de propinas y descuentos en caja
- HU-39 menú digital por categorías con buscador
- HU-002, HU-003 y HU-006 modelo de datos de clientes, usuarios, roles, mesas y zonas
- HU-021 y HU-023 modelo de datos de detalle de pedido y alertas de inventario
- HU-045 estado de cada mesa diferenciado por color en el mapa de salón
- HU-046 detalle de la mesa seleccionada
- HU-047 identificador único y visible de cada mesa en el mapa
- HU-048 cantidad de personas por mesa, con opción de liberar la mesa
- HU-049 filtro del mapa de salón por estado de mesa
- HU-60 advertencia al superar la capacidad de la mesa al asignar comensales

Pendiente:

- Pantalla de autenticación real: hoy la sesión de caja se pasa por propiedad del sistema
- Registro de pedidos en base de datos desde la vista del mesero
- Módulo de comandas a cocina y reportes
- Unificar `database.ConexionBD` y `conf.ConexionDB` en una sola clase de conexión
- Resolver las clases duplicadas que quedan tras integrar los módulos: `entity/Cliente`
  frente a `models/RegistroCliente/Cliente`, y lo mismo con `Plato`, `EntradaInventario`
  y `ReglaDescuento` y sus repositorios y servicios
- Decidir qué hacer con `src/tests/`, que contiene repositorios y no pruebas

---

## Contexto académico

- Asignatura: Fundamentos de Ingeniería de Software
- Docente: Luis Gabriel Moreno Sandoval, PhD
- Contacto: morenoluis@javeriana.edu.co
- Institución: Pontificia Universidad Javeriana

---

## Contacto

Todos los integrantes son estudiantes de Ingeniería de Sistemas de la Pontificia
Universidad Javeriana.

| Integrante | Correo |
|---|---|
| Gabriel Quiroga | quirogarg@javeriana.edu.co |
| Julian Parra | julians_parra@javeriana.edu.co |
| Samuel Zeudec | malaverl-sz@javeriana.edu.co |
| Nassin Suz | suz.nassinn@javeriana.edu.co |
| Mariana Niño | ninov.mariana@javeriana.edu.co |

---

## Licencia

Proyecto desarrollado con fines académicos. Ver [LICENSE](LICENSE).
