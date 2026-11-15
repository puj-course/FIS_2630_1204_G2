# Configuración y ejecución de GastroFlow

## 1. Requisitos

De acuerdo con `pom.xml` y `scripts/setup.sh`, se requieren:

- JDK 21.
- Maven 3.9 o superior.
- PostgreSQL 16.
- Cliente y utilidades de PostgreSQL disponibles en el PATH: `psql`, `createdb` y `dropdb`.
- Git para clonar y actualizar el repositorio.

En Windows, `scripts/setup.sh` está preparado para ejecutarse desde Git Bash.

## 2. Obtener el proyecto

Clonar el repositorio y ubicarse en la carpeta del proyecto:

```bash
git clone <URL_DEL_REPOSITORIO>
cd <CARPETA_DEL_PROYECTO>
```

Reemplazar los marcadores por la URL y el nombre reales del repositorio.

## 3. Preparar PostgreSQL

Asegurarse de que el servidor PostgreSQL esté iniciado y que el usuario configurado tenga permisos para crear bases de datos.

El script de instalación utiliza estas variables de entorno:

| Variable | Valor por defecto |
|---|---|
| `PGHOST` | `localhost` |
| `PGPORT` | `5432` |
| `PGUSER` | `postgres` |
| `PGPASSWORD` | Sin valor predeterminado; se utiliza si está definida. |

Crear el esquema:

```bash
./scripts/setup.sh
```

Crear el esquema e insertar datos de prueba:

```bash
./scripts/setup.sh --con-datos-prueba
```

Recrear la base desde cero con datos de prueba:

```bash
./scripts/setup.sh --recrear --con-datos-prueba
```

**Advertencia:** `--recrear` elimina la base de datos seleccionada antes de crearla nuevamente. Utilizarlo únicamente cuando se puedan perder los datos existentes.

El script aplica los DDL, las funciones almacenadas y las migraciones en el orden definido en `scripts/setup.sh`. Se detiene ante el primer error.

## 4. Configurar la conexión de la aplicación

La conexión JDBC de la aplicación está definida en `src/main/java/ConexionBD/ConexionBD.java`. En la versión revisada, la URL, el usuario y la contraseña están declarados como constantes en esa clase.

Verificar que los valores correspondan a la instancia local de PostgreSQL y a la base `gastroflow`. La configuración `PG*` del script de instalación no reemplaza automáticamente los valores de `ConexionBD.java`.

No guardar contraseñas personales o de producción en Git. La externalización de estas credenciales es una mejora técnica recomendada.

## 5. Compilar y ejecutar

Desde la raíz del proyecto:

```bash
mvn clean compile
mvn javafx:run
```

El plugin de JavaFX utiliza `Main` como clase principal, según la configuración del `pom.xml`.

## 6. Ejecutar pruebas

```bash
mvn test
```

Para generar el informe HTML de Surefire después de ejecutar las pruebas:

```bash
mvn surefire-report:report-only
```

El informe se configura para generarse en `target/site/surefire-report.html`. La cobertura se configura mediante JaCoCo en `target/site/jacoco/`.

## 7. Solución de problemas

- **No se encuentra `psql`, `createdb` o `dropdb`:** instalar PostgreSQL y agregar su carpeta `bin` al PATH; en Windows, abrir Git Bash después de actualizar el PATH.
- **No se puede conectar a PostgreSQL:** verificar que el servicio esté iniciado, el puerto sea correcto y las credenciales de `PGUSER`/`PGPASSWORD` sean válidas.
- **La aplicación no conecta aunque el script sí:** revisar también `ConexionBD.java`; el programa Java utiliza sus propias constantes JDBC.
- **Falla la compilación:** comprobar que se utiliza JDK 21 y revisar el error de Maven. El `pom.xml` contiene exclusiones de algunos archivos fuente; no retirarlas sin revisar su compatibilidad con el build actual.

## 8. Scripts del repositorio

`setup.sh` es el script de preparación de base de datos documentado en esta guía. En la copia revisada, `start.sh`, `deploy.sh` y `test.sh` no contienen una implementación ejecutable, por lo que se deben utilizar los comandos Maven anteriores en lugar de depender de esos archivos.
