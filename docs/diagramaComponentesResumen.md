# Diagramas de componentes (vista simple)

Cuatro vistas para entender la estructura de un vistazo: una caja por componente con su nombre y una flecha por dependencia, sin operaciones ni listas de archivos. Salen de las mismas llamadas medidas sobre el código que las vistas detalladas, así que no dicen nada distinto: dicen lo mismo con menos detalle. El desglose completo está en diagramaComponentes.md.

---

## Vista de conjunto: las cinco capas

![Vista de conjunto: las cinco capas](Imagenes/DiagramaComponentes_S1_Capas.png)

**Por qué está.** Es la entrada al resto. Responde en cuántas partes está dividido el sistema y en qué dirección depende cada una de las otras, sin entrar en nombres de clase.

**Qué muestra.** Cinco capas y las dependencias reales entre ellas, con el número de operaciones distintas que se invocan a través de cada una. El flujo normal baja de interfaz a negocio, de negocio a datos y de datos a la conexión. Las dos flechas que no siguen ese orden son los saltos de capa: la interfaz llamando directamente a datos y el negocio llamando directamente a la conexión.

---

## Qué usa cada pantalla

![Qué usa cada pantalla](Imagenes/DiagramaComponentes_S2_UI.png)

**Por qué está.** Antes de tocar una pantalla hay que saber de qué depende. Esta vista lo responde de un vistazo, con una caja por pantalla y una flecha por dependencia.

**Qué muestra.** Las ocho pantallas con los componentes que llaman, coloreados por capa. Se ve que la mayoría depende de una o dos piezas, y que el mapa de salón y la carta del mesero son las que más acumulan. También se ve qué pantallas llaman directamente a repositorios en lugar de pasar por un servicio.

---

## Qué repositorio usa cada servicio

![Qué repositorio usa cada servicio](Imagenes/DiagramaComponentes_S3_Negocio.png)

**Por qué está.** Muestra el camino de una operación de negocio hasta la base de datos, que es lo que hay que seguir para saber dónde se guarda cada cosa.

**Qué muestra.** Arriba quedan los servicios que se apoyan unos en otros: PedidoFacade coordina a PedidoService, MesaService e InventarioService, y esos tres comparten repositorios. Abajo, los servicios que usan un único repositorio propio y no dependen de nadie más. El aviso marca las clases que el pom.xml excluye de la compilación.

---

## Las piezas que usa todo el sistema

![Las piezas que usa todo el sistema](Imagenes/DiagramaComponentes_S4_Apoyo.png)

**Por qué está.** Son los componentes que no pertenecen a ninguna capa y que, por eso, cualquier cambio en ellos afecta a muchos sitios a la vez.

**Qué muestra.** ConexionBD, SesionUsuario y TextoBusqueda, con sus consumidores agrupados por capa y el número de componentes de cada una. ConexionBD es con diferencia la más usada, porque sin ORM cada repositorio abre su propia conexión.
