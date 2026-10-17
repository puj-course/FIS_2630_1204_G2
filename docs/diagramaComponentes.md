# Diagramas de componentes

Documentan en qué piezas sustituibles está dividido el sistema y cómo dependen unas de otras, con las operaciones que cada componente provee y los archivos que lo forman. Las interfaces no son un diseño sobre el papel: cada una corresponde a las llamadas que de verdad cruzan el límite entre dos componentes. Cuatro de las vistas llevan debajo una versión resumida, con una caja por componente y una flecha por dependencia, para leerlas de un vistazo.

---

## Los cinco grupos y su dependencia

![Los cinco grupos y su dependencia](Imagenes/DiagramaComponentes_General.png)

**Por qué está.** Un componente es una pieza que se puede sustituir si se respeta su interfaz. Esta vista responde en cuántas piezas está dividido GastroFlow y en qué orden dependen unas de otras.

**Qué muestra.** El sistema se agrupa en interfaz, negocio, datos, conexión y apoyo. Las dependencias van en una sola dirección salvo las que atraviesan hacia datos desde la interfaz. Las interfaces provistas llevan el nombre real de la clase que las implementa: se dedujeron de las llamadas que cruzan el límite entre un grupo y otro, no de un diseño sobre el papel.

### Versión resumida: las cinco capas

![Versión resumida: las cinco capas](Imagenes/DiagramaComponentes_S1_Capas.png)

**Por qué está.** La misma división en cinco grupos, sin interfaces ni nombres de clase, para quedarse solo con la dirección de las dependencias.

**Qué muestra.** Las cinco capas con el número de operaciones distintas que se invocan a través de cada dependencia. El flujo normal baja de interfaz a negocio, de negocio a datos y de datos a la conexión. Las dos flechas que no siguen ese orden son los saltos de capa: la interfaz llamando directamente a datos y el negocio llamando directamente a la conexión.

---

## Qué necesita cada pantalla para funcionar

![Qué necesita cada pantalla para funcionar](Imagenes/DiagramaComponentes_UI.png)

**Por qué está.** Antes de tocar una pantalla conviene saber de qué depende. Esta vista lo resuelve pantalla por pantalla, incluidos los recursos que no son código.

**Qué muestra.** Son 20 componentes y 14 interfaces consumidas, con 17 conexiones. Cada componente agrupa su .fxml, su hoja de estilos y su controlador, de modo que se ve qué archivos hay que tocar para cambiar una pantalla. Las hojas de estilo las carga Main y no cada controlador, que es una dependencia fácil de pasar por alto.

### Versión resumida: qué usa cada pantalla

![Versión resumida: qué usa cada pantalla](Imagenes/DiagramaComponentes_S2_UI.png)

**Por qué está.** Lo mismo sin los archivos ni las operaciones: una caja por pantalla y una flecha por dependencia.

**Qué muestra.** Las ocho pantallas con los componentes que llaman, coloreados por capa. La mayoría depende de una o dos piezas, y el mapa de salón y la carta del mesero son las que más acumulan. También se ve qué pantallas llaman directamente a repositorios en lugar de pasar por un servicio.

---

## Cómo se apoyan los servicios entre sí

![Cómo se apoyan los servicios entre sí](Imagenes/DiagramaComponentes_Negocio.png)

**Por qué está.** La capa de negocio es la que concentra las reglas. Esta vista muestra si están aisladas por componente o si un servicio depende de otro para cumplir su función.

**Qué muestra.** Son 30 componentes y 23 interfaces, con 28 conexiones. Aparece SesionUsuario, que es el Singleton del proyecto y el único estado global del sistema: lo consultan varias pantallas para saber el rol del usuario. PagoService figura marcada como excluida de la compilación, así que la gestión de pagos está incompleta a este nivel.

### Versión resumida: qué repositorio usa cada servicio

![Versión resumida: qué repositorio usa cada servicio](Imagenes/DiagramaComponentes_S3_Negocio.png)

**Por qué está.** La misma capa sin las operaciones de cada interfaz, para seguir de un vistazo el camino de un servicio hasta la base de datos.

**Qué muestra.** Arriba quedan los servicios que se apoyan unos en otros: PedidoFacade coordina a PedidoService, MesaService e InventarioService, y esos tres comparten repositorios. Abajo, los servicios que usan un único repositorio propio y no dependen de nadie más. El aviso marca las clases que el pom.xml excluye de la compilación.

---

## Qué tabla toca cada componente de datos

![Qué tabla toca cada componente de datos](Imagenes/DiagramaComponentes_Datos.png)

**Por qué está.** Sin ORM esta correspondencia solo existe dentro de las cadenas de SQL. Hacerla explícita es lo que permite saber el impacto de cambiar una tabla antes de cambiarla.

**Qué muestra.** Son 29 componentes y 16 tablas, con 36 conexiones, y cada flecha sale de una consulta real. Los tres DAO aparecen aquí junto a los repositorios porque hacen lo mismo: ejecutar SQL. Las tablas que solo toca un componente son las de bajo riesgo; las compartidas van en el diagrama siguiente.

---

## Las tablas que escriben varios componentes

![Las tablas que escriben varios componentes](Imagenes/DiagramaComponentes_Datos_Compartidos.png)

**Por qué está.** Una tabla escrita desde varios sitios es donde aparecen las inconsistencias, porque ningún componente ve lo que hacen los otros. Esta vista las aísla para poder vigilarlas.

**Qué muestra.** Son 11 tablas compartidas con 33 conexiones. El caso más expuesto es movimientos_inventario, escrita desde tres repositorios distintos. También lo están pedidos y mesas, que son justamente las dos entidades cuyos estados documentamos aparte.

---

## En qué archivo vive cada componente

![En qué archivo vive cada componente](Imagenes/DiagramaComponentes_Artefactos.png)

**Por qué está.** Las vistas anteriores hablan de componentes lógicos. Para trabajar hace falta la correspondencia con los archivos reales del repositorio, y esta es la vista que la da.

**Qué muestra.** Son 24 artefactos y 18 relaciones. Relaciona los paquetes de código, los .fxml, las hojas de estilo y los recursos con el componente al que pertenecen. Sirve de índice para localizar dónde se implementa cada pieza.

---

## Las tres piezas de las que depende medio sistema

![Las tres piezas de las que depende medio sistema](Imagenes/DiagramaComponentes_Infraestructura.png)

**Por qué está.** Hay componentes que no pertenecen a ninguna capa porque los usa todo el mundo. Son los que más cuesta cambiar, y por eso conviene saber exactamente quién los consume.

**Qué muestra.** ConexionBD abre todas las conexiones a PostgreSQL, SesionUsuario guarda quién ha iniciado sesión y TextoBusqueda normaliza las búsquedas. Entre las tres suman 31 consumidores. Cualquier cambio en su interfaz afecta a buena parte del sistema.

### Versión resumida: quién usa cada pieza compartida

![Versión resumida: quién usa cada pieza compartida](Imagenes/DiagramaComponentes_S4_Apoyo.png)

**Por qué está.** Las mismas tres piezas con los consumidores agrupados por capa en lugar de uno a uno, que a ese nivel son 31 flechas cruzando el dibujo.

**Qué muestra.** ConexionBD, SesionUsuario y TextoBusqueda, con el número de componentes de cada capa que las usan. ConexionBD es con diferencia la más usada, porque sin ORM cada repositorio abre su propia conexión.

---

## El código de la etapa con Spring que ya no compila

![El código de la etapa con Spring que ya no compila](Imagenes/DiagramaComponentes_Spring_Sin_Compilar.png)

**Por qué está.** El proyecto empezó con Spring y se unificó después en JavaFX con JDBC, según architecture.md. Parte de aquel código sigue en el repositorio. Documentarlo evita que alguien lo dé por funcional o intente usarlo.

**Qué muestra.** Son 12 artefactos. Seis clases importan Spring o Jakarta sin que el pom.xml declare esas dependencias, y el maven-compiler-plugin las excluye una por una de la compilación. Con ellas quedan fuera entidades y repositorios de pagos y meseros, que es la funcionalidad que falta completar.
