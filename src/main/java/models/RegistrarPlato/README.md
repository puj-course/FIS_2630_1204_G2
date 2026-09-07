# Módulo: Registrar Plato

## Descripción
Este módulo permite registrar un nuevo plato en el catálogo de productos del restaurante,
almacenando su información básica (nombre, código, descripción, categoría, precio de venta
y costo) en la tabla `productos` de la base de datos.

## Historia de usuario relacionada
HU-011 — Registrar Plato

## Estructura de archivos

| Archivo | Ubicación | Responsabilidad |
|---|---|---|
| `Plato.java` | `src/models/RegistrarPlato/` | Representa los datos de un plato (modelo). |
| `PlatoRepository.java` | `src/repositories/RegistrarPlato/` | Ejecuta el `INSERT` del plato en la tabla `productos`. |

## Atributos del modelo `Plato`

| Atributo | Tipo | Descripción |
|---|---|---|
| `productoId` | `long` | Identificador único, generado automáticamente por la base de datos. |
| `codigo` | `String` | Código interno del plato (ej. "PLA001"). |
| `nombre` | `String` | Nombre del plato. |
| `descripcion` | `String` | Descripción breve del plato. |
| `categoria` | `String` | Categoría a la que pertenece (ej. "Platos Fuertes"). |
| `precioVenta` | `double` | Precio de venta al público. |
| `costo` | `double` | Costo de producción del plato. |
| `stockActual`, `stockMinimo`, `stockMaximo` | `double` | Manejados por la base de datos con valores por defecto; no se envían al registrar un plato nuevo. |
| `estado` | `String` | Estado del plato (`DISPONIBLE`, `AGOTADO`, etc.), asignado por defecto en la base de datos. |

## Tabla de base de datos: `productos`

Definida por el equipo de Database Engineer. Columnas relevantes usadas en este módulo:
`producto_id`, `codigo`, `nombre`, `descripcion`, `categoria`, `precio_venta`, `costo`.

Las columnas `stock_actual`, `stock_minimo` y `estado` tienen valores por defecto
(`DEFAULT`), por lo que no es necesario incluirlos al registrar un plato nuevo.

## Pendientes / mejoras futuras
- La columna `ingredientes` (tipo `JSONB`) no se está utilizando todavía; la relación entre
  platos e insumos (receta) se maneja en un módulo aparte (HU-019).
- Falta validar la unicidad del `codigo` antes de insertar, para evitar duplicados.

## Notas
Código desarrollado con apoyo de una LLM, sujeto a ajustes tras pruebas de integración
con el equipo de base de datos.
