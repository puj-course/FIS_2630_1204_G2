# Módulo: Registro de Entradas de Inventario

## Descripción
Este módulo permite registrar el ingreso de insumos/ingredientes al inventario,
actualizando automáticamente el stock del producto correspondiente.

## Clases
- **EntradaInventario**: modelo que representa un movimiento de entrada.
- **EntradaInventarioRepository**: maneja el guardado en la tabla `movimientos_inventario`.
- **EntradaInventarioService**: valida la cantidad (> 0) y actualiza el stock en `productos`.

## Historia de usuario relacionada
HU-010 (#29)

## Notas
- Requiere que el `ingrediente_id`/`producto_id` ya exista previamente en la tabla `productos`.
- Desarrollado con apoyo de una LLM, sujeto a ajustes tras pruebas con el equipo de base de datos.

