# GastroFlow — aplicación de escritorio

Proyecto JavaFX + Maven + PostgreSQL con los módulos del sistema. Reúne las historias de
usuario implementadas hasta ahora en un solo proyecto, con un único `pom.xml` y un único
punto de entrada.

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

### HU-045 · HU-046 · HU-049 — Mapa de salón

Las tres historias comparten una sola vista, que dibuja las mesas del restaurante como
botones sobre un panel.

HU-045, estado por color. Cada mesa se pinta según su estado, y la leyenda de la esquina
superior izquierda se arma sola a partir de los estados definidos en el código:

| Estado | Color |
|---|---|
| Disponible | verde `#2E7D32` |
| Ocupada | rojo `#C62828` |
| Reservada | naranja `#EF6C00` |
| Pendiente de pago | azul `#1565C0` |
| Inhabilitada | gris `#616161` |

Los códigos que llegan de la base se normalizan antes de compararlos: se pasan a
mayúsculas y los espacios y guiones se vuelven guiones bajos, así que `LIBRE` y
`DISPONIBLE` caen en el mismo estado, igual que `PENDIENTE_PAGO` y `PENDIENTE_DE_PAGO`, o
`INHABILITADA`, `INHABILITADO` y `MANTENIMIENTO`. Un código que no corresponda a ninguno
se dibuja con un estilo aparte en vez de romper la vista. El mapa se refresca solo cada 5
segundos, reutilizando los botones que ya existen en lugar de volver a construir el panel.

HU-049, filtro por estado. Cuatro botones (`Todas`, `Libres`, `Ocupadas`, `Reservadas`)
dejan ver únicamente las mesas de ese estado. Las que no cumplen el filtro se retiran del
panel y las visibles se reacomodan en la cuadrícula. Si el filtro no deja ninguna mesa, se
muestra un mensaje en lugar de un panel vacío, y la selección sobrevive al cambio de
filtro.

HU-046, detalle de la mesa. Al hacer clic en una mesa, el recuadro de la derecha muestra
número, estado, capacidad máxima, cantidad de comensales, pedido activo, mesero
responsable y código de la mesa. Los campos que no apliquen o no existan en la base se
muestran con un texto por defecto en vez de dejarse en blanco, y el detalle se actualiza
con cada refresco automático mientras la mesa siga seleccionada.

La vista incluye además `Agregar Mesa`, que pide el número por diálogo, y `Quitar Mesa`,
que da de baja la mesa seleccionada marcando `is_active = 0` en lugar de borrar la fila.

Vista: `views/MapaSalon.fxml`
Tablas: `mesas`, `zonas`, `estados_mesa`

---

## Estructura

```text
gastroflow/
├── pom.xml
└── src/
    ├── main/java/
    │   ├── Main.java          Menú principal: abre cada módulo en su ventana.
    │   ├── conf/              Conexión a PostgreSQL del mapa de salón.
    │   ├── controller/        Controladores de las vistas JavaFX.
    │   ├── dao/               Acceso a datos con JDBC.
    │   ├── database/          Conexión a PostgreSQL del resto de módulos.
    │   ├── dto/               Objetos que se pasan entre capas.
    │   ├── entity/            Entidades del dominio.
    │   ├── enums/             Enumeraciones (todavía sin contenido).
    │   ├── exceptions/        Excepciones propias (todavía sin contenido).
    │   ├── repository/        Consultas del mapa de salón.
    │   ├── service/           Reglas de negocio del mapa de salón.
    │   ├── session/           Usuario autenticado.
    │   └── util/              Utilidades sin dependencias.
    ├── main/resources/        FXML y hojas de estilo.
    └── test/java/             Pruebas unitarias.
```

Los paquetes son planos: no hay prefijo `com.gastroflow`, y `Main` queda en el paquete por
defecto, que es lo que declara el `javafx-maven-plugin` como `mainClass`.

Los FXML y las hojas de estilo de los tres primeros módulos están en la raíz de
`resources`, y los del mapa de salón en `resources/views/`. Cada módulo se abre en su
propia ventana con su propia hoja de estilos: las hojas definen reglas sobre `.root`, que
en JavaFX aplica al nodo raíz de la escena, así que cargarlas en una misma escena haría
que una pisara a la otra. El mapa de salón no usa hoja de estilos, los colores van en el
controlador.

---

## Requisitos

- JDK 21
- Maven 3.9+
- PostgreSQL 16 con el esquema de [`db/`](../db/ddl/README.md) ya cargado

---

## Configuración de la base de datos

Conviven dos clases de conexión con variables de entorno distintas. Si la variable no está
definida, se usa el valor por defecto.

`database.ConexionBD`, que usan disponibilidad, menú y caja:

| Variable | Valor por defecto |
|---|---|
| `GASTROFLOW_DB_URL` | `jdbc:postgresql://localhost:5432/gastroflow` |
| `GASTROFLOW_DB_USER` | `postgres` |
| `GASTROFLOW_DB_PASSWORD` | `postgres` |

Esta define además tiempos de espera: 5 segundos para conectar y para el login, y 15 para
cada consulta. Sin ellos, con el servidor caído la ventana se queda congelada varios
minutos esperando el tiempo de espera del sistema operativo.

`conf.ConexionDB`, que usa el mapa de salón:

| Variable | Valor por defecto |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/gastroflow` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | *(vacía)* |

Para correr todos los módulos contra la misma base hay que definir los dos juegos.

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

El `id` debe corresponder a un usuario con `is_active = 1` y rol `CAJERO`. Si se pasa un
valor que no sea un número entero, se avisa por consola y la aplicación abre igual: antes
eso tumbaba el arranque y no se abría ninguna ventana.

---

## Pruebas

```bash
mvn test
```

19 pruebas en tres clases, todas sin base de datos:

| Clase | Pruebas | Qué cubre |
|---|---|---|
| `dto.CuentaPagoTest` | 6 | Cálculo del total con propina, descuento e impuestos. |
| `dto.ItemPedidoTest` | 4 | Líneas de pedido y su subtotal. |
| `util.TextoBusquedaTest` | 9 | Normalización del buscador: tildes, mayúsculas y texto vacío. |

---

## Pendientes

- Pantalla de autenticación real, que reemplace la sesión de prueba por propiedades.
- Unificar `database.ConexionBD` y `conf.ConexionDB` en una sola clase de conexión, con un
  solo juego de variables de entorno.
- Llenar `enums/` y `exceptions/`, o quitarlas si no se van a usar.
- `PagoDAO.validarPagoNoDuplicado` usa `SELECT ... FOR UPDATE`, que no bloquea nada cuando
  la consulta no devuelve filas; la unicidad la garantiza el índice parcial
  `uq_pagos_pedido_pagado` de la base de datos.
- El mapa de salón consulta la base cada 5 segundos desde el hilo de la interfaz. Con
  muchas mesas conviene moverlo a un hilo aparte, como ya se hizo en los otros módulos.
