# Diagrama de despliegue

Documenta cómo se instala y se ejecuta el sistema hoy, y qué hace falta para ponerlo en funcionamiento.

---

## Cómo se instala y se ejecuta el sistema hoy

![Cómo se instala y se ejecuta el sistema hoy](Imagenes/DiagramaDespliegue_Entorno.png)

**Por qué está.** Responde qué hace falta para poner GastroFlow en funcionamiento. No dibuja servidores de aplicaciones porque no existen: el pom.xml no declara ningún plugin de empaquetado, así que el sistema no produce un instalable. Lo que sí existe, y es lo que documenta, son el entorno de desarrollo y el de integración continua.

**Qué muestra.** Son 6 nodos y 15 artefactos. Para ejecutarlo hacen falta JDK 21, Maven 3.9 o superior y PostgreSQL 16, con las dependencias que declara el pom (JavaFX 21.0.5, el driver postgresql 42.7.5 y jbcrypt 0.4). La base se prepara con los scripts de db/ y el arranque es mvn clean compile seguido de mvn javafx:run, contra la base en localhost:5432. El nodo de integración continua ejecuta los pasos declarados en .github/workflows/ci.yml.
