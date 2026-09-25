# Decisión de arquitectura

**Fecha:** 22 de septiembre de 2026  
**Participantes:** _(Samuel Malaver)_  
**Rama de trabajo:** personal → Pull Request hacia `develop`

---

## Decisión

El proyecto **GastroFlow** usará una única arquitectura:

**Aplicación de escritorio JavaFX + acceso a datos con JDBC (PostgreSQL).**

No se utilizará Spring Boot, Spring Data JPA ni Spring Web en esta etapa del proyecto.

---

## Motivación

1. GastroFlow es una **aplicación de escritorio**, no un backend web. El punto de entrada natural es JavaFX (`Main` / vistas `.fxml`).
2. El `pom.xml` del equipo se orientó a JavaFX y al driver PostgreSQL; mantener Spring implicaba una segunda forma de arranque (`SpringApplication`), configuración adicional y mayor complejidad.
3. Existía código residual con anotaciones Spring (`@Entity`, `JpaRepository`, `@RestController`, `@Service`) que **no compilaba** al no haber dependencias ni clase `@SpringBootApplication` de forma estable y alineada con el resto del repo.
4. Unificar bajo **JavaFX + JDBC** elimina el conflicto de dos arquitecturas mezcladas y permite que `mvn clean compile` sea consistente para todo el equipo.
5. La lógica de negocio útil se conserva en capas `entity` (POJOs), `repository` (SQL con JDBC), `service` y `controller` (solo controladores JavaFX).

---

## Stack tecnológico oficial

| Capa | Tecnología |
|------|------------|
| Interfaz | JavaFX 21.0.5 |
| Lenguaje | Java 17+ |
| Acceso a datos | JDBC |
| Base de datos | PostgreSQL |
| Driver | org.postgresql:postgresql 42.7.5 |
| Construcción | Maven |
| Pruebas | JUnit 5 |
| Control de versiones | Git |

---

## Cómo se ejecuta

| Qué | Cómo |
|-----|------|
| Compilar | `mvn clean compile` |
| App de escritorio | `mvn javafx:run` (clase principal: `Main`, cuando exista en la rama) |
| Conexión JDBC | Clase de conexión del proyecto (p. ej. `ConexionDB.ConexionBD` / `conf.ConexionDB`) hacia PostgreSQL |

No existe arranque tipo `mvn spring-boot:run` en esta arquitectura.

---

## Estructura de paquetes (acuerdo del equipo)

Los fuentes viven bajo `src/main/java/` con carpetas de primer nivel alineadas al `package`:

```text
src/main/java/
├── ConexionDB/          # conexión JDBC (si aplica)
├── conf/                # configuración / conexión alternativa
├── controller/          # solo controladores JavaFX
├── entity/              # POJOs (sin JPA)
├── repository/          # acceso a datos con JDBC
├── service/             # lógica de negocio
├── exceptions/
├── dto/
└── ...
