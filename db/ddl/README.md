# Esquema de base de datos — GastroFlow

Definiciones DDL de las tablas del sistema, en PostgreSQL. Cada archivo contiene una
sola sentencia `CREATE TABLE` con sus llaves, restricciones e índices.

Las funciones almacenadas están en `../functions/` y los cambios sobre bases ya
creadas en `../migrations/`.

---

## Contenido

| Archivo | Tabla | Historia | Descripción |
|---|---|---|---|
| `unidades_medida.ddl` | `unidades_medida` | HU-04 | Catálogo de unidades (kilogramo, litro, unidad…) con código, abreviatura y tipo. |
| `ingredientes.ddl` | `ingredientes` | HU-04 | Insumos del inventario: costo, stock actual/mínimo/máximo, proveedor y vencimiento. |
| `categorias.ddl` | `categorias` | HU-05 · HU-39 | Categorías del menú que administra el restaurante, con su orden de presentación. |
| `productos.ddl` | `productos` | HU-05 | Catálogo de platos y bebidas: precio vigente, receta (`ingredientes` JSONB) y estado. |
| `precios_producto.ddl` | `precios_producto` | HU-05 | Historial de precios. `productos.precio_venta` sigue siendo el precio vigente. |
| `pedidos.ddl` | `pedidos` | HU-07 | Pedidos: mesa, mesero, líneas (`productos` JSONB), estado, totales y propina. |
| `movimientos_inventario.ddl` | `movimientos_inventario` | HU-022 | Kardex de entradas y salidas, con el usuario responsable de cada movimiento. |
| `pagos.ddl` | `pagos` | HU-35 | Cobros: subtotal, descuento con motivo, impuestos, propina y total pagado. |
| `roles.ddl` | `roles` | HU-003 (#22) | Catálogo de roles del sistema (Administrador, Mesero, Cocinero…). |
| `tipos_documento.ddl` | `tipos_documento` | HU-003 (#22) | Catálogo de tipos de documento de identidad (CC, CE, Pasaporte, NIT). |
| `usuarios.ddl` | `usuarios` | HU-003 (#22) | Empleados y usuarios del sistema con credenciales e información de contacto. |
| `clientes.ddl` | `clientes` | HU-002 (#21) | Clientes del restaurante, para facturación y seguimiento. |
| `zonas.ddl` | `zonas` | HU-006 (#25) | Catálogo de áreas del restaurante (Terraza, Salón Principal, Bar). |
| `estados_mesa.ddl` | `estados_mesa` | HU-006 (#25) | Catálogo de estados de mesa (DISPONIBLE, OCUPADA, RESERVADA, MANTENIMIENTO). |
| `mesas.ddl` | `mesas` | HU-006 (#25) · HU-047 · HU-048 · HU-60 | Mesas del restaurante: número, código, capacidad, zona y estado. |
| `detalle_pedido.ddl` | `detalle_pedido` | HU-021 (#47) | Desglose de ítems, cantidades y precios de cada pedido. |
| `alerta_inventario.ddl` | `alerta_inventario` | HU-023 (#48) | Avisos de insumos por debajo del stock mínimo. |
| `historial_estado_mesa.ddl`    | `historial_estado_mesa`    | HU-059                                 | Historial de cambios de estado de cada mesa (estado anterior/nuevo y motivo).         |
| `nota_mesa.ddl` | `nota_mesa` | HU-058 | Notas que el mesero deja sobre una mesa. |
| `reservas.ddl` | `reservas` | HU-055 | Reservas de mesa, con fecha de cancelación para auditoría. |
| `adicional.ddl` | `adicional` | HU-115 | Adicionales que se suman a una línea del pedido (extra de queso, porción aparte). |
| `asignacion_mesa.ddl` | `asignacion_mesa` | HU-115 | Qué mesero atiende cada mesa. Una asignación activa por mesa; las anteriores quedan como historial. |
| `detalle_pedido_adicional.ddl` | `detalle_pedido_adicional` | HU-115 | Adicionales aplicados a una línea concreta del pedido. |
| `plato_insumo.ddl` | `plato_insumo` | HU-115 | Receta normalizada: cuánto insumo consume cada plato. Se solapa con `productos.ingredientes`. |
| `regla_descuento.ddl` | `regla_descuento` | HU-115 | Reglas de descuento aplicables al cobrar, por porcentaje o monto fijo. |

---

## Orden de ejecución

El orden importa: hay llaves foráneas entre las tablas.

```
 1. roles.ddl, tipos_documento.ddl, zonas.ddl, estados_mesa.ddl   ← catálogos, sin dependencias
 2. unidades_medida.ddl
 3. categorias.ddl
 4. clientes.ddl                        ← depende de tipos_documento
 5. usuarios.ddl                        ← depende de roles y tipos_documento
 6. mesas.ddl                           ← depende de zonas y estados_mesa
 7. ingredientes.ddl                    ← depende de unidades_medida
 8. productos.ddl                       ← depende de categorias
 9. precios_producto.ddl                ← depende de productos
10. pedidos.ddl                         ← depende de clientes, mesas y usuarios
11. detalle_pedido.ddl                  ← depende de pedidos y productos
12. movimientos_inventario.ddl          ← depende de productos, ingredientes y usuarios
13. alerta_inventario.ddl               ← depende de ingredientes
14. pagos.ddl                           ← depende de pedidos y usuarios
15. historial_estado_mesa               ← depende de mesas y estados_mesa
16. ../functions/hu28_disponibilidad.sql
```

---

## Ejecución

```bash
createdb gastroflow

# 1. tablas, en el orden de arriba
psql -d gastroflow -f roles.ddl
psql -d gastroflow -f tipos_documento.ddl
psql -d gastroflow -f zonas.ddl
psql -d gastroflow -f estados_mesa.ddl
psql -d gastroflow -f unidades_medida.ddl
psql -d gastroflow -f categorias.ddl
psql -d gastroflow -f clientes.ddl
psql -d gastroflow -f usuarios.ddl
psql -d gastroflow -f mesas.ddl
psql -d gastroflow -f ingredientes.ddl
psql -d gastroflow -f productos.ddl
psql -d gastroflow -f precios_producto.ddl
psql -d gastroflow -f pedidos.ddl
psql -d gastroflow -f detalle_pedido.ddl
psql -d gastroflow -f movimientos_inventario.ddl
psql -d gastroflow -f alerta_inventario.ddl
psql -d gastroflow -f pagos.ddl
psql -d gastroflow -f historial_estado_mesa.ddl

# 3. funciones de HU-28
psql -d gastroflow -f ../functions/hu28_disponibilidad.sql
```

Sobre una base **ya creada**, aplicar en cambio los scripts de `../migrations/` en
orden numérico. Todos son idempotentes: correrlos dos veces no duplica datos ni
falla. Los dos últimos tocan `mesas`: `005` agrega `cantidad_comensales` (HU-60) y
`006` vuelve `codigo_mesa` obligatorio y único, que es el identificador que el mapa
de salón le muestra al mesero (HU-047).

Para verificar:

```bash
psql -d gastroflow -c '\dt'
psql -d gastroflow -c '\df fn_*'
```

---

## Funciones almacenadas

`../functions/hu28_disponibilidad.sql` define las cuatro funciones que invoca
`DisponibilidadDAO`:

| Función | Devuelve | Uso |
|---|---|---|
| `fn_recalcular_estados_productos()` | `INTEGER` | Recalcula todo el catálogo; devuelve cuántos productos cambiaron de estado. |
| `fn_recalcular_estado_producto(producto_id)` | `VARCHAR` | Recalcula un producto y devuelve su estado resultante. |
| `fn_producto_disponible_cantidad(producto_id, cantidad)` | `BOOLEAN` | ¿Alcanza el inventario para N unidades? |
| `fn_motivo_receta(ingredientes)` | `TEXT` | Nombres de los ingredientes faltantes, o `NULL` si no falta ninguno. |

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

- **Llaves primarias:** `BIGINT GENERATED BY DEFAULT AS IDENTITY`.
- **Dinero:** `NUMERIC(12,2)`.
- **Cantidades de inventario:** `NUMERIC(12,4)`, tanto en stock como en movimientos.
- **Estados:** cadenas en mayúsculas validadas con `CHECK`, no enteros ni booleanos.
  - `unidades_medida.estado`, `categorias.estado`, `ingredientes.estado`: `ACTIVO`, `INACTIVO`
  - `productos.estado`: `DISPONIBLE`, `AGOTADO`, `INACTIVO`
  - `pedidos.estado`: `PENDIENTE`, `ASIGNADO_MESA`, `EN_PREPARACION`, `COMPLETADO`, `ENTREGADO`, `CANCELADO`
  - `movimientos_inventario.tipo_movimiento`: `ENTRADA`, `SALIDA`
  - `pagos.estado`: `PAGADO`, `ANULADO`
- **Auditoría:** `fecha_creacion` y `fecha_actualizacion` en las tablas maestras.
- **Nombres:** `pk_`, `uq_`, `fk_`, `ck_` para restricciones; `ix_` para índices.

---

## Choques de nombre, resueltos en HU-115

Tres consultas apuntaban a tablas que no existen, mientras la tabla equivalente
sí existía con otro nombre. No se crearon tablas nuevas para ellas —eso habría
dejado la base con dos tablas de clientes y dos de productos—: se corrigió el SQL
del lado Java.

| Lo que decía el código | Lo que dice ahora | Archivo |
|---|---|---|
| `FROM cliente` | `INTO clientes`, con tipo y número de documento y apellido | `repository/ClienteRepository.java` |
| `FROM insumo WHERE id` | `FROM ingredientes WHERE ingrediente_id` | `service/DisponibilidadService.java` |
| `UPDATE producto_menu SET disponible` | `UPDATE productos SET estado` | `service/DisponibilidadService.java` |

Dos no eran un cambio de nombre:

- `clientes` exige `id_tipo_documento`, `numero_documento` y `apellido`, los tres
  obligatorios. El `INSERT` anterior, con solo nombre, teléfono y correo, habría
  fallado siempre. Se añadió `apellido` a `entity/Cliente` y el tipo de documento
  se resuelve por su código (`CC`, `CE`…) en vez de por un id fijo.
- `productos` no tiene una columna booleana `disponible`: guarda un `estado` de
  tres valores. La actualización ahora escribe `DISPONIBLE` o `AGOTADO`, y deja
  en paz los productos `INACTIVO`, que el administrador sacó del menú a propósito.

Las pruebas están en `src/test/java/repository/ChoquesNombreTablaTest.java`.

`receta_ingrediente` salió de la lista por otra razón: el único archivo que la
consultaba, `dao/RecetaDAO.java`, se eliminó en el PR #253 al centralizar la
conexión. El `pom.xml` todavía lo excluye del build, junto con
`modelo/Receta.java` y `modelo/RecetaDetalle.java`, que tampoco existen ya.

---

## Pendientes conocidos

- `service/DisponibilidadService` y las funciones `fn_*` de HU-28 calculan lo
  mismo por caminos distintos: el primero lee la receta de `plato_insumo`, las
  segundas de `productos.ingredientes` (JSONB). Son dos fuentes para el mismo
  dato y dos implementaciones de la misma regla. Hay que decidir cuál queda.
- Unificar las dos convenciones de restricciones: este README usa `uq_`, y las tablas que
  llegaron del módulo de usuarios y mesas usan `uk_`.
- Definir trigger para que `fecha_actualizacion` se actualice sola en cada `UPDATE`.
- Los `CHECK` sobre columnas JSONB (`ck_producto_ingredientes_json`,
  `ck_pedido_productos_json`) no validan nada: `jsonb_typeof()` nunca devuelve `NULL`
  para un JSONB no nulo. Deberían comparar contra `'array'`.
- Evaluar normalizar las recetas y las líneas de pedido en tablas
  (`producto_ingrediente`, `pedido_detalle`): en JSONB no hay integridad referencial
  contra `ingredientes` ni `productos`.
- Unificar convención de nombres: `movimientos_inventario` usa `id_movimiento` e
  `INTEGER GENERATED ALWAYS`; el resto usa `<tabla>_id` y `BIGINT GENERATED BY DEFAULT`.
- `../functions/db-hu082-login.sql` inserta en una tabla `usuario` que creaba
  Hibernate cuando el proyecto iba a usar Spring. Con JDBC esa tabla no existe, así
  que el archivo falla siempre y `setup.sh` lo salta. Los usuarios de acceso los
  siembra la migración `008`. Queda por decidir si se borra.
- Las tablas de HU-115 usan `id` como llave primaria, igual que `detalle_pedido`,
  porque es el nombre que esperan los repositorios que las consultan. El resto del
  esquema usa `<tabla>_id`. Son dos convenciones conviviendo.
