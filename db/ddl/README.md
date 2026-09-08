# Esquema de base de datos — GastroFlow

Definiciones DDL de las tablas del sistema, en PostgreSQL. Cada archivo contiene una
sola sentencia `CREATE TABLE` con sus llaves, restricciones e índices.

Las funciones almacenadas están en `../functions/` y los cambios sobre bases ya
creadas en `../migrations/`.

---

## Contenido

### Catálogos

| Archivo | Tabla | Historia / Issue | Descripción |
|---|---|---|---|
| `roles.ddl` | `roles` | HU-003 (#22) | Catálogo de roles del sistema (Administrador, Mesero, Cocinero, Cajero). |
| `tipos_documento.ddl` | `tipos_documento` | HU-003 (#22) | Tipos de documento de identidad (CC, CE, Pasaporte, NIT). |
| `zonas.ddl` | `zonas` | HU-006 (#25) | Áreas del restaurante (Terraza, Salón Principal, Bar). |
| `estados_mesa.ddl` | `estados_mesa` | HU-006 (#25) | Estados de las mesas (DISPONIBLE, OCUPADA, RESERVADA, MANTENIMIENTO). |
| `unidades_medida.ddl` | `unidades_medida` | HU-04 (#23) | Unidades de medida (kilogramo, litro, unidad) con código, abreviatura y tipo. |
| `categorias.ddl` | `categorias` | HU-05 (#24) · HU-39 (#72) | Categorías del menú, con su orden de presentación. |

### Personas y espacio

| Archivo | Tabla | Historia / Issue | Descripción |
|---|---|---|---|
| `usuarios.ddl` | `usuarios` | HU-003 (#22) | Empleados del sistema con credenciales, rol y estado de turno. |
| `clientes.ddl` | `clientes` | HU-002 (#21) | Clientes del restaurante, para facturación y seguimiento. |
| `mesas.ddl` | `mesas` | HU-006 (#25) | Mesas del local: número, capacidad, zona y estado actual. |

### Catálogo del menú e inventario

| Archivo | Tabla | Historia / Issue | Descripción |
|---|---|---|---|
| `ingredientes.ddl` | `ingredientes` | HU-04 (#23) | Insumos: costo, stock actual/mínimo/máximo, proveedor y vencimiento. |
| `productos.ddl` | `productos` | HU-05 (#24) | Platos y bebidas: precio vigente, receta (`ingredientes` JSONB) y estado. |
| `precios_producto.ddl` | `precios_producto` | HU-05 (#24) | Historial de precios. `productos.precio_venta` es el precio vigente. |
| `movimientos_inventario.ddl` | `movimientos_inventario` | HU-022 (#45) | Kardex de entradas y salidas, con el usuario responsable de cada movimiento. |
| `alerta_inventario.ddl` | `alerta_inventario` | HU-023 (#48) | Avisos de insumos por debajo del stock mínimo. |

### Operación

| Archivo | Tabla | Historia / Issue | Descripción |
|---|---|---|---|
| `pedidos.ddl` | `pedidos` | HU-07 (#90) | Pedidos: mesa, mesero, líneas (`productos` JSONB), estado, totales y propina. |
| `detalle_pedido.ddl` | `detalle_pedido` | HU-021 (#47) | Desglose de ítems, cantidades y precios de un pedido. |
| `pagos.ddl` | `pagos` | HU-35 (#74) | Cobros: subtotal, descuento con motivo, impuestos, propina y total pagado. |

---

## Orden de ejecución

El orden importa: hay llaves foráneas entre las tablas.

```
 1. roles.ddl
 2. tipos_documento.ddl
 3. zonas.ddl
 4. estados_mesa.ddl
 5. unidades_medida.ddl
 6. categorias.ddl
 7. usuarios.ddl                  ← depende de roles y tipos_documento
 8. clientes.ddl                  ← depende de tipos_documento y usuarios
 9. mesas.ddl                     ← depende de zonas y estados_mesa
10. ingredientes.ddl              ← depende de unidades_medida
11. productos.ddl                 ← depende de categorias
12. precios_producto.ddl          ← depende de productos
13. pedidos.ddl                   ← depende de clientes, mesas y usuarios
14. movimientos_inventario.ddl    ← depende de productos, ingredientes y usuarios
15. pagos.ddl                     ← depende de pedidos y usuarios
16. detalle_pedido.ddl            ← depende de pedidos y productos
17. alerta_inventario.ddl         ← depende de ingredientes y productos
18. ../functions/hu28_disponibilidad.sql
```

`usuarios` va **antes** de `clientes`: `clientes.id_usuario_cuenta` tiene llave
foránea contra `usuarios.id_usuario`.

---

## Ejecución

```bash
createdb gastroflow

for f in roles tipos_documento zonas estados_mesa unidades_medida categorias \
         usuarios clientes mesas ingredientes productos precios_producto \
         pedidos movimientos_inventario pagos detalle_pedido alerta_inventario; do
  psql -d gastroflow -v ON_ERROR_STOP=1 -f "$f.ddl"
done

psql -d gastroflow -v ON_ERROR_STOP=1 -f ../functions/hu28_disponibilidad.sql
psql -d gastroflow -v ON_ERROR_STOP=1 -f ../seed.sql
```

`../seed.sql` carga datos de prueba para demostrar HU-28, HU-35 y HU-39. Es
idempotente y resuelve todo por llave natural, así que se puede correr varias
veces y también sobre una base que ya tenga datos.

Sobre una base **ya creada**, aplicar en cambio los scripts de `../migrations/` en
orden numérico. Los cuatro son idempotentes: correrlos dos veces no duplica datos ni
falla.

Para verificar:

```bash
psql -d gastroflow -c '\dt'
psql -d gastroflow -c '\df fn_*'
```

---

## Funciones almacenadas

`../functions/hu28_disponibilidad.sql` define las cinco funciones que invoca
`DisponibilidadDAO`:

| Función | Devuelve | Uso |
|---|---|---|
| `fn_recalcular_estados_productos()` | `INTEGER` | Recalcula todo el catálogo; devuelve cuántos productos cambiaron de estado. |
| `fn_recalcular_estado_producto(producto_id)` | `VARCHAR` | Recalcula un producto y devuelve su estado resultante. |
| `fn_producto_disponible_cantidad(producto_id, cantidad)` | `BOOLEAN` | ¿Alcanza el inventario para N unidades? |
| `fn_motivo_receta(receta)` | `TEXT` | Nombres de los ingredientes faltantes, o `NULL` si no falta ninguno. |
| `fn_pedido_faltantes(lineas)` | `TABLE(faltante, requerido, disponible)` | Valida el pedido **completo**: suma el consumo de todas sus líneas por ingrediente. |

Formato esperado de `productos.ingredientes`, donde la cantidad corresponde a **una**
unidad del producto:

```json
[
  {"ingrediente_id": 1, "cantidad": 2},
  {"ingrediente_id": 2, "cantidad": 0.250}
]
```

Un producto con receta `NULL` o `[]` no depende del inventario de ingredientes y se
evalúa por su propio `productos.stock_actual`.

---

## Convenciones

Hoy conviven dos convenciones en el esquema, según la historia de la que salió cada
tabla. Está pendiente unificarlas.

| | Catálogo, inventario y operación | Personas, espacio y mesas |
|---|---|---|
| Llave primaria | `BIGINT GENERATED BY DEFAULT AS IDENTITY` | `INTEGER GENERATED ALWAYS AS IDENTITY` |
| Nombre de la llave | `<tabla>_id` (`producto_id`) | `id_<tabla>` (`id_mesa`) |
| Estado del registro | `estado VARCHAR` con `CHECK` | `is_active SMALLINT` con `CHECK (0,1)` |

Comunes a todo el esquema:

- **Dinero:** `NUMERIC(12,2)`.
- **Cantidades de inventario:** `NUMERIC(12,4)`, tanto en stock como en movimientos.
- **Estados como cadenas en mayúsculas** validadas con `CHECK`:
  - `unidades_medida.estado`, `categorias.estado`, `ingredientes.estado`: `ACTIVO`, `INACTIVO`
  - `productos.estado`: `DISPONIBLE`, `AGOTADO`, `INACTIVO`
  - `pedidos.estado`: `PENDIENTE`, `ASIGNADO_MESA`, `EN_PREPARACION`, `COMPLETADO`, `ENTREGADO`, `CANCELADO`
  - `movimientos_inventario.tipo_movimiento`: `ENTRADA`, `SALIDA`
  - `pagos.estado`: `PAGADO`, `ANULADO`
- **Auditoría:** `fecha_creacion`/`fecha_actualizacion` o `created_at`/`updated_at` en las tablas maestras.
- **Nombres:** `pk_`, `uq_`/`uk_`, `fk_`, `ck_`/`chk_` para restricciones; `ix_` para índices.

---

## Pendientes conocidos

- Unificar las dos convenciones de la tabla de arriba, incluida la de nombres de
  restricciones (`uq_`/`uk_`, `ck_`/`chk_`).
- Definir trigger para que `fecha_actualizacion` y `updated_at` se actualicen solas
  en cada `UPDATE`.
- Los `CHECK` sobre columnas JSONB (`ck_producto_ingredientes_json`,
  `ck_pedido_productos_json`) no validan nada: `jsonb_typeof()` nunca devuelve `NULL`
  para un JSONB no nulo. Deberían comparar contra `'array'`.
- **Decidir entre `pedidos.productos` (JSONB) y `detalle_pedido`**: hoy son dos
  fuentes de verdad para lo mismo. Si gana `detalle_pedido`, hay que reescribir
  `fn_pedido_faltantes` y las consultas de `DisponibilidadDAO`.
- En JSONB no hay integridad referencial: las recetas de `productos.ingredientes` no
  validan contra `ingredientes`.
- Falta un `CHECK` de coherencia de totales en `pedidos`, equivalente al que ya
  tiene `pagos`.
