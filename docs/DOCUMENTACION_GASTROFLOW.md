# 📋 Documentación del Proyecto GastroFlow

## 1. Descripción General
**GastroFlow** es un sistema de software orientado a la **gestión integral de restaurantes**. Su objetivo es centralizar en una única plataforma los procesos críticos de operación de un restaurante, incluyendo:

- Control de inventario (ingredientes, movimientos con trazabilidad)
- Gestión de mesas y zonas
- Registro y gestión de pedidos
- Administración de productos y categorías
- Disponibilidad automática de platos (según inventario)
- Gestión de pago, propinas y descuentos
- Menú digital interactivo

**Problema que resuelve:** La falta de organización y trazabilidad que ocurre cuando estos procesos se manejan manualmente o mediante herramientas separadas. Esto genera errores en pedidos, desactualización de inventario, pérdida de información y dificultades en la comunicación entre el personal.

**Solución:** Plataforma modular y escalable que permite incorporar nuevas funcionalidades sin afectar la estructura principal.

---

## 2. Información del Equipo

| Nombre | Rol | GitHub |
|---|---|---|
| Mariana Niño | Scrum Master / Sprint Planner | [@mariananvv16](https://github.com/mariananvv16) |
| Gabriel Quiroga | Product Owner | [@Quirogaaaa](https://github.com/Quirogaaaa) |
| Samuel Zeudec | Configuration Manager | [@szml-PUJ](https://github.com/szml-PUJ) |
| Julian Parra | QA Lead | [@JulianLeal12](https://github.com/JulianLeal12) |
| Nassin Suz | DevOps Engineer | [@zeuznnss](https://github.com/zeuznnss) |

### 📧 Correos de contacto:
- Gabriel Quiroga: `quirogarg@javeriana.edu.co`
- Julian Parra: `julians_parra@javeriana.edu.co`
- Samuel Zeudec: `malaverl-sz@javeriana.edu.co`
- Nassin Suz: `suz.nassinn@javeriana.edu.co`
- Mariana Niño: `ninov.mariana@javeriana.edu.co`

---

## 3. Stack Tecnológico

| Componente | Tecnología |
|---|---|
| **Interfaz (Frontend)** | JavaFX 21 |
| **Lógica de Aplicación** | Java 21 + JDBC |
| **Base de Datos** | PostgreSQL 16 |
| **Construcción & Empaquetado** | Maven 3.9+ |
| **Pruebas** | JUnit 5 |
| **Control de Versiones** | Git |
| **Licencia** | MIT |

---

## 4. Estructura del Repositorio

```text
FIS_2630_1204_G2/
│
├── GastroFlow/                          # Aplicación principal (JavaFX + Maven)
│   ├── pom.xml                          # Configuración Maven con dependencias
│   ├── README.md                        # Documentación específica de módulos
│   │
│   └── src/main/
│       ├── java/com/gastroflow/
│       │   ├── Main.java                # Punto de entrada: menú principal
│       │   ├── controller/              # Controladores de vistas JavaFX
│       │   ├── dao/                     # Data Access Objects (JDBC)
│       │   ├── model/                   # Modelos de dominio
│       │   ├── database/
│       │   │   └── ConexionBD.java      # Conexión a PostgreSQL
│       │   ├── session/
│       │   │   └── SesionUsuario.java   # Gestión de usuario autenticado
│       │   └── util/                    # Utilidades sin dependencias
│       │
│       └── resources/com/gastroflow/    # FXML (vistas) y CSS (estilos)
│           ├── admin-disponibilidad.fxml
│           ├── menu-mesero.fxml
│           ├── cajero-pago-view.fxml
│           ├── hu28.css
│           └── cajero-pago.css
│
├── db/                                  # Base de datos
│   ├── ddl/                             # Definiciones DDL (una tabla por archivo)
│   │   ├── README.md                    # Orden de ejecución y documentación
│   │   ├── roles.ddl
│   │   ├── tipos_documento.ddl
│   │   ├── usuarios.ddl
│   │   ├── clientes.ddl
│   │   ├── zonas.ddl
│   │   ├── estados_mesa.ddl
│   │   ├── mesas.ddl
│   │   ├── detalle_pedido.ddl
│   │   └── alerta_inventario.ddl
│   │
│   ├── functions/                       # Funciones almacenadas en PostgreSQL
│   └── migrations/                      # Scripts de cambios en BD existentes
│
├── app/                                 # Aplicación Node.js de prueba
│   ├── index.js
│   └── package.json
│
├── conf/                                # Configuraciones
├── docs/                                # Documentación adicional
├── scripts/                             # Scripts de utilidad
├── temp/                                # Archivos temporales
│
├── LICENSE                              # Licencia MIT
├── README.md                            # Documentación principal
└── BOILERPLATE_template.md              # Guía de estructura base
```

---

## 5. Arquitectura y Flujo de Funcionamiento

### Punto de Entrada Principal: `Main.java`
La aplicación abre un **menú central** desde el cual se accede a cada módulo en una **ventana independiente**. Esto se implementa así porque cada módulo tiene su propia hoja de estilos CSS que aplica al nodo raíz.

```text
[Menú Principal]
├── Administrador (Disponibilidad de platos)
├── Mesero (Menú y pedidos)
└── Cajero (Pago de cuentas)
```

### Capas de la Aplicación
1. **Capa de Presentación (Views):** Archivos FXML + CSS.
2. **Capa de Controladores:** Controladores que enlazan vistas con lógica.
3. **Capa de Modelos:** Clases de dominio (Usuario, Producto, Pedido, etc.).
4. **Capa de Acceso a Datos (DAO):** JDBC para interactuar con PostgreSQL.
5. **Capa de Base de Datos:** PostgreSQL con funciones almacenadas.

### Gestión de Sesión
La clase `SesionUsuario` mantiene el contexto del usuario autenticado:
```java
SesionUsuario.iniciarSesion(id, nombre, rol);  // Inicia sesión
SesionUsuario.getUsuarioId();                  // Obtiene ID del usuario
SesionUsuario.esCajero();                      // Valida si es cajero
SesionUsuario.cerrarSesion();                  // Cierra sesión
```

### Conexión a Base de Datos
`ConexionBD` lee variables de entorno (con valores por defecto) para conectar a PostgreSQL. Incluye timeouts configurables para evitar que la aplicación se congele.
```properties
GASTROFLOW_DB_URL = jdbc:postgresql://localhost:5432/gastroflow
GASTROFLOW_DB_USER = postgres
GASTROFLOW_DB_PASSWORD = postgres
```

---

## 6. Módulos Implementados

### 🔹 HU-28: Disponibilidad Automática de Platos
- **Descripción:**
  - Valida automáticamente los ingredientes requeridos por cada producto.
  - Cambia el estado a AGOTADO cuando falta al menos un ingrediente.
  - Vuelve a DISPONIBLE cuando hay stock suficiente.
  - Mantiene INACTIVO como decisión administrativa.
- **Características:**
  - El menú del mesero muestra productos agotados y bloquea agregarlos.
  - Validación antes de agregar un producto y confirmar el pedido.
  - Vista de administrador con el estado de los productos y motivo del agotamiento.
- **Vistas:** `admin-disponibilidad.fxml`, `menu-mesero.fxml`

### 🔹 HU-35: Gestión de Propinas y Descuentos
- **Descripción:**
  - Consulta de cuentas por número de pedido.
  - Visualización de subtotal, impuestos, descuentos, propina y total.
- **Características:**
  - Agregar, modificar y eliminar descuentos (con motivo obligatorio).
  - Agregar, modificar y eliminar propinas.
  - Cálculo automático: *subtotal + impuestos + propina - descuento*.
  - Solo un usuario con rol CAJERO puede cerrar el pago.
- **Estado:** El usuario debe estar activo y autenticado.
- **Vista:** `cajero-pago-view.fxml`

### 🔹 HU-39: Menú Digital por Categorías
- **Descripción:**
  - Menú organizado por categorías (en orden definido por el Administrador).
  - Cada producto muestra: nombre, precio, descripción y disponibilidad.
- **Características:**
  - Buscador por nombre y descripción (ignora tildes y mayúsculas).
  - Filtro por categoría.
  - Opción para ocultar productos agotados.
  - Productos agotados no se pueden agregar (ni con búsqueda).
- **Vista:** `menu-mesero.fxml` (compartida con HU-28)

---

## 7. Modelos de Datos Implementados

Según el README de `db/ddl/`, se han implementado los siguientes modelos:

| Historia | Descripción |
|---|---|
| **HU-002** | Clientes del restaurante |
| **HU-003** | Usuarios, roles y tipos de documento |
| **HU-004** | Ingredientes y unidades de medida |
| **HU-005** | Productos, categorías y precios |
| **HU-006** | Mesas, zonas y estados |
| **HU-007** | Pedidos (estructura base) |
| **HU-021** | Detalle de pedidos (ítems, cantidades, precios) |
| **HU-022** | Movimientos de inventario con trazabilidad |
| **HU-023** | Alertas de inventario (stock bajo) |
| **HU-28**  | Disponibilidad automática de platos |
| **HU-35**  | Gestión de propinas y descuentos |
| **HU-39**  | Menú digital por categorías |

---

## 8. Requisitos de Instalación

**Prerrequisitos del Sistema:**
- JDK 21 (Java Development Kit)
- Maven 3.9+
- PostgreSQL 16
- Git

**Variables de Entorno (Opcionales):**
Si no se configuran, el sistema utilizará los valores por defecto:
```bash
export GASTROFLOW_DB_URL=jdbc:postgresql://localhost:5432/gastroflow
export GASTROFLOW_DB_USER=postgres
export GASTROFLOW_DB_PASSWORD=postgres
```

---

## 9. Instalación Paso a Paso

### 1. Clonar el Repositorio
```bash
git clone https://github.com/puj-course/FIS_2630_1204_G2.git
cd FIS_2630_1204_G2
```

### 2. Crear la Base de Datos
```bash
# Crear la base de datos
createdb -U postgres gastroflow

# Navegar a scripts DDL
cd db/ddl

# Ejecutar los archivos en el orden especificado en db/ddl/README.md
# Respetando las dependencias de llaves foráneas:
psql -U postgres -d gastroflow -f roles.ddl
psql -U postgres -d gastroflow -f tipos_documento.ddl
psql -U postgres -d gastroflow -f zonas.ddl
psql -U postgres -d gastroflow -f estados_mesa.ddl
psql -U postgres -d gastroflow -f clientes.ddl
psql -U postgres -d gastroflow -f usuarios.ddl
psql -U postgres -d gastroflow -f mesas.ddl
psql -U postgres -d gastroflow -f detalle_pedido.ddl
psql -U postgres -d gastroflow -f alerta_inventario.ddl
```

### 3. Compilar y Ejecutar
```bash
cd ../../GastroFlow

# Limpiar y compilar
mvn clean compile

# Ejecutar la aplicación
mvn javafx:run
```

**Para Probar el Módulo de Caja (con usuario automático):**
```bash
mvn javafx:run -Dgastroflow.cajeroId=1 -Dgastroflow.cajeroNombre="Ana"
```
> *Nota: El ID debe corresponder a un usuario con `is_active = 1` y rol `CAJERO` en la BD.*

---

## 10. Ejecución de Pruebas

```bash
cd GastroFlow
mvn test
```
**Estado actual:** El proyecto está en desarrollo. Las pruebas unitarias se irán agregando progresivamente en `src/test`.

---

## 11. Configuración del Proyecto Maven

El archivo `GastroFlow/pom.xml` define:

- **Dependencias Principales:**
  - `javafx-controls` y `javafx-fxml` (UI)
  - `postgresql` (Driver JDBC)
  - `junit-jupiter` (Testing)
- **Plugins:**
  - `maven-compiler-plugin` (Java 21)
  - `maven-surefire-plugin` (Test Runner)
  - `javafx-maven-plugin` (Ejecución de JavaFX)
- **Punto de entrada:** `com.gastroflow.Main`

---

## 12. Estado Actual del Proyecto

**✅ Implementado:**
- [x] HU-04: Modelo de datos de ingredientes y unidades de medida
- [x] HU-05: Modelo de datos de productos, categorías y precios
- [x] HU-07: Modelo de datos de pedidos
- [x] HU-022: Movimientos de inventario con trazabilidad por usuario
- [x] HU-28: Bloqueo automático de platos agotados
- [x] HU-35: Gestión de propinas y descuentos en caja
- [x] HU-39: Menú digital por categorías con buscador

**⏳ Pendiente:**
- [ ] Pantalla de autenticación real (actualmente usa sesión por propiedades del sistema)
- [ ] Registro de pedidos en BD desde la vista del mesero
- [ ] Módulo de comandas a cocina y reportes
- [ ] Suite completa de pruebas unitarias
- [ ] Validación con `SELECT ... FOR UPDATE` (actualmente garantizada por índice)

---

## 13. Contexto Académico

- **Asignatura:** Fundamentos de Ingeniería de Software (FIS)
- **Docente:** Luis Gabriel Moreno Sandoval, PhD
- **Contacto Docente:** `morenoluis@javeriana.edu.co`
- **Institución:** Pontificia Universidad Javeriana
- **Propósito:** Proyecto de curso / práctica académica

---

## 14. Convenciones del Código

- **Nombres de Restricciones de BD:**
  - `pk_` → Primary Key
  - `fk_` → Foreign Key
  - `ck_` → Check
  - `uq_` → Unique
- **Nombres de Índices:**
  - `ix_<tabla>_<columna>`
- **Auditoría en Tablas Maestras:**
  - Columna `created_at` (fecha de creación)
  - Columna `updated_at` (fecha de última actualización)
- **Llaves Primarias:**
  - Tipo: `BIGINT GENERATED BY DEFAULT AS IDENTITY`

---

## 15. Cómo Contribuir

1. Crear una rama desde `develop`.
2. Realizar cambios y commits descriptivos.
3. Hacer push a la rama.
4. Crear un Pull Request hacia `develop`.
5. Esperar la revisión del equipo.

> **Rama Principal:** `develop` es la rama principal de integración y desarrollo.

---

## 16. Licencia

Proyecto distribuido bajo **Licencia MIT**. Consultar el archivo `LICENSE` en el repositorio para más detalles.

---

## 17. Recursos Adicionales

- `README.md` (raíz): Descripción general del proyecto.
- `GastroFlow/README.md`: Documentación de módulos y ejecución.
- `db/ddl/README.md`: Documentación del esquema de BD y orden de ejecución.
- `BOILERPLATE_template.md`: Guía de estructura base del repositorio.

---

## 18. Preguntas Frecuentes

**¿Cómo accedo a cada módulo?**
Ejecuta `mvn javafx:run` en la carpeta `GastroFlow/`. Se abrirá un menú principal con tres opciones: Administrador, Mesero y Cajero.

**¿Qué pasa si no tengo PostgreSQL ejecutándose?**
La aplicación lanzará una excepción de conexión. Asegúrate de que el servicio de PostgreSQL esté activo y de que la base de datos `gastroflow` exista.

**¿Puedo modificar las credenciales de BD?**
Sí, puedes inyectarlas usando variables de entorno o modificándolas directamente en la clase `ConexionBD.java`.

**¿Cómo inicio sesión en el módulo Caja?**
Usa la propiedad del sistema al compilar/ejecutar:
`mvn javafx:run -Dgastroflow.cajeroId=1 -Dgastroflow.cajeroNombre="Ana"`

**¿Dónde encuentro la estructura de la base de datos?**
En la carpeta `db/ddl/`. Allí encontrarás un archivo DDL por cada tabla y un `README.md` con el orden exacto de ejecución.

**¿Qué lenguaje principal usa el proyecto?**
Java 21 con JavaFX 21 para la interfaz gráfica y PostgreSQL 16 para la persistencia de datos.

**¿Cómo se estructura el código fuente?**
Está diseñado bajo un patrón de arquitectura en capas (presentación, controladores, modelos, DAOs y base de datos).

---

## 19. Resumen Técnico

**GastroFlow** es una aplicación de escritorio modular que implementa un sistema de gestión de restaurantes. Utiliza una arquitectura en capas con una separación clara entre la capa de presentación (JavaFX), la lógica de negocio (controladores y modelos) y el acceso a datos (JDBC). La base de datos PostgreSQL centraliza toda la información de la operación del restaurante, garantizando la integridad referencial y facilitando la auditoría de procesos. El proyecto está construido bajo los estándares académicos de ingeniería de software exigidos, incorporando control de versiones con Git, documentación técnica clara, pruebas unitarias integradas con JUnit y automatización de la construcción mediante Maven.
