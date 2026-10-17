# Diagramas de clases

**Issue:** Documentación de diseño y consolidación del modelo de datos — trabajo previo, no corresponde a ninguna de las tres HU de esta semana  
**Rama:** `develop`  
**Commit:** `084c099`  
**Fecha del commit:** 2026-10-15

Documentan el modelo de clases completo de la rama: 98 tipos, 371 atributos y 663 métodos. Se reparten en quince láminas porque una sola resultaba ilegible: siete de miembros, agrupadas por capa, y ocho de relaciones, separadas por origen y destino para que las líneas no se crucen.

Son 15 diagramas.

---

## Entidades A — de Adicional a EstadoMesa

![Entidades A — de Adicional a EstadoMesa](Imagenes/DiagramaClases_Entidades_A.png)

**Por qué está.** El modelo tiene 98 tipos. Dibujarlos juntos produce una lámina que no se puede leer ni imprimir, así que partimos las entidades en tres láminas por orden alfabético. Esta primera cubre el primer tercio y existe para poder consultar los atributos y las operaciones de cada entidad sin ir a buscarlas al código.

**Qué muestra.** Contiene 19 clases y 1 interfaz. Están las entidades del dominio de adicionales, inventario, asignación de mesas y clientes, cada una con sus atributos y sus métodos tal como los declara el código. Al lado de cada entidad aparecen los repositorios y servicios que la manejan, para que la lámina se lea sin saltar a otra.

---

## Entidades B — de HistorialEstadoMesa a MovimientoInventario

![Entidades B — de HistorialEstadoMesa a MovimientoInventario](Imagenes/DiagramaClases_Entidades_B.png)

**Por qué está.** Segundo tercio del modelo, con el mismo criterio de corte que la lámina anterior: mantener cada una por debajo del tamaño en el que las cajas dejan de ser legibles.

**Qué muestra.** Son 17 tipos: 15 clases, la interfaz MeseroRepository y el enum TipoMovimiento. Aquí está el núcleo de mesas (Mesa, HistorialEstadoMesa, NotaMesa) y el de inventario (Ingrediente, MovimientoInventario), con los controladores de cocina y mapa de salón que los consumen.

---

## Entidades C — de Pago a Zona

![Entidades C — de Pago a Zona](Imagenes/DiagramaClases_Entidades_C.png)

**Por qué está.** Último tercio del modelo. Cierra el recorrido por las entidades y recoge las dos enumeraciones que gobiernan el flujo del pedido y el del pago.

**Qué muestra.** Son 31 tipos. Incluye Pedido y Pago, los enums EstadoPedido y MetodoPago, la interfaz PagoRepository, y las entidades de carta y salón (Plato, Producto, Zona, Reserva). EstadoPedido es la base del diagrama de estados que documentamos aparte.

---

## Controladores JavaFX

![Controladores JavaFX](Imagenes/DiagramaClases_Controladores.png)

**Por qué está.** La capa de presentación es la que más cambia entre entregas y la que más preguntas recibe en las revisiones. Separarla en su propia lámina permite ver de un vistazo qué pantalla existe y qué operaciones expone cada una.

**Qué muestra.** Son 29 tipos y 13 relaciones. Están los ocho controladores JavaFX (Login, MapaSalon, MenuMesero, Cocina, CajeroPago, Inventario y DisponibilidadAdmin) con sus métodos @FXML, más las clases auxiliares que declaran dentro (AccionMesa, ResumenMesas) y los enums que solo ellos usan, como EstadoMesaVisual.

---

## Repositorios

![Repositorios](Imagenes/DiagramaClases_Repositorios.png)

**Por qué está.** El proyecto no usa ORM: cada repositorio escribe su propio SQL. Esta lámina existe para poder revisar de una vez las clases que tocan la base de datos, que es donde se concentran los errores de consulta.

**Qué muestra.** Son 64 tipos y 27 relaciones. Además de los repositorios aparecen las tres interfaces que extienden JpaRepository y que el pom.xml excluye de la compilación (DetallePedidoRepository, MeseroRepository y PagoRepository), señaladas como tales, y los enums que las consultas usan como parámetro.

---

## Servicios y DAO

![Servicios y DAO](Imagenes/DiagramaClases_Servicios_DAO.png)

**Por qué está.** La lógica de negocio vive repartida entre service y dao. Verlas juntas es lo único que deja comprobar que no haya reglas duplicadas en las dos capas, que fue uno de los riesgos que detectamos al unificar la arquitectura.

**Qué muestra.** Son 64 tipos y 33 relaciones. Están los doce servicios y los tres DAO (DisponibilidadDAO, PagoDAO y ProductoDAO), con PedidoFacade como fachada del flujo de pedido. Los tres DAO ejecutan SQL directamente, igual que un repositorio, y por eso en el diagrama de arquitectura los situamos en la capa de datos y no en la de negocio.

---

## Clases de apoyo: excepciones, enums, DTO y utilidades

![Clases de apoyo: excepciones, enums, DTO y utilidades](Imagenes/DiagramaClases_Apoyo.png)

**Por qué está.** Las excepciones y los enums no pertenecen a ninguna capa y ensucian cualquier lámina en la que se metan. Agruparlos aparte mantiene limpias las otras y da un sitio único donde consultar qué excepción lanza el sistema y qué valores admite cada enumeración.

**Qué muestra.** Son 47 tipos. Incluye las nueve excepciones propias, los seis enums del dominio, los DTO (IngredienteStockDTO, ProductoDisponibilidad, CuentaPago e ItemPedido), la clase de conexión y Main.

---

## Relaciones: controladores hacia las otras capas

![Relaciones: controladores hacia las otras capas](Imagenes/DiagramaClases_Relaciones_Controller_Capas.png)

**Por qué está.** En las láminas anteriores las relaciones se omiten para que las cajas se lean. Esta serie de siete mapas las recupera, una capa por lámina, que es la única forma de que las líneas no se crucen hasta volverse indistinguibles.

**Qué muestra.** Muestra a qué servicios, DAO y repositorios llama cada controlador. Son 21 tipos y 5 relaciones. Permite ver qué controladores se saltan la capa de servicio y acceden directamente a datos, que es un hallazgo que confirmamos en el código.

---

## Relaciones: controladores hacia el modelo

![Relaciones: controladores hacia el modelo](Imagenes/DiagramaClases_Relaciones_Controller_Modelo.png)

**Por qué está.** Separada de la anterior porque mezclar las llamadas a otras capas con el uso de entidades volvía la lámina ilegible.

**Qué muestra.** Son 19 tipos y 17 relaciones. Muestra qué entidades y qué enums manipula cada controlador directamente, sin pasar por un servicio.

---

## Relaciones: servicios hacia las otras capas

![Relaciones: servicios hacia las otras capas](Imagenes/DiagramaClases_Relaciones_Service_Capas.png)

**Por qué está.** Mismo criterio: una lámina por origen y por destino. Esta es la que muestra el camino normal de una operación de negocio hacia la base de datos.

**Qué muestra.** Son 33 tipos y 9 relaciones: cada servicio con los repositorios y DAO que usa. PedidoFacade aparece aquí como el único punto que agrupa varios servicios, que es el patrón Facade que ya teníamos implementado.

---

## Relaciones: servicios hacia el modelo

![Relaciones: servicios hacia el modelo](Imagenes/DiagramaClases_Relaciones_Service_Modelo.png)

**Por qué está.** Completa la vista anterior con las entidades que cada servicio construye o modifica.

**Qué muestra.** Son 45 tipos. Muestra qué entidades toca cada servicio y con qué enums trabaja, entre ellos EstadoPedido y EstadoMesa.

---

## Relaciones: repositorios hacia las otras capas

![Relaciones: repositorios hacia las otras capas](Imagenes/DiagramaClases_Relaciones_Repository_Capas.png)

**Por qué está.** Un repositorio no debería llamar a otra capa. Esta lámina existe precisamente para poder comprobarlo de un vistazo.

**Qué muestra.** Son 21 tipos. La lámina está prácticamente vacía de líneas, y ese es el resultado: los repositorios no llaman hacia arriba. La única relación entre iguales que sí existe, de ProductoDAO hacia DisponibilidadDAO, la recogemos en la lámina de la capa de datos.

---

## Relaciones: repositorios hacia el modelo

![Relaciones: repositorios hacia el modelo](Imagenes/DiagramaClases_Relaciones_Repository_Modelo.png)

**Por qué está.** Es el mapa de qué entidad lee y escribe cada repositorio, que es lo que hace falta para rastrear quién toca cada tabla.

**Qué muestra.** Son 48 tipos y 27 relaciones, entre ellas las de herencia hacia JpaRepository. Junto con el diagrama de componentes de datos permite detectar tablas escritas desde más de un repositorio.

---

## Relaciones dentro del modelo

![Relaciones dentro del modelo](Imagenes/DiagramaClases_Relaciones_Modelo.png)

**Por qué está.** Las relaciones entre entidades son las que sostienen el modelo de datos, y conviene verlas sin el ruido de las otras capas.

**Qué muestra.** Son 18 tipos y 13 relaciones: composiciones, agregaciones y asociaciones entre entidades, con la multiplicidad deducida del tipo declarado de cada campo. Solo las composiciones llevan multiplicidad en el extremo de origen, porque es la única que el código justifica.

---

## Clases de prueba

![Clases de prueba](Imagenes/DiagramaClases_Pruebas.png)

**Por qué está.** Las pruebas son parte de la entrega y tienen sus propias dependencias. Documentarlas aparte evita mezclarlas con el código de producción en las otras láminas.

**Qué muestra.** Son 29 tipos y 24 relaciones: 10 clases de prueba repartidas en 7 archivos, con las clases de producción que cada una ejercita. Incluye las clases anidadas que JUnit usa para agrupar casos.

---


## Cómo se construyeron

Estos diagramas no se dibujaron a mano. Los genera un conjunto de scripts que
leen el código fuente de la rama y producen el `.puml`, y se comprueban contra
ese mismo código con verificadores independientes que vuelven a leerlo con otro
método. Si el código cambia y el diagrama deja de ser cierto, la comprobación
falla.

| Qué | Dónde |
|---|---|
| Rama | `develop` |
| Commit | `084c099` (084c099423b4bdd827894e4907aca8b943b1a963) |
| Fecha del commit | 2026-10-15 |
| Fuente de los diagramas | PlantUML, en `docs/diagramas-fuente/` |
| Herramienta que los genera | `docs/diagramas-fuente/herramientas/` |
| Imágenes | PNG a dpi 96, en `docs/Imagenes/` |

Para regenerarlos hace falta el clon de la rama y una sola orden:

```bash
cd docs/diagramas-fuente/herramientas
./rehacer.sh /ruta/al/clon/de/FIS_2630_1204_G2
```

Esa orden vuelve a leer el código, regenera los `.puml`, los renderiza y corre
todas las comprobaciones. Necesita Python 3, Java y PlantUML. Los archivos
intermedios los deja dentro de esa misma carpeta.
