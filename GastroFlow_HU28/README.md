# Gastroflow — HU-28 Disponibilidad automática de platos

Este proyecto implementa la HU-28 sobre el sistema Gastroflow, incluyendo la validación automática de disponibilidad de productos según el stock de sus ingredientes.

## Qué hace

- Valida automáticamente los ingredientes requeridos por cada producto.
- Cambia el estado del producto a `AGOTADO` cuando falta al menos un ingrediente necesario.
- Actualiza nuevamente el estado a `DISPONIBLE` cuando los ingredientes cuentan con stock suficiente.
- Mantiene el estado `INACTIVO` como una decisión administrativa y no lo modifica automáticamente.
- El menú del Mesero muestra los productos agotados y bloquea la opción de agregarlos al pedido.
- Antes de agregar un producto y antes de confirmar un pedido se valida nuevamente la disponibilidad.
- Incluye una vista para el Administrador donde se visualiza el estado del producto y el motivo de agotamiento.

## Formato requerido en productos.ingredientes

La funcionalidad utiliza la información de la receta asociada a cada producto mediante la columna `productos.ingredientes`.

Esta columna debe contener la receta en formato JSONB.

Ejemplo:

```json
[
  {"ingrediente_id": 1, "cantidad": 150.0000},
  {"ingrediente_id": 2, "cantidad": 40.0000}
]