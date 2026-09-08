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
| Interfaz | JavaFX 17 |
| Lógica de aplicación | Java 17, JDBC |
| Base de datos | PostgreSQL 16 |
| Construcción | Maven |
| Pruebas | JUnit 5 |
| Control de versiones | Git |

---

## Estructura del repositorio

```text
FIS_2630_1204_G2/
├── conf/                      configuracion para generar base de datos
├── db/                        Base de datos
│   ├── ddl/                   Una tabla por archivo, con su README
│   ├── functions/             Funciones almacenadas
│   └── migrations/            Cambios sobre bases ya creadas
├── docs/                      documentacion del proyecto
├── GastroFlow/                Aplicación de escritorio (JavaFX + Maven)
│   ├── pom.xml
│   ├── README.md              Módulos implementados y cómo ejecutarlos
│   ├── src/
│       ├── main/
│       │   ├── Main.java              Menú principal: abre cada módulo
│       │   ├── controller/            Controladores de las vistas
│       │   ├── dao/                   Acceso a datos con JDBC
│       │   ├── database/              Conexión a PostgreSQL
│       │   ├── dto/                   transporte de datos atraves de la aplicacion
│       │   ├── entity/                entidades de dominio
│       │   ├── enums/                 enumeradores de estados
│       │   ├── exceptions/            excepciones del proyecto
│       │   ├── repository/            repositorios de entidades
│       │   ├── service/               servicios del proyecto
│       │   ├── session/               Usuario autenticado
│       │   └── util/                  Utilidades sin dependencias
│       ├── resources/   Vistas FXML y hojas de estilo
│       └── test/        Pruebas unitarias
├── BOILERPLATE_template.md
├── LICENSE
└── README.md
```

La documentación detallada está repartida donde corresponde: [`GastroFlow/README.md`](GastroFlow/README.md) explica los módulos y cómo ejecutarlos, y [`db/ddl/README.md`](db/ddl/README.md) documenta el esquema, el orden de ejecución y las convenciones.

---

## Requisitos

- JDK 17
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

Y ejecutar los archivos en el orden que indica [`db/ddl/README.md`](db/ddl/README.md), que respeta las dependencias entre llaves foráneas.

### 3. Configurar la conexión

La aplicación lee estas variables de entorno; si no están, usa los valores por defecto:

| Variable | Valor por defecto |
|---|---|
| `GASTROFLOW_DB_URL` | `jdbc:postgresql://localhost:5432/gastroflow` |
| `GASTROFLOW_DB_USER` | `postgres` |
| `GASTROFLOW_DB_PASSWORD` | `postgres` |

---

## Ejecución

```bash
cd GastroFlow
mvn clean compile
mvn javafx:run
```

Se abre un menú desde el que se entra a cada módulo.

## Pruebas

```bash
cd GastroFlow
mvn test
```

---

## Estado actual

Implementado:

- **HU-04** modelo de datos de ingredientes y unidades de medida
- **HU-05** modelo de datos de productos, categorías y precios
- **HU-07** modelo de datos de pedidos
- **HU-022** movimientos de inventario con trazabilidad por usuario
- **HU-28** bloqueo automático de platos agotados
- **HU-35** gestión de propinas y descuentos en caja
- **HU-39** menú digital por categorías con buscador

Pendiente:

- Pantalla de autenticación real (hoy la sesión de caja se pasa por propiedad del sistema)
- Registro de pedidos en base de datos desde la vista del mesero
- Módulo de comandas a cocina y reportes

---

## Contexto académico

- **Asignatura:** Fundamentos de Ingeniería de Software
- **Docente:** Luis Gabriel Moreno Sandoval, PhD
- **Contacto:** morenoluis@javeriana.edu.co
- **Institución:** Pontificia Universidad Javeriana

---

## Contacto

Todos los integrantes son estudiantes de Ingeniería de Sistemas de la Pontificia Universidad Javeriana.

| Integrante | Correo |
|---|---|
| Gabriel Quiroga | quirogarg@javeriana.edu.co |
| Julian Parra | julians_parra@javeriana.edu.co |
| Samuel Zeudec | malaverl-sz@javeriana.edu.co |
| Nassin Suz | suz.nassinn@javeriana.edu.co |
| Mariana Niño | ninov.mariana@javeriana.edu.co |
