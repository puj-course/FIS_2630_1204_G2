# GastroFlow — aplicación de escritorio

Proyecto JavaFX + Maven + PostgreSQL con los módulos del sistema. Reúne las historias
de usuario implementadas hasta ahora en un solo proyecto, con un único `pom.xml` y un
único punto de entrada.

---

## Módulos

### HU-28 — Disponibilidad automática de platos

- Valida automáticamente los ingredientes requeridos por cada producto.
- Cambia el estado del producto a `AGOTADO` cuando falta al menos un ingrediente.
- Vuelve a `DISPONIBLE` cuando los ingredientes tienen stock suficiente.
- Mantiene `INACTIVO` como decisión administrativa: no lo modifica automáticamente.
- El menú del mesero muestra los productos agotados y bloquea agregarlos al pedido.
- Valida la disponibilidad antes de agregar un producto y antes de confirmar el pedido.
- Vista de administrador con el estado de cada producto y el motivo del agotamiento.

Vistas: `admin-disponibilidad.fxml`, `menu-mesero.fxml`

### HU-35 — Gestión de propinas y descuentos

- Consulta de cuentas por número de pedido.
- Visualización de subtotal, impuestos, descuentos, propina y total.
- Agregar, modificar y eliminar descuentos, con motivo obligatorio.
- Agregar, modificar y eliminar propinas.
- Cálculo automático del total: `subtotal + impuestos + propina - descuento`.
- Solo un usuario activo con rol `CAJERO` puede cerrar un pago.

Vista: `cajero-pago-view.fxml`

### HU-39 — Menú digital por categorías

- Menú organizado por categorías, en el orden que define el Administrador.
- Cada producto muestra nombre, precio, descripción y disponibilidad.
- Buscador por nombre y descripción, que ignora tildes y mayúsculas.
- Filtro por categoría y casilla para ocultar los productos agotados.
- Los productos agotados nunca se pueden agregar, ni siquiera encontrándolos con el buscador.

Vista: `menu-mesero.fxml` (compartida con HU-28)

---

## Estructura

```text
GastroFlow/
├── pom.xml
└── src/main/
    ├── java/com/gastroflow/
    │   ├── Main.java                 Menú principal: abre cada módulo en su ventana.
    │   ├── controller/               Controladores de las vistas JavaFX.
    │   ├── dao/                      Acceso a datos (JDBC).
    │   ├── model/                    Modelos de dominio.
    │   ├── database/ConexionBD.java  Conexión a PostgreSQL.
    │   └── session/SesionUsuario.java Usuario autenticado.
    └── resources/com/gastroflow/     FXML y hojas de estilo.
```

Cada módulo se abre en su propia ventana con su propia hoja de estilos. Las dos hojas
definen reglas sobre `.root`, que en JavaFX aplica al nodo raíz de la escena, así que
cargarlas en una misma escena haría que una pisara a la otra.

---

## Requisitos

- JDK 21
- Maven 3.9+
- PostgreSQL 16 con el esquema de [`db/`](../db/ddl/README.md) ya cargado

---

## Configuración de la base de datos

`ConexionBD` lee variables de entorno y usa estos valores por defecto:

| Variable | Valor por defecto |
|---|---|
| `GASTROFLOW_DB_URL` | `jdbc:postgresql://localhost:5432/gastroflow` |
| `GASTROFLOW_DB_USER` | `postgres` |
| `GASTROFLOW_DB_PASSWORD` | `postgres` |

---

## Ejecución

```bash
mvn clean compile
mvn javafx:run
```

El módulo de Caja exige un usuario autenticado con rol `CAJERO`. Para probarlo sin
pantalla de login:

```bash
mvn javafx:run -Dgastroflow.cajeroId=1 -Dgastroflow.cajeroNombre="Ana"
```

El `id` debe corresponder a un usuario con `is_active = 1` y rol `CAJERO`.

---

## Pendientes

- Pantalla de autenticación real, que reemplace la sesión de prueba por propiedades.
- Pruebas unitarias: el proyecto todavía no tiene `src/test`.
- `PagoDAO.validarPagoNoDuplicado` usa `SELECT ... FOR UPDATE`, que no bloquea nada
  cuando la consulta no devuelve filas; la unicidad la garantiza el índice parcial
  `uq_pagos_pedido_pagado` de la base de datos.
