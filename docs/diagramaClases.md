# Diagramas de clases

Documentan el modelo de clases del sistema: 98 tipos, 371 atributos y 663 métodos. Están organizados siguiendo la arquitectura en capas del proyecto: primero las entidades del dominio, después cada capa con sus miembros, y al final las relaciones, separadas por origen y destino para que cada vista responda una sola pregunta.

---

## Entidades del dominio (A): adicionales, inventario, mesas y clientes

![Entidades del dominio (A): adicionales, inventario, mesas y clientes](Imagenes/DiagramaClases_Entidades_A.png)

**Por qué está.** Las entidades son el vocabulario del sistema: los servicios, los repositorios y las pantallas se definen todos en términos de ellas, así que el modelo empieza por aquí. Este bloque cubre la parte del negocio que sostiene la carta y el salón.

**Qué muestra.** Hay 19 clases y 1 interfaz, con sus atributos y sus métodos. Se ve que un adicional se asocia a un detalle de pedido a través de DetallePedidoAdicional, que la asignación de una mesa es una entidad propia y no un campo de Mesa, y que las alertas de inventario se guardan en lugar de calcularse al vuelo. Junto a cada entidad aparece el repositorio y el servicio que la manejan.

---

## Entidades del dominio (B): mesas, historial e inventario

![Entidades del dominio (B): mesas, historial e inventario](Imagenes/DiagramaClases_Entidades_B.png)

**Por qué está.** Aquí está el núcleo del salón. La mesa es la entidad con más estados y más escrituras concurrentes del sistema, y entenderla es condición para leer el diagrama de estados y el de componentes de datos.

**Qué muestra.** Son 17 tipos. Mesa no guarda su estado como texto sino como clave hacia el catálogo estados_mesa, y cada cambio deja una fila en HistorialEstadoMesa: el historial es parte del modelo, no una tabla de auditoría añadida aparte. NotaMesa cuelga de la mesa con una nota por mesa como máximo. El enum TipoMovimiento clasifica las entradas y salidas de inventario.

---

## Entidades del dominio (C): pedidos, pagos, carta y reservas

![Entidades del dominio (C): pedidos, pagos, carta y reservas](Imagenes/DiagramaClases_Entidades_C.png)

**Por qué está.** Cierra el modelo con el flujo que da sentido al resto: un pedido se abre sobre una mesa, se llena con productos de la carta y termina en un pago. Las dos enumeraciones que gobiernan ese flujo se declaran aquí.

**Qué muestra.** Son 31 tipos. EstadoPedido define los seis estados por los que pasa una comanda, y MetodoPago las formas de cobro. Pago no cuelga del pedido por composición sino que lo referencia, de modo que un pedido puede existir sin pago; eso sí, la entidad Pago es una de las que el pom.xml excluye de la compilación, así que el cobro no está cerrado. Reserva apunta a la mesa y no al revés, y Zona agrupa las mesas del salón.

---

## Capa de presentación: los controladores JavaFX

![Capa de presentación: los controladores JavaFX](Imagenes/DiagramaClases_Controladores.png)

**Por qué está.** Cada pantalla de la aplicación es un controlador, así que este diagrama es el inventario de lo que el usuario puede hacer. Sirve para saber qué funcionalidad está construida y cuál sigue sin interfaz.

**Qué muestra.** Son 29 tipos y 12 relaciones. Hay ocho pantallas: acceso, mapa de salón, carta del mesero, cocina, caja, inventario y disponibilidad. El mapa de salón es con diferencia el controlador más grande, y declara dentro sus propias clases auxiliares (AccionMesa, ResumenMesas) y el enum EstadoMesaVisual, que traduce los códigos de la base a colores y etiquetas de pantalla.

---

## Capa de acceso a datos: los repositorios

![Capa de acceso a datos: los repositorios](Imagenes/DiagramaClases_Repositorios.png)

**Por qué está.** La decisión de arquitectura fue JDBC sin ORM, lo que significa que cada repositorio escribe su propio SQL. Esa decisión concentra aquí el riesgo de inconsistencia, y por eso la capa se documenta completa.

**Qué muestra.** Son 64 tipos y 27 relaciones. Cada repositorio expone métodos de consulta con nombre propio en lugar de un CRUD genérico, porque las consultas están escritas a mano. Tres interfaces (DetallePedidoRepository, MeseroRepository y PagoRepository) extienden JpaRepository y son el residuo de la etapa con Spring: el pom.xml las excluye de la compilación y no forman parte del sistema que se ejecuta.

---

## Capa de negocio: servicios y DAO

![Capa de negocio: servicios y DAO](Imagenes/DiagramaClases_Servicios_DAO.png)

**Por qué está.** Es la capa donde se decide si una operación es válida. Documentarla completa es lo que permite comprobar que las reglas viven en un solo sitio y no repartidas entre dos estilos de clase.

**Qué muestra.** Son 64 tipos y 33 relaciones. Conviven doce servicios y tres DAO (DisponibilidadDAO, PagoDAO y ProductoDAO) que hacen el mismo trabajo con otro nombre: los tres ejecutan SQL directamente, así que pertenecen a la capa de datos aunque estén en el paquete dao. PedidoFacade agrupa el flujo de pedido detrás de una sola interfaz, que es el patrón Facade aplicado en el proyecto.

---

## Excepciones, enumeraciones, DTO y arranque

![Excepciones, enumeraciones, DTO y arranque](Imagenes/DiagramaClases_Apoyo.png)

**Por qué está.** Las excepciones definen el contrato de error del sistema: qué puede fallar y con qué nombre. Junto con las enumeraciones son el vocabulario que atraviesa todas las capas, y por eso se documentan juntas.

**Qué muestra.** Son 47 tipos. Las nueve excepciones propias nombran reglas de negocio concretas (MesaOcupadaException, PedidoNoEditableException, StockInsuficienteException), de modo que una regla rota se distingue de un fallo técnico. Los DTO transportan datos agregados que no corresponden a una sola entidad, como CuentaPago o ProductoDisponibilidad. Main es el punto de entrada JavaFX.

---

## Qué llama la presentación hacia abajo

![Qué llama la presentación hacia abajo](Imagenes/DiagramaClases_Relaciones_Controller_Capas.png)

**Por qué está.** Una arquitectura en capas solo se sostiene si las llamadas respetan el orden. Este diagrama responde si la presentación pasa siempre por la capa de negocio o si atajaba hacia los datos.

**Qué muestra.** Son 21 tipos y 5 relaciones. La respuesta es que no siempre pasa: hay controladores que llaman directamente a repositorios y a DAO, saltándose los servicios. El mapa de salón es el caso más claro, y es el mismo salto que reaparece en el diagrama de estados de la mesa como escritura sin auditoría.

---

## Qué entidades manipula la presentación

![Qué entidades manipula la presentación](Imagenes/DiagramaClases_Relaciones_Controller_Modelo.png)

**Por qué está.** Complementa al anterior con la otra pregunta: además de a quién llama, qué datos del dominio toca cada pantalla directamente.

**Qué muestra.** Son 19 tipos y 16 relaciones. Los controladores construyen y modifican entidades por su cuenta, no solo DTO, y usan los enums del dominio para decidir qué pintar. Es lo que explica que la lógica de estado del pedido esté repartida entre el servicio y la pantalla de cocina.

---

## Qué usa la capa de negocio para llegar a los datos

![Qué usa la capa de negocio para llegar a los datos](Imagenes/DiagramaClases_Relaciones_Service_Capas.png)

**Por qué está.** Es el camino normal de una operación: una pantalla pide algo a un servicio y el servicio lo resuelve contra la base. Este diagrama lo traza entero.

**Qué muestra.** Son 33 tipos y 9 relaciones. Cada servicio depende de varios repositorios, señal de que las operaciones de negocio cruzan varias tablas. PedidoFacade es el único que depende de otros servicios en lugar de repositorios, que es justo lo que hace de él una fachada.

---

## Qué entidades construye y modifica cada servicio

![Qué entidades construye y modifica cada servicio](Imagenes/DiagramaClases_Relaciones_Service_Modelo.png)

**Por qué está.** Permite saber quién es responsable de cada entidad: si dos servicios escriben la misma, hay una regla de negocio repartida en dos sitios.

**Qué muestra.** Son 44 tipos. Muestra con qué entidades y con qué enumeraciones trabaja cada servicio. EstadoPedido y EstadoMesa aparecen en varios, que es la razón por la que las transiciones de estado se documentan en su propio diagrama.

---

## Comprobación: los repositorios no llaman hacia arriba

![Comprobación: los repositorios no llaman hacia arriba](Imagenes/DiagramaClases_Relaciones_Repository_Capas.png)

**Por qué está.** En una arquitectura en capas la de datos debe ser la última: puede ser llamada, pero no debe llamar a negocio ni a presentación. Este diagrama existe para verificarlo.

**Qué muestra.** Son 21 tipos y el resultado es que la comprobación se cumple: no hay flechas saliendo de la capa de datos hacia arriba. La única relación entre iguales es ProductoDAO hacia DisponibilidadDAO, dentro de la misma capa, y queda recogida en el diagrama de componentes de datos.

---

## Qué entidad lee y escribe cada repositorio

![Qué entidad lee y escribe cada repositorio](Imagenes/DiagramaClases_Relaciones_Repository_Modelo.png)

**Por qué está.** Sin ORM, la correspondencia entre clase y tabla no está declarada en ninguna anotación: hay que deducirla del SQL. Este diagrama la deja escrita, y es lo que permite rastrear quién toca cada dato.

**Qué muestra.** Son 47 tipos y 27 relaciones. La mayoría de repositorios mapean una entidad cada uno, pero algunos leen varias para componer un resultado. Las tres relaciones de herencia que aparecen son las de las interfaces de Spring que no compilan.

---

## Cómo se relacionan las entidades entre sí

![Cómo se relacionan las entidades entre sí](Imagenes/DiagramaClases_Relaciones_Modelo.png)

**Por qué está.** Es el modelo conceptual del negocio, sin capas ni tecnología: qué pertenece a qué y con qué cardinalidad. Es la vista que hay que contrastar con el modelo de base de datos.

**Qué muestra.** Son 18 tipos y 13 relaciones, con la multiplicidad deducida del tipo declarado de cada campo. Se distingue composición de agregación: un pedido se compone de sus ítems y no tienen vida propia, mientras que un pago referencia al pedido sin ser parte de él. Solo las composiciones llevan multiplicidad en el extremo de origen, porque es la única que el código permite afirmar.

---

## Qué cubren las pruebas automáticas

![Qué cubren las pruebas automáticas](Imagenes/DiagramaClases_Pruebas.png)

**Por qué está.** Las pruebas son parte de la entrega y delimitan qué partes del sistema están verificadas. Este diagrama responde qué se prueba y qué no.

**Qué muestra.** Son 29 tipos y 24 relaciones: 10 clases de prueba en 7 archivos, con las clases de producción que ejercita cada una. La cobertura funcional se concentra en mesas y pedidos: son los dos únicos módulos con pruebas contra la base de datos. El resto son pruebas unitarias de clases sueltas (CuentaPago, ItemPedido, TextoBusqueda y disponibilidad), sin inventario ni carta completos. Las clases anidadas son los grupos de casos de JUnit.
