"""Arma la carpeta docs/ que se copia al repositorio.

Renderiza los 27 diagramas a JPG de doble resolucion con el nombre que usa la
convencion de docs/Imagenes, y escribe un .md por tipo de diagrama con la
justificacion y la explicacion de cada uno.

El commit del que salen los diagramas se lee del clon, no se escribe a mano.
"""
import os, re, subprocess, shutil, textwrap

REPO = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                      'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
JAR = os.environ.get('PLANTUML_JAR', '/home/claude/pl/package/vendor/plantuml.jar')
OUT = BASE + '/docs_entrega'
IMG = OUT + '/Imagenes'

COMMIT = subprocess.run(['git', '-C', REPO, 'log', '-1', '--format=%h'],
                        capture_output=True, text=True).stdout.strip()
COMMIT_LARGO = subprocess.run(['git', '-C', REPO, 'log', '-1', '--format=%H'],
                              capture_output=True, text=True).stdout.strip()
FECHA = subprocess.run(['git', '-C', REPO, 'log', '-1', '--format=%ad',
                        '--date=short'], capture_output=True, text=True).stdout.strip()
RAMA = 'develop'

# ====================================================== que se documenta ===
# (archivo .puml, nombre de la imagen, titulo, justificacion, explicacion)
CLASES = [
    ('puml_clases/01_entity_A.puml', 'DiagramaClases_Entidades_A',
     'Entidades del dominio (A): adicionales, inventario, mesas y clientes',
     'Las entidades son el vocabulario del sistema: los servicios, los '
     'repositorios y las pantallas se definen todos en términos de ellas, así '
     'que el modelo empieza por aquí. Este bloque cubre la parte del negocio '
     'que sostiene la carta y el salón.',
     'Hay {clases} clases y {interfaces} interfaz, con sus atributos y sus '
     'métodos. Se ve que un adicional se asocia a un detalle de pedido a '
     'través de DetallePedidoAdicional, que la asignación de una mesa es una '
     'entidad propia y no un campo de Mesa, y que las alertas de inventario se '
     'guardan en lugar de calcularse al vuelo. Junto a cada entidad aparece el '
     'repositorio que la maneja y, cuando existe, el servicio: la mayoría de '
     'estas entidades no tiene servicio propio y se usa directamente desde su '
     'repositorio.'),
    ('puml_clases/02_entity_B.puml', 'DiagramaClases_Entidades_B',
     'Entidades del dominio (B): mesas, historial e inventario',
     'Aquí está el núcleo del salón. La mesa es la entidad con más estados y '
     'más escrituras concurrentes del sistema, y entenderla es condición para '
     'leer el diagrama de estados y el de componentes de datos.',
     'Son {tipos} tipos. Mesa no guarda su estado como texto sino como clave '
     'hacia el catálogo estados_mesa, y los cambios que pasan por MesaService '
     'dejan una fila en HistorialEstadoMesa: el historial es parte del modelo, '
     'no una tabla de auditoría añadida aparte. No todos pasan por ahí, y el '
     'diagrama de estados de la mesa marca cuáles se saltan el historial. '
     'NotaMesa cuelga de la mesa con una nota por mesa como máximo. El enum '
     'TipoMovimiento solo tiene dos valores, VENTA_COMANDA y REVERSO_COMANDA: '
     'clasifica la salida de inventario que genera una comanda y su reverso.'),
    ('puml_clases/03_entity_C.puml', 'DiagramaClases_Entidades_C',
     'Entidades del dominio (C): pedidos, pagos, carta y reservas',
     'Cierra el modelo con el flujo que da sentido al resto: un pedido se '
     'abre sobre una mesa, se llena con productos de la carta y termina en un '
     'pago. Las dos enumeraciones que gobiernan ese flujo se declaran aquí.',
     'Son {tipos} tipos. EstadoPedido define los seis estados por los que pasa '
     'una comanda, y MetodoPago las formas de cobro. Pago no cuelga del pedido '
     'por composición sino que lo referencia, de modo que un pedido puede '
     'existir sin pago; eso sí, la entidad Pago es una de las que el pom.xml '
     'excluye de la compilación, así que el cobro que de verdad funciona no '
     'pasa por ella: lo resuelve PagoDAO escribiendo en la tabla pagos. '
     'Reserva '
     'apunta a la mesa y no al revés, y Zona agrupa las mesas del salón.'),
    ('puml_clases/04_controller.puml', 'DiagramaClases_Controladores',
     'Capa de presentación: los controladores JavaFX',
     'Cada pantalla de la aplicación es un controlador, así que este diagrama '
     'es el inventario de lo que el usuario puede hacer. Sirve para saber qué '
     'funcionalidad está construida y cuál sigue sin interfaz.',
     'Son {tipos} tipos y {flechas} relaciones. Hay siete pantallas con '
     'código: acceso, mapa de salón, carta del mesero, cocina, caja, '
     'inventario y disponibilidad. El octavo controlador, PagoController, es '
     'un archivo vacío que el pom.xml excluye de la compilación. El mapa de '
     'salón es con diferencia el controlador más '
     'grande, y declara dentro sus propias clases auxiliares (AccionMesa, '
     'ResumenMesas) y el enum EstadoMesaVisual, que traduce los códigos de la '
     'base a colores y etiquetas de pantalla.'),
    ('puml_clases/05_repository.puml', 'DiagramaClases_Repositorios',
     'Capa de acceso a datos: los repositorios',
     'La decisión de arquitectura fue JDBC sin ORM, lo que significa que cada '
     'repositorio escribe su propio SQL. Esa decisión concentra aquí el riesgo '
     'de inconsistencia, y por eso la capa se documenta completa.',
     'Son {tipos} tipos y {flechas} relaciones. Los repositorios que se '
     'ejecutan exponen métodos de consulta con nombre propio en lugar de un '
     'CRUD genérico, porque las consultas están escritas a mano. Tres '
     'interfaces '
     '(DetallePedidoRepository, MeseroRepository y PagoRepository) extienden '
     'JpaRepository y son el residuo de la etapa con Spring: el pom.xml las '
     'excluye de la compilación y no forman parte del sistema que se ejecuta.'),
    ('puml_clases/06_service_dao.puml', 'DiagramaClases_Servicios_DAO',
     'Capa de negocio: servicios y DAO',
     'Es la capa donde se decide si una operación es válida. Documentarla '
     'completa es lo que permite comprobar que las reglas viven en un solo '
     'sitio y no repartidas entre dos estilos de clase.',
     'Son {tipos} tipos y {flechas} relaciones. Conviven doce servicios y tres '
     'DAO (DisponibilidadDAO, PagoDAO y ProductoDAO) que hacen el mismo '
     'trabajo con otro nombre: los tres ejecutan SQL directamente, así que '
     'pertenecen a la capa de datos aunque estén en el paquete dao. '
     'PedidoFacade agrupa el flujo de pedido detrás de una sola interfaz, que '
     'es el patrón Facade aplicado en el proyecto.'),
    ('puml_clases/07_apoyo.puml', 'DiagramaClases_Apoyo',
     'Excepciones, enumeraciones, DTO y arranque',
     'Las excepciones definen el contrato de error del sistema: qué puede '
     'fallar y con qué nombre. Junto con las enumeraciones son el vocabulario '
     'que atraviesa todas las capas, y por eso se documentan juntas.',
     'Son {tipos} tipos. Las nueve excepciones propias nombran reglas de '
     'negocio concretas (MesaOcupadaException, PedidoNoEditableException, '
     'StockInsuficienteException), de modo que una regla rota se distingue de '
     'un fallo técnico. Los DTO transportan datos agregados que no '
     'corresponden a una sola entidad, como CuentaPago o '
     'ProductoDisponibilidad. Main es el punto de entrada JavaFX.'),
    ('puml_clases/08_rel_controller_capas.puml',
     'DiagramaClases_Relaciones_Controller_Capas',
     'Qué llama la presentación hacia abajo',
     'Una arquitectura en capas solo se sostiene si las llamadas respetan el '
     'orden. Este diagrama responde si la presentación pasa siempre por la '
     'capa de negocio o si atajaba hacia los datos.',
     'Son {tipos} tipos y {flechas} relaciones. La respuesta es que no siempre '
     'pasa: hay controladores que llaman directamente a repositorios y a DAO, '
     'saltándose los servicios. El mapa de salón es el caso más claro, y es el '
     'mismo salto que reaparece en el diagrama de estados de la mesa como '
     'escritura sin auditoría.'),
    ('puml_clases/09_rel_controller_modelo.puml',
     'DiagramaClases_Relaciones_Controller_Modelo',
     'Qué entidades manipula la presentación',
     'Complementa al anterior con la otra pregunta: además de a quién llama, '
     'qué datos del dominio toca cada pantalla directamente.',
     'Son {tipos} tipos y {flechas} relaciones. Los controladores construyen y '
     'modifican entidades por su cuenta, no solo DTO, y usan los enums del '
     'dominio para decidir qué pintar. Es lo que explica que la lógica de '
     'estado del pedido esté repartida entre el servicio y la pantalla de '
     'cocina.'),
    ('puml_clases/10_rel_service_capas.puml',
     'DiagramaClases_Relaciones_Service_Capas',
     'Qué usa la capa de negocio para llegar a los datos',
     'Es el camino normal de una operación: una pantalla pide algo a un '
     'servicio y el servicio lo resuelve contra la base. Este diagrama lo '
     'traza entero.',
     'Son {tipos} tipos y {flechas} relaciones. Siete de los doce servicios '
     'dependen de un único repositorio; el que más cruza es MesaService, con '
     'cuatro. Tres se apoyan en otros servicios además de, o en vez de, '
     'repositorios: PedidoService, ReservaService y PedidoFacade. PedidoFacade '
     'es el único que no toca ningún repositorio, y eso es lo que hace de él '
     'una fachada.'),
    ('puml_clases/11_rel_service_modelo.puml',
     'DiagramaClases_Relaciones_Service_Modelo',
     'Qué entidades construye y modifica cada servicio',
     'Permite saber quién es responsable de cada entidad: si dos servicios '
     'escriben la misma, hay una regla de negocio repartida en dos sitios.',
     'Son {tipos} tipos. Muestra con qué entidades y con qué enumeraciones '
     'trabaja cada servicio. EstadoPedido aparece en tres servicios y '
     'EstadoMesa solo en MesaService, que es donde se concentra el cambio de '
     'estado de la mesa. Las transiciones de los dos se documentan en su '
     'propio diagrama.'),
    ('puml_clases/12_rel_repository_capas.puml',
     'DiagramaClases_Relaciones_Repository_Capas',
     'Comprobación: los repositorios no llaman hacia arriba',
     'En una arquitectura en capas la de datos debe ser la última: puede ser '
     'llamada, pero no debe llamar a negocio ni a presentación. Este diagrama '
     'existe para verificarlo.',
     'Son {tipos} tipos y el resultado es que la comprobación se cumple: no '
     'hay flechas saliendo de la capa de datos hacia arriba. La única relación '
     'entre iguales es ProductoDAO hacia DisponibilidadDAO, dentro de la misma '
     'capa, y queda recogida en el diagrama de componentes de datos.'),
    ('puml_clases/13_rel_repository_modelo.puml',
     'DiagramaClases_Relaciones_Repository_Modelo',
     'Qué entidad lee y escribe cada repositorio',
     'Sin ORM, la correspondencia entre clase y tabla no está declarada en '
     'ninguna anotación: hay que deducirla del SQL. Este diagrama la deja '
     'escrita, y es lo que permite rastrear quién toca cada dato.',
     'Son {tipos} tipos y {flechas} relaciones. La mayoría de repositorios '
     'mapean una entidad cada uno, pero algunos leen varias para componer un '
     'resultado. Las tres relaciones de herencia que aparecen son las de las '
     'interfaces de Spring que no compilan.'),
    ('puml_clases/14_rel_modelo.puml', 'DiagramaClases_Relaciones_Modelo',
     'Cómo se relacionan las entidades entre sí',
     'Es el modelo conceptual del negocio, sin capas ni tecnología: qué '
     'pertenece a qué y con qué cardinalidad. Es la vista que hay que '
     'contrastar con el modelo de base de datos.',
     'Son {tipos} tipos y {flechas} relaciones, deducidas de los campos que '
     'declara cada entidad. Ninguna entidad guarda una colección: las líneas '
     'de un pedido no viven dentro de Pedido sino en DetallePedido, con la '
     'clave del pedido, y quien dice que no tienen vida propia es el ON DELETE '
     'CASCADE del esquema, no el código Java. Un pago, en cambio, referencia '
     'al pedido sin ser parte de él.'),
    ('puml_clases/15_pruebas.puml', 'DiagramaClases_Pruebas',
     'Qué cubren las pruebas automáticas',
     'Las pruebas son parte de la entrega y delimitan qué partes del sistema '
     'están verificadas. Este diagrama responde qué se prueba y qué no.',
     'Son {tipos} tipos y {flechas} relaciones: 10 clases de prueba en 7 '
     'archivos, con las clases de producción que ejercita cada una. La '
     'cobertura funcional se concentra en mesas y pedidos, pero no son los '
     'únicos que necesitan la base: de los siete archivos, cuatro se conectan '
     'a PostgreSQL (los dos módulos funcionales, el de disponibilidad de la '
     'HU-134 y el de choques de nombre de tabla). Los otros tres son pruebas '
     'unitarias de clases sueltas: CuentaPago, ItemPedido y TextoBusqueda. Las '
     'clases anidadas son los grupos de casos de JUnit.'),
]

# De estas cuatro solo se usa la imagen: van pegadas debajo del
# diagrama detallado del que son resumen, sin titulo y sin texto.
COMPONENTES_SIMPLE = [
    ('puml_componentes/09_Simple_Capas.puml', 'DiagramaComponentes_S1_Capas',
     'Las cinco capas',
     'La misma división en cinco grupos, sin interfaces ni nombres de clase, '
     'para quedarse solo con la dirección de las dependencias.',
     'Las cinco capas con el número de operaciones distintas que se invocan a '
     'través de cada dependencia. El flujo normal baja de interfaz a negocio, '
     'de negocio a datos y de datos a la conexión. Las dos flechas que no '
     'siguen ese orden son los saltos de capa: la interfaz llamando '
     'directamente a datos y el negocio llamando directamente a la conexión.'),
    ('puml_componentes/10_Simple_UI.puml', 'DiagramaComponentes_S2_UI',
     'Qué usa cada pantalla',
     'Lo mismo sin los archivos ni las operaciones: una caja por pantalla y '
     'una flecha por dependencia.',
     'Las ocho pantallas con los componentes que llaman, coloreados por capa. '
     'La mayoría depende de una o dos piezas, y el mapa de salón y la carta '
     'del mesero son las que más acumulan. También se ve qué pantallas llaman '
     'directamente a repositorios en lugar de pasar por un servicio.'),
    ('puml_componentes/11_Simple_Negocio.puml',
     'DiagramaComponentes_S3_Negocio',
     'Qué repositorio usa cada servicio',
     'La misma capa sin las operaciones de cada interfaz, para seguir de un '
     'vistazo el camino de un servicio hasta la base de datos.',
     'Arriba quedan los servicios que se apoyan unos en otros: PedidoFacade '
     'coordina a PedidoService, MesaService e InventarioService, y esos tres '
     'comparten repositorios. Abajo, los servicios que usan un único '
     'repositorio propio y no dependen de nadie más. El aviso marca las '
     'clases que el pom.xml excluye de la compilación.'),
    ('puml_componentes/12_Simple_Apoyo.puml',
     'DiagramaComponentes_S4_Apoyo',
     'Quién usa cada pieza compartida',
     'Las mismas tres piezas con los consumidores agrupados por capa en lugar '
     'de uno a uno, que a ese nivel son 31 flechas cruzando el dibujo.',
     'ConexionBD, SesionUsuario y TextoBusqueda, con el número de componentes '
     'de cada capa que las usan. ConexionBD es con diferencia la más usada, '
     'porque sin ORM cada repositorio abre su propia conexión.'),
    ('puml_componentes/13_Simple_Datos.puml',
     'DiagramaComponentes_S5_Datos',
     'Qué tabla toca cada componente de datos',
     'resumen', 'resumen'),
    ('puml_componentes/14_Simple_Artefactos.puml',
     'DiagramaComponentes_S6_Artefactos',
     'En qué carpeta vive cada componente',
     'resumen', 'resumen'),
]

COMPONENTES = [
    ('puml_componentes/01_Componentes_general.puml',
     'DiagramaComponentes_General', 'Los cinco grupos y su dependencia',
     'Un componente es una pieza que se puede sustituir si se respeta su '
     'interfaz. Esta vista responde en cuántas piezas está dividido GastroFlow '
     'y en qué orden dependen unas de otras.',
     'El sistema se agrupa en interfaz, negocio, datos, conexión y apoyo. Las '
     'dependencias van en una sola dirección salvo las que atraviesan hacia '
     'datos desde la interfaz. Las interfaces provistas llevan el nombre real '
     'de la clase que las implementa: se dedujeron de las llamadas que cruzan '
     'el límite entre un grupo y otro, no de un diseño sobre el papel.'),
    ('puml_componentes/02_Componentes_ui.puml', 'DiagramaComponentes_UI',
     'Qué necesita cada pantalla para funcionar',
     'Antes de tocar una pantalla conviene saber de qué depende. Esta vista lo '
     'resuelve pantalla por pantalla, incluidos los recursos que no son '
     'código.',
     'Son {componentes} componentes y {interfaces} interfaces consumidas, con '
     '{flechas} conexiones. Cada componente agrupa los archivos de su '
     'pantalla: el .fxml, el controlador y, cuando la tiene, su hoja de '
     'estilos. Inventario y el mapa de salón no tienen hoja propia, y Main no '
     'tiene .fxml porque arma su interfaz en código. Las hojas de estilo las carga Main y no cada '
     'controlador, que es una dependencia fácil de pasar por alto.'),
    ('puml_componentes/03_Componentes_negocio.puml',
     'DiagramaComponentes_Negocio', 'Cómo se apoyan los servicios entre sí',
     'La capa de negocio es la que concentra las reglas. Esta vista muestra si '
     'están aisladas por componente o si un servicio depende de otro para '
     'cumplir su función.',
     'Son {componentes} componentes y {interfaces} interfaces, con {flechas} '
     'conexiones. Aparece SesionUsuario, que es el Singleton del proyecto y el '
     'único estado global del sistema: lo consultan el arranque y dos '
     'pantallas, casi siempre para saber si hay sesión y quién es; el rol solo '
     'se mira en Main y en la pantalla de caja. PagoService figura marcada '
     'como excluida de la compilación, así que el cobro funciona sin capa de '
     'servicio: la pantalla de caja escribe directamente por PagoDAO.'),
    ('puml_componentes/04_Componentes_datos.puml', 'DiagramaComponentes_Datos',
     'Qué tabla toca cada componente de datos',
     'Sin ORM esta correspondencia solo existe dentro de las cadenas de SQL. '
     'Hacerla explícita es lo que permite saber el impacto de cambiar una '
     'tabla antes de cambiarla.',
     'Son {componentes} componentes y {tablas} tablas, con {flechas} '
     'conexiones, y cada flecha sale de una consulta real. Los tres DAO '
     'aparecen aquí junto a los repositorios porque hacen lo mismo: ejecutar '
     'SQL. Las tablas que solo toca un componente son las de bajo riesgo; las '
     'compartidas van en el diagrama siguiente.'),
    ('puml_componentes/05_Componentes_datos_compartidos.puml',
     'DiagramaComponentes_Datos_Compartidos',
     'Las tablas que tocan varios componentes',
     'Una tabla escrita desde varios sitios es donde aparecen las '
     'inconsistencias, porque ningún componente ve lo que hacen los otros. '
     'Esta vista aísla las que tocan dos o más componentes para poder '
     'vigilarlas.',
     'Son {tablas} tablas compartidas con {flechas} conexiones. Solo se leen '
     '{n_solo_lectura} de ellas ({solo_lectura}), así que el riesgo está en el '
     'resto. Las más expuestas son {mas_escritas}, con {n_mas_escritas} '
     'componentes escribiendo en cada una; productos además recibe escrituras '
     'desde dos capas distintas, un repositorio y dos servicios. En mesas '
     'escribe un único componente aunque la consulten otros dos.'),
    ('puml_componentes/06_Componentes_artefactos.puml',
     'DiagramaComponentes_Artefactos',
     'En qué archivo vive cada componente',
     'Las vistas anteriores hablan de componentes lógicos. Para trabajar hace '
     'falta la correspondencia con los archivos reales del repositorio, y esta '
     'es la vista que la da.',
     'Son {artefactos} artefactos y {flechas} relaciones. Relaciona los '
     'paquetes de código, los .fxml, las hojas de estilo y los recursos con el '
     'componente al que pertenecen. Sirve de índice para localizar dónde se '
     'implementa cada pieza.'),
    ('puml_componentes/07_Componentes_infraestructura.puml',
     'DiagramaComponentes_Infraestructura',
     'Las tres piezas compartidas y quién las usa',
     'Hay tres componentes que no pertenecen a ninguna capa. Conviene saber '
     'quién consume cada uno antes de tocarlo, porque el reparto es muy '
     'desigual y desde fuera no se adivina.',
     'ConexionBD abre todas las conexiones a PostgreSQL, SesionUsuario guarda '
     'quién ha iniciado sesión y TextoBusqueda normaliza las búsquedas. Entre '
     'las tres suman {flechas} consumidores, pero muy mal repartidos: '
     '{reparto}. Cambiar ConexionBD toca casi todo el sistema; cambiar '
     'TextoBusqueda toca una sola pantalla.'),
    ('puml_componentes/08_Componentes_spring_sin_compilar.puml',
     'DiagramaComponentes_Spring_Sin_Compilar',
     'El código de la etapa con Spring que ya no compila',
     'architecture.md cuenta que quedó código residual con anotaciones Spring '
     'y que el proyecto se unificó bajo JavaFX con JDBC para no mantener dos '
     'arquitecturas mezcladas. Ese código sigue en el repositorio. '
     'Documentarlo evita que alguien lo dé por funcional o intente usarlo.',
     'Son {artefactos} artefactos. Seis clases importan Spring o Jakarta sin '
     'que el pom.xml declare esas dependencias, y el maven-compiler-plugin '
     'excluye {n_excludes} archivos uno por uno: esos seis más '
     'controller/PagoController.java, que está vacío. Quedan fuera las '
     'entidades y los repositorios de pagos y meseros, y también '
     'DetallePedidoRepository, el único sitio que tocaría la tabla '
     'detalle_pedido.'),
]

ARQUITECTURA = [
    ('puml/01_Arquitectura.puml', 'DiagramaArquitectura_Capas',
     'Organización del código en capas y dependencias entre ellas',
     'Responde, en una sola vista, cómo está organizado el código y qué '
     'depende de qué, sin tener que recorrer los archivos uno por uno. '
     'Comprueba además si el código cumple la decisión registrada en '
     'architecture.md: JavaFX con JDBC y sin Spring.',
     'Son {elementos} elementos y {flechas} dependencias: cuatro entre capas '
     'contiguas, dos saltos de capa, tres hacia el modelo y tres desde las '
     'pruebas. Los paquetes de '
     'src/main/java se agrupan en presentación, negocio, datos e '
     'infraestructura, con los recursos y PostgreSQL. Los tres DAO están en la '
     'capa de datos y no en la de negocio, porque ejecutan SQL igual que un '
     'repositorio. El diagrama no esconde los incumplimientos: marca los '
     'saltos de capa que el código comete y cuántas veces ocurre cada uno, '
     'sobre todo desde presentación hacia datos.'),
]

DESPLIEGUE = [
    ('puml/06_Despliegue.puml', 'DiagramaDespliegue_Entorno',
     'Cómo se instala y se ejecuta el sistema hoy',
     'Responde qué hace falta para poner GastroFlow en funcionamiento. No '
     'dibuja servidores de aplicaciones porque no existen: el pom.xml no '
     'declara ningún plugin de empaquetado, así que el sistema no produce un '
     'instalable. Lo que sí existe, y es lo que documenta, son el entorno de '
     'desarrollo y el de integración continua.',
     'Son {nodos} nodos y {artefactos} artefactos. Para ejecutarlo hacen falta '
     'JDK 21, Maven 3.9 o superior y PostgreSQL 16, con las dependencias que '
     'declara el pom (JavaFX 21.0.5, el driver postgresql 42.7.5 y jbcrypt '
     '0.4). La base se prepara ejecutando scripts/setup.sh, que aplica en '
     'orden los archivos de db/, y el arranque es mvn '
     'clean compile seguido de mvn javafx:run, contra la base en '
     'localhost:5432. GitHub se dibuja aparte, como servicio externo y no '
     'como nodo de '
     'despliegue: de ahí se descarga el código con git clone y la '
     'instalación ocurre entera en el equipo. Su runner ejecuta los pasos '
     'declarados en .github/workflows/ci.yml, que compilan y prueban para '
     'comprobar que el proyecto está sano, pero no producen nada que se '
     'instale.'),
]

ESTADOS = [
    ('puml/08_Estados_Pedido.puml', 'DiagramaEstados_Pedido',
     'Estados por los que pasa un pedido y transiciones válidas',
     'El estado de una comanda decide qué se puede hacer con ella: si se puede '
     'editar, cobrar o cancelar. Ese estado se cambia desde varios puntos del '
     'código, así que sin esta vista no hay forma de saber qué transiciones '
     'son válidas salvo leyéndolo entero.',
     '{estados} estados y {transiciones} transiciones. Un pedido nace '
     'PENDIENTE al abrirse sobre una mesa, avanza por cocina hasta COMPLETADO, '
     'y termina en ENTREGADO o CANCELADO, que son los dos estados cerrados. '
     'Cerrar y cancelar se pueden hacer desde cualquier estado activo: el '
     'código no mira en cuál está, solo que no sea uno de los dos finales. La '
     'tabla da por cada transición el origen, el destino, la operación que la '
     'ejecuta y la condición que debe cumplirse. ASIGNADO_MESA está en gris '
     'porque ningún código lo asigna, y las flechas azules señalan los cambios '
     'que se escriben sin pasar por PedidoService y por tanto sin la '
     'comprobación de estado cerrado.'),
    ('puml/09_Estados_Mesa.puml', 'DiagramaEstados_Mesa',
     'Estados por los que pasa una mesa y transiciones válidas',
     'La mesa es el recurso que el salón reparte, y su estado determina si se '
     'puede sentar a alguien. A diferencia del pedido, su estado no se guarda '
     'en un enum de Java sino como una fila de la tabla estados_mesa, y se '
     'escribe desde cuatro métodos de MesaRepository a los que se llega por '
     'varios caminos. Existe un enum EstadoMesa en el código, pero no lo usa '
     'nadie.',
     '{estados_catalogo} estados de catálogo y {transiciones} transiciones. La '
     'conclusión principal es que el sistema casi no restringe los cambios: '
     'salvo la asignación de comensales, que exige en su propio SQL que la '
     'mesa esté libre, ninguna transición consulta el estado actual, por eso '
     'las demás salen del bloque que agrupa todo el catálogo. MANTENIMIENTO '
     'está en el catálogo y ningún código lo asigna. LIBRE aparece en rojo '
     'porque el código lo pide y el catálogo no lo tiene. Las flechas azules '
     'escriben directamente en la tabla sin pasar por MesaService, de modo que '
     'esos cambios no quedan registrados en historial_estado_mesa.'),
]

# Las cuatro vistas simples no van en un archivo aparte: cada una se coloca
# justo debajo del diagrama detallado del que es resumen, como subseccion.
DEBAJO_DE = {
    'DiagramaComponentes_General': 'DiagramaComponentes_S1_Capas',
    'DiagramaComponentes_UI': 'DiagramaComponentes_S2_UI',
    'DiagramaComponentes_Negocio': 'DiagramaComponentes_S3_Negocio',
    'DiagramaComponentes_Infraestructura': 'DiagramaComponentes_S4_Apoyo',
    'DiagramaComponentes_Datos': 'DiagramaComponentes_S5_Datos',
    'DiagramaComponentes_Artefactos': 'DiagramaComponentes_S6_Artefactos',
}
POR_NOMBRE = {d[1]: d for d in COMPONENTES_SIMPLE}
COMPONENTES_MEZCLA = []
for d in COMPONENTES:
    COMPONENTES_MEZCLA.append(d)
    if d[1] in DEBAJO_DE:
        # la marca final hace que se escriba con ### en vez de ##
        COMPONENTES_MEZCLA.append(POR_NOMBRE[DEBAJO_DE[d[1]]] + ('sub',))
assert len(COMPONENTES_MEZCLA) == len(COMPONENTES) + len(COMPONENTES_SIMPLE)

SECCIONES = [
    ('diagramaClases.md', 'Diagramas de clases', CLASES,
     'Documentan el modelo de clases del sistema: {n_tipos} tipos, {n_attr} '
     'atributos y {n_met} métodos. Están organizados siguiendo la arquitectura '
     'en capas del proyecto: primero las entidades del dominio, después cada '
     'capa con sus miembros, y al final las relaciones, separadas por origen y '
     'destino para que cada vista responda una sola pregunta.'),
    ('diagramaComponentes.md', 'Diagramas de componentes',
     COMPONENTES_MEZCLA,
     'Documentan en qué piezas sustituibles está dividido el sistema y cómo '
     'dependen unas de otras, con las operaciones que cada componente provee '
     'y los archivos que lo forman. Las interfaces no son un diseño sobre el '
     'papel: cada una corresponde a las llamadas que de verdad cruzan el '
     'límite entre dos componentes. Seis de las ocho vistas llevan debajo una '
     'versión resumida, con una caja por componente y una flecha por '
     'dependencia, para leerlas de un vistazo.'),
    ('diagramaArquitectura.md', 'Diagrama de arquitectura', ARQUITECTURA,
     'Documenta en una sola vista cómo está organizado el código y qué depende '
     'de qué, y contrasta el resultado con la decisión de arquitectura '
     'registrada en architecture.md.'),
    ('diagramaDespliegue.md', 'Diagrama de despliegue', DESPLIEGUE,
     'Documenta cómo se instala y se ejecuta el sistema hoy, y qué hace falta '
     'para ponerlo en funcionamiento.'),
    ('diagramaEstados.md', 'Diagramas de estados', ESTADOS,
     'Documentan los estados por los que pasan un pedido y una mesa, y qué '
     'transiciones son válidas entre ellos. Cada flecha lleva un número y el '
     'detalle de cada transición está en la tabla que acompaña al diagrama.'),
]

# la direccion y el color pueden ir en cualquier orden: -up[#X]..> y
# -[#X]down-> son los dos validos
FLECHA = re.compile(
    r'^\s*[\w"\[\]\*\.]+\s+[-.]{1,2}'
    r'(?:\[[^\]]*\])?(?:up|down|left|right)?(?:\[[^\]]*\])?'
    r'[-.]{0,2}[|o\*]?>', re.M)


_EXTRA = {}


def extras():
    """Los datos que una frase cita por su nombre y no solo por su numero.

    Salen de los propios .puml y del pom.xml, no se escriben a mano: si
    manana otra tabla pasa a ser la mas escrita, el texto cambia solo en
    lugar de quedarse mintiendo.
    """
    if _EXTRA:
        return _EXTRA
    import collections
    p5 = open(f'{BASE}/puml_componentes/05_Componentes_datos_compartidos.puml',
              encoding='utf-8').read()
    esc = {t: int(n) for t, n in
           re.findall(r'database "(\w+)\\n<size:\d+>escriben (\d+)', p5)}
    lec = re.findall(r'database "(\w+)\\n<size:\d+>solo lectura', p5)
    tope = max(esc.values()) if esc else 0
    _EXTRA['mas_escritas'] = ' y '.join(sorted(t for t, n in esc.items()
                                               if n == tope))
    _EXTRA['n_mas_escritas'] = tope
    _EXTRA['solo_lectura'] = ', '.join(sorted(lec))
    _EXTRA['n_solo_lectura'] = len(lec)
    p7 = open(f'{BASE}/puml_componentes/07_Componentes_infraestructura.puml',
              encoding='utf-8').read()
    cons = collections.Counter(re.findall(r'\.\.> I_(\w+) ', p7))
    _EXTRA['reparto'] = ', '.join(f'{n} de {k}' for k, n in cons.most_common())
    _EXTRA['n_excludes'] = len(re.findall(
        r'<exclude>', open(f'{REPO}/pom.xml', encoding='utf-8').read()))
    return _EXTRA


def conteo(ruta):
    """Los numeros que el texto cita, leidos del propio .puml."""
    s = open(f'{BASE}/{ruta}', encoding='utf-8').read()
    c = dict(
        clases=len(re.findall(r'^\s*(?:abstract )?class\s', s, re.M)),
        interfaces=len(re.findall(r'^\s*interface\s', s, re.M)),
        enums=len(re.findall(r'^\s*enum\s', s, re.M)),
        nodos=len(re.findall(r'^\s*node\s', s, re.M)),
        artefactos=len(re.findall(r'^\s*artifact\s', s, re.M)),
        componentes=len(re.findall(r'^\s*component\s', s, re.M)),
        tablas=len(re.findall(r'^\s*database\s', s, re.M)),
        estados=len(re.findall(r'^\s*state\s+"[^"]*"\s+as\s+\w+\s*(?:#\w+)?\s*$',
                               s, re.M)),
        transiciones=len(re.findall(r'^\|\s*<b>T\d+</b>', s, re.M)),
        # los de dentro del bloque compuesto: para la mesa son los del
        # catalogo, sin los huerfanos ni los que solo existen en la interfaz
        estados_catalogo=len(re.findall(
            r'^    state\s+"[^"]*"\s+as\s+\w+', s, re.M)),
        flechas=len([x for x in FLECHA.findall(s) if 'hidden' not in x]),
    )
    c['tipos'] = c['clases'] + c['interfaces'] + c['enums']
    c['elementos'] = c['tipos'] + c['nodos'] + c['artefactos'] + \
        c['componentes'] + c['tablas']
    c.update(extras())
    return c


def render():
    os.makedirs(IMG, exist_ok=True)
    tmp = OUT + '/.tmp'
    os.makedirs(tmp, exist_ok=True)
    todos = (CLASES + COMPONENTES_SIMPLE + COMPONENTES + ARQUITECTURA
             + DESPLIEGUE + ESTADOS)
    for ruta, nombre, *_ in todos:
        src = f'{BASE}/{ruta}'
        txt = open(src, encoding='utf-8').read()
        txt = txt.replace('@startuml', '@startuml\nskinparam dpi 96', 1)
        open(f'{tmp}/{nombre}.puml', 'w', encoding='utf-8').write(txt)
    r = subprocess.run('java -DPLANTUML_LIMIT_SIZE=32768 -jar ' + JAR
                       + ' -tpng ./*.puml', shell=True, cwd=tmp,
                       capture_output=True, text=True)
    err = '\n'.join(l for l in (r.stderr or '').split('\n')
                     if l and 'Picked up' not in l)
    if err:
        print('  plantuml:', err[:400])
    for ruta, nombre, *_ in todos:
        png = f'{tmp}/{nombre}.png'
        if not os.path.exists(png):
            print(f'  NO SE RENDERIZO: {nombre}')
            continue
        # PNG y no JPG: son dibujos de lineas y texto, donde PNG es sin
        # perdida y ademas pesa unas tres veces menos que un JPG de calidad
        # equivalente
        shutil.copy(png, f'{IMG}/{nombre}.png')
    # los .puml fuente van con la entrega: sin ellos el diagrama no se
    # puede regenerar ni auditar
    src_dir = OUT + '/diagramas-fuente'
    os.makedirs(src_dir, exist_ok=True)
    for ruta, nombre, *_ in todos:
        shutil.copy(f'{BASE}/{ruta}', f'{src_dir}/{nombre}.puml')
    # la herramienta que este documento manda ejecutar tiene que viajar con
    # el: si no, la instruccion de regenerar no se puede cumplir
    her = OUT + '/diagramas-fuente/herramientas'
    os.makedirs(her, exist_ok=True)
    sh = open(f'{BASE}/rehacer.sh').read()
    usados = re.findall(r'\$AQUI/([\w\.]+)\.py', sh)
    # los que el script recorre en un bucle no llevan el nombre en la ruta
    for bucle in re.findall(r'for g in ([\w\s]+); do', sh):
        usados += bucle.split()
    for nombre_py in sorted(set(usados)):
        shutil.copy(f'{BASE}/{nombre_py}.py', f'{her}/{nombre_py}.py')
    shutil.copy(f'{BASE}/rehacer.sh', f'{her}/rehacer.sh')
    os.chmod(f'{her}/rehacer.sh', 0o755)
    print(f'  herramientas: {len(set(usados)) + 1} archivos')
    shutil.rmtree(tmp)
    return len(todos)


def escribir():
    # los totales del intro tambien salen del codigo, no se escriben a mano
    import json
    T = json.load(open(BASE + '/clases.json', encoding='utf-8'))
    tot = {'n_tipos': len(T),
           'n_attr': sum(len(t['campos']) for t in T),
           'n_met': sum(len(t['metodos']) for t in T)}
    for archivo, titulo, lista, intro in SECCIONES:
        L = [f'# {titulo}', '', intro.format(**tot), '', '---', '']
        for i, item in enumerate(lista):
            ruta, nombre, tit, just, expl = item[:5]
            sub = len(item) > 5          # la version resumida del anterior
            if sub:
                # solo la imagen, pegada al diagrama del que es resumen: sin
                # titulo y sin texto, que es justo lo que sobraba
                L += [f'![{tit}](Imagenes/{nombre}.png)', '']
                continue
            c = conteo(ruta)
            L += [f'## {tit}', '',
                  f'![{tit}](Imagenes/{nombre}.png)', '',
                  '**Por qué está.** ' + just.format(**c), '',
                  '**Qué muestra.** ' + expl.format(**c), '']
            # la linea divisoria no va entre un diagrama y su resumen, que
            # son una sola cosa leida a dos niveles
            if not (i + 1 < len(lista) and len(lista[i + 1]) > 5):
                L += ['---', '']
        while L and L[-1] in ('', '---'):
            L.pop()
        open(f'{OUT}/{archivo}', 'w', encoding='utf-8').write('\n'.join(L) + '\n')
        print(f'  {archivo}  ({len(lista)} diagramas)')


if __name__ == '__main__':
    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(OUT, exist_ok=True)
    n = render()
    escribir()
    # los archivos escritos a mano viven fuera de la carpeta que este script
    # borra al empezar, y se copian al final
    manual = BASE + '/docs_manual'
    if os.path.isdir(manual):
        for f in sorted(os.listdir(manual)):
            shutil.copy(f'{manual}/{f}', f'{OUT}/{f}')
            print(f'  {f}  (escrito a mano)')
    print(f'\n{n} imagenes en {IMG}')
    print(f'commit documentado: {COMMIT}  ({FECHA})')
