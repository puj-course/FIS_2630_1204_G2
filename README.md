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
├── .github/
│   ├── workflows/
│   │   └── ci.yml
│   └── pull_request_template.md
├── .mvn/
│   └── wrapper/
├── db/
│   ├── ddl/
│   ├── functions/
│   └── migrations/
├── docs/
│   ├── Imagenes/
│   ├── database/
│   └── pruebas/
├── scripts/
│   └── setup.sh
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── ConexionBD/
│   │   │   ├── controller/
│   │   │   ├── dao/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── enums/
│   │   │   ├── exceptions/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── session/
│   │   │   ├── util/
│   │   │   └── Main.java
│   │   └── resources/
│   │       ├── css/
│   │       └── views/
│   └── test/
│       └── java/
│           ├── dto/
│           ├── repository/
│           └── util/
├── .gitattributes
├── .gitignore
├── BOILERPLATE_template.md
├── CONTRIBUTING.md
├── LICENSE
├── README.md
├── mvnw
├── mvnw.cmd
└── pom.xml
```
BOILERPLATE_template.md documenta la estructura del repositorio: qué carpetas existen, qué contiene cada una y qué responsabilidad cumple. Describe la organización en capas del código fuente, el contenido de db con las definiciones de tablas, migraciones y datos de prueba, la documentación en docs, los scripts de instalación y los archivos de configuración de la raíz.

---

## Requisitos

- JDK 21
- Maven 3.9 o superior
- PostgreSQL 16

---

## Ejecución

```bash
mvn clean compile
mvn javafx:run
```
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
