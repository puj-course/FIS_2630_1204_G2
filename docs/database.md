# Base de datos de GastroFlow

## 1. Motor y nombre de la base

GastroFlow utiliza PostgreSQL 16. El script de instalación crea por defecto una base llamada `gastroflow`, aunque permite indicar otro nombre mediante `--db`.

La definición de las tablas se encuentra en `db/ddl/`; las funciones almacenadas están en `db/functions/`; y las modificaciones incrementales se encuentran en `db/migrations/`.

## 2. Organización funcional de las tablas

El directorio `db/ddl/` contiene 28 archivos de definición de tablas. Para facilitar su lectura, se agrupan por responsabilidad:

| Área | Tablas principales |
|---|---|
| Seguridad y catálogos | `roles`, `tipos_documento`, `usuarios`, `categorias`, `categoria_ingrediente`, `proveedores`, `unidades_medida` |
| Clientes y atención | `clientes`, `mesas`, `zonas`, `estados_mesa`, `asignacion_mesa`, `historial_estado_mesa`, `nota_mesa`, `reservas` |
| Productos y recetas | `productos`, `precios_producto`, `plato_insumo`, `adicional`, `detalle_pedido_adicional`, `regla_descuento` |
| Pedidos y pagos | `pedidos`, `detalle_pedido`, `pagos` |
| Inventario | `ingredientes`, `lote_ingrediente`, `movimientos_inventario`, `alerta_inventario` |

La migración `009_hu134_unificar_recetas.sql` también crea la tabla `auditoria_recetas_hu134`, utilizada para conservar información durante la normalización de recetas.

## 3. Relaciones principales

- Un rol puede estar asociado a varios usuarios.
- Un tipo de documento puede utilizarse en registros de usuarios y clientes.
- Una zona agrupa varias mesas; cada mesa tiene un estado definido en el catálogo `estados_mesa`.
- Una mesa puede tener historial de cambios, notas, reservas y asignaciones.
- Una categoría agrupa productos.
- Un producto puede tener varios ingredientes asociados mediante `plato_insumo`.
- Un pedido se relaciona con una mesa, un usuario responsable y sus detalles.
- Cada detalle de pedido referencia un producto y puede tener adicionales.
- Los movimientos y alertas de inventario registran información relacionada con ingredientes y, según el caso, productos.
- Los pagos se relacionan con pedidos y usuarios responsables.

Para conocer las columnas, llaves foráneas, restricciones e índices exactos, se deben consultar los archivos `.ddl`, que son la definición ejecutable del esquema.

## 4. Recetas y disponibilidad

El repositorio contiene `plato_insumo`, que relaciona productos e ingredientes con la cantidad necesaria. También conserva la columna `productos.ingredientes` de tipo JSONB. El DDL de `plato_insumo` advierte que ambas estructuras se solapan y que las funciones actuales de disponibilidad de HU-28 consultan el JSONB heredado.

Por tanto, no se debe describir `plato_insumo` como única fuente efectiva de disponibilidad sin revisar o completar la migración correspondiente. Esta coexistencia es una consideración técnica pendiente de unificación.

## 5. Funciones almacenadas

El archivo `db/functions/hu28_disponibilidad.sql` contiene las funciones utilizadas por `DisponibilidadDAO` para:

- Recalcular el estado de todos los productos.
- Recalcular el estado de un producto específico.
- Comprobar si hay inventario suficiente para una cantidad solicitada.
- Obtener los ingredientes faltantes de la receta.

El nombre y la firma exacta de cada función deben consultarse en ese archivo SQL.

## 6. Instalación y orden de creación

El script `scripts/setup.sh` automatiza la creación de la base. Ejecuta los DDL en orden de dependencias, aplica las funciones almacenadas y luego recorre las migraciones numeradas.

```bash
./scripts/setup.sh
./scripts/setup.sh --con-datos-prueba
./scripts/setup.sh --recrear --con-datos-prueba
./scripts/setup.sh --db otra_base
```

**Precaución:** `--recrear` elimina la base indicada y la crea de nuevo. No debe ejecutarse sobre una base con información que se necesite conservar.

## 7. Archivos de referencia

- `db/ddl/`: definiciones de tablas.
- `db/functions/`: funciones almacenadas.
- `db/migrations/`: cambios incrementales del esquema.
- `db/ddl/README.md`: descripción y orden de los DDL.
- `docs/database/gastroflow_er_actual.mmd`: diagrama entidad-relación existente; debe contrastarse con los DDL antes de considerarlo actualizado.

## 8. Observaciones de consistencia

Se detectaron diferencias entre algunos documentos y los archivos ejecutables. Por ejemplo, `db/ddl/README.md` describe un conteo de tablas que no coincide con la lista actual de `TABLAS` en `scripts/setup.sh`, y el diagrama ER existente utiliza nombres de entidades en singular mientras los DDL actuales crean varias tablas con nombres plurales. Para evitar errores, la fuente de verdad del esquema debe ser el conjunto de DDL y migraciones que ejecuta `scripts/setup.sh`.
