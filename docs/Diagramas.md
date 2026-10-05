# Diagramas de Anáñisis EBC, Clases,Componentes y Despliegue
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

### Diagrama de Clases - Registrar Plato

![Diagrama de Clases de Registrar Plato](imagenes/DiagramaClases_RegistrarPlato.JPG)

Plato es la entidad central, con código, nombre, descripción, categoría, precio de aventa, costo, stock actual, mínimo y máximo, y estado, además de sus getters y setters. PlatoRepository la usa mediante guardar(plato) y obtiene la conexión a PostgreSQL desde ConexionDB.obtenerConexion(), ubicada en el paquete conf.

### Diagrama de Clases - Alerta Invetario

![Diagrama de Clases de Alerta Inventario](imagenes/DiagramaClases_AlertaInventario.JPG)

La solución está organizada por capas. AlertaInventarioController (paquete controller) recibe la petición y la delega a AlertaInventarioService (paquete service) mediante evaluarInventario(ingrediente). La entidad AlertaInventario (paquete entity) guarda el ingrediente, el stock actual y mínimo, la cantidad sugerida, si está resuelta y las fechas de creación y resolución. StockInsuficienteException queda en el paquete exception.

### Diagrama de Clases - Control de cambios de Pedidos


![Diagrama de Clases de Control de cambios](imagenes/DiagramaClases_ControlCambios.JPG)

El diagrama muestra la entidad DetallePedido, que relaciona un Pedido con un Plato y una cantidad. Es el vínculo entre las líneas del pedido y el catálogo.

### Diagrama Componentes - Registrar Plato

![Diagrama de Componentes de Registrar Plato](imagenes/DiagramaComponentes_RegistrarPlato.JPG)

El componente Administrador (DisponibilidadAdminController) consume una interfaz de Lógica de Registro de Plato, que agrupa Plato, Receta, RecetaDetalle y PlatoIngrediente. Esta lógica usa el componente Persistencia (PlatoRepository, PlatoIngredienteRepository, RecetaDAO), que accede a la base de datos.

### Diagrama Componentes - Alerta Invetario

![Diagrama de Componentes de Alerta Invetario](imagenes/DiagramaComponentes_AlertaInventario.JPG)

El componente Inventario (InventarioController, AlertaInventarioController) se conecta por interfaz con Gestión de Alertas (AlertaInventario, AlertaInventarioService). Este componente usa Persistencia (AlertaInventarioRepository, IngredienteRepository, MovimientoInventarioRepository), que accede a la base de datos.

### Diagrama Componentes - Control de cambios de Pedidos

![Diagrama de Componentes de Control de cambios](imagenes/DiagramaComponentes_ControlCambios.JPG)

El componente Mesero (MenuMeseroController) se comunica con Gestión de Pedidos (PedidoService, Pedido, DetallePedido, EstadoPedido). Esta se relaciona con Gestión de Inventario (InventarioService, MovimientoInventario) y con Persistencia (PedidoRepository, DetallePedidoRepository), que accede a la base de datos PostgreSQL.

### Diagrama de Despliegue - Registro de plato, Alerta inventario y Control de cambios de pñedido

![Diagrama de Despliegue](imagenes/DiagramaDespliegue.JPG)
El sistema se despliega en tres nodos. En Dispositivos de Usuario, el Dispositivo Administrador ejecuta la UI Administrador y el Dispositivo Mesero ejecuta la UI Mesero. Ambos se comunican por API/TCP con el Servidor de Aplicaciones, que aloja el Módulo de Inventario (atiende al administrador) y el Módulo de Pedidos (atiende al mesero). Los dos módulos envían datos al Servidor de Base de Datos, donde PostgreSQL almacena las tablas. Es una arquitectura de tres capas.
