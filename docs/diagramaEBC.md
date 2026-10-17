# Diagramas de análisis EBC

Los tres diagramas entidad-borde-control del taller 4 de análisis, tal como se entregaron. Son los únicos que no tienen debajo una versión nueva: el EBC describe el análisis de una historia de usuario, no la estructura del código construido, así que no se puede generar leyendo el repositorio. Por eso se quedan en un archivo propio en lugar de encabezar uno de los otros.

---

## Diagramas del taller 4 de análisis

Se presentan los diagramas correspondientes al análisis del desarrollo lógico del proyecto, basado en tres issues las cuales son:
- HU-012 — Como administrador del restaurante, quiero registrar un nuevo plato con su información y los ingredientes de su receta para ofrecerlo en el menú, controlar su disponibilidad y descontar automáticamente los ingredientes del inventario.
- HU-015 — Como administrador del restaurante, quiero recibir alertas de inventario bajo para identificar los insumos que necesitan reposición y prevenir el desabastecimiento.
- HU-033: Como mesero, quiero editar y cancelar un pedido mientras se encuentra en proceso de solicitud o preparación, para corregir errores, modificar las cantidades o productos solicitados y cancelar pedidos cuando sea necesario.

### Diagrama EBC - Registrar plato

![Diagrama EBC de Registrar plato](imagenes/DiagramaEBC_RegistrarPlato.JPG)

El Administrador del restaurante interactúa con RegistroPlatoVista, que da paso a RecetaPlatoVista. PlatoService guarda el plato, RecetaService consulta la receta y DisponibilidadService verifica la disponibilidad, que se muestra en MenuMeseroVista. Las entidades son Plato, Categoria, Receta, RecetaDetalle e Ingrediente. Una categoría agrupa muchos platos, cada plato tiene una receta y cada receta se compone de varios detalles, cada uno ligado a un ingrediente.

### Diagrama EBC - Alerta Inventario

![Diagrama EBC de Alerta Inventario](imagenes/DiagramaEBC_AlertaInventario.JPG)

El Administrador usa tres pantallas: ConsultaStockVista, AlertasInventarioVista y RegistroEntradaInventarioVista. InventarioService consulta el ingrediente y genera la alerta, y AlertaInventarioService administra las alertas. Cuando hay que reabastecer, EntradaInventarioService registra la entrada. Las entidades son Ingrediente, AlertaInventario, MovimientoInventario, EntradaInventario y pedido, esta última relacionada con 0..1 movimientos.

### Diagrama EBC - Control de Cambios de Pedidos


![Diagrama EBC de Control de Cambios](imagenes/DiagramaEBC_ControlCambios.JPG)

El Mesero acciona el "botón cancelar pedido" en MenuMeseroVista, y PedidoService procesa la solicitud sobre Pedido y DetallePedido. Para completar la operación se apoya en InventarioService y MesaService. Un pedido tiene muchos detalles y pertenece a una mesa.
