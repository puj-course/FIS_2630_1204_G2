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
     'Entidades A — de Adicional a EstadoMesa',
     'El modelo tiene 98 tipos. Dibujarlos juntos produce una lámina que no se '
     'puede leer ni imprimir, así que partimos las entidades en tres láminas '
     'por orden alfabético. Esta primera cubre el primer tercio y existe para '
     'poder consultar los atributos y las operaciones de cada entidad sin ir a '
     'buscarlas al código.',
     'Contiene {clases} clases y {interfaces} interfaz. Están las entidades del dominio de '
     'adicionales, inventario, asignación de mesas y clientes, cada una con sus '
     'atributos y sus métodos tal como los declara el código. Al lado de cada '
     'entidad aparecen los repositorios y servicios que la manejan, para que la '
     'lámina se lea sin saltar a otra.'),
    ('puml_clases/02_entity_B.puml', 'DiagramaClases_Entidades_B',
     'Entidades B — de HistorialEstadoMesa a MovimientoInventario',
     'Segundo tercio del modelo, con el mismo criterio de corte que la lámina '
     'anterior: mantener cada una por debajo del tamaño en el que las cajas '
     'dejan de ser legibles.',
     'Son {tipos} tipos: {clases} clases, la interfaz MeseroRepository y el enum '
     'TipoMovimiento. Aquí está el núcleo de mesas (Mesa, HistorialEstadoMesa, '
     'NotaMesa) y el de inventario (Ingrediente, MovimientoInventario), con los '
     'controladores de cocina y mapa de salón que los consumen.'),
    ('puml_clases/03_entity_C.puml', 'DiagramaClases_Entidades_C',
     'Entidades C — de Pago a Zona',
     'Último tercio del modelo. Cierra el recorrido por las entidades y recoge '
     'las dos enumeraciones que gobiernan el flujo del pedido y el del pago.',
     'Son {tipos} tipos. Incluye Pedido y Pago, los enums EstadoPedido y MetodoPago, '
     'la interfaz PagoRepository, y las entidades de carta y salón (Plato, '
     'Producto, Zona, Reserva). EstadoPedido es la base del diagrama de estados '
     'que documentamos aparte.'),
    ('puml_clases/04_controller.puml', 'DiagramaClases_Controladores',
     'Controladores JavaFX',
     'La capa de presentación es la que más cambia entre entregas y la que más '
     'preguntas recibe en las revisiones. Separarla en su propia lámina permite '
     'ver de un vistazo qué pantalla existe y qué operaciones expone cada una.',
     'Son {tipos} tipos y {flechas} relaciones. Están los ocho controladores JavaFX '
     '(Login, MapaSalon, MenuMesero, Cocina, CajeroPago, Inventario y '
     'DisponibilidadAdmin) con sus métodos @FXML, más las clases auxiliares que '
     'declaran dentro (AccionMesa, ResumenMesas) y los enums que solo ellos '
     'usan, como EstadoMesaVisual.'),
    ('puml_clases/05_repository.puml', 'DiagramaClases_Repositorios',
     'Repositorios',
     'El proyecto no usa ORM: cada repositorio escribe su propio SQL. Esta '
     'lámina existe para poder revisar de una vez las clases que tocan la base '
     'de datos, que es donde se concentran los errores de consulta.',
     'Son {tipos} tipos y {flechas} relaciones. Además de los repositorios aparecen las '
     'tres interfaces que extienden JpaRepository y que el pom.xml excluye de '
     'la compilación (DetallePedidoRepository, MeseroRepository y '
     'PagoRepository), señaladas como tales, y los enums que las consultas usan '
     'como parámetro.'),
    ('puml_clases/06_service_dao.puml', 'DiagramaClases_Servicios_DAO',
     'Servicios y DAO',
     'La lógica de negocio vive repartida entre service y dao. Verlas juntas es '
     'lo único que deja comprobar que no haya reglas duplicadas en las dos '
     'capas, que fue uno de los riesgos que detectamos al unificar la '
     'arquitectura.',
     'Son {tipos} tipos y {flechas} relaciones. Están los doce servicios y los tres DAO '
     '(DisponibilidadDAO, PagoDAO y ProductoDAO), con PedidoFacade como fachada '
     'del flujo de pedido. Los tres DAO ejecutan SQL directamente, igual que un '
     'repositorio, y por eso en el diagrama de arquitectura los situamos en la '
     'capa de datos y no en la de negocio.'),
    ('puml_clases/07_apoyo.puml', 'DiagramaClases_Apoyo',
     'Clases de apoyo: excepciones, enums, DTO y utilidades',
     'Las excepciones y los enums no pertenecen a ninguna capa y ensucian '
     'cualquier lámina en la que se metan. Agruparlos aparte mantiene limpias '
     'las otras y da un sitio único donde consultar qué excepción lanza el '
     'sistema y qué valores admite cada enumeración.',
     'Son {tipos} tipos. Incluye las nueve excepciones propias, los seis enums del '
     'dominio, los DTO (IngredienteStockDTO, ProductoDisponibilidad, CuentaPago '
     'e ItemPedido), la clase de conexión y Main.'),
    ('puml_clases/08_rel_controller_capas.puml',
     'DiagramaClases_Relaciones_Controller_Capas',
     'Relaciones: controladores hacia las otras capas',
     'En las láminas anteriores las relaciones se omiten para que las cajas se '
     'lean. Esta serie de siete mapas las recupera, una capa por lámina, que es '
     'la única forma de que las líneas no se crucen hasta volverse '
     'indistinguibles.',
     'Muestra a qué servicios, DAO y repositorios llama cada controlador. Son '
     '{tipos} tipos y {flechas} relaciones. Permite ver qué controladores se '
     'saltan la capa '
     'de servicio y acceden directamente a datos, que es un hallazgo que '
     'confirmamos en el código.'),
    ('puml_clases/09_rel_controller_modelo.puml',
     'DiagramaClases_Relaciones_Controller_Modelo',
     'Relaciones: controladores hacia el modelo',
     'Separada de la anterior porque mezclar las llamadas a otras capas con el '
     'uso de entidades volvía la lámina ilegible.',
     'Son {tipos} tipos y {flechas} relaciones. Muestra qué entidades y qué enums manipula '
     'cada controlador directamente, sin pasar por un servicio.'),
    ('puml_clases/10_rel_service_capas.puml',
     'DiagramaClases_Relaciones_Service_Capas',
     'Relaciones: servicios hacia las otras capas',
     'Mismo criterio: una lámina por origen y por destino. Esta es la que '
     'muestra el camino normal de una operación de negocio hacia la base de '
     'datos.',
     'Son {tipos} tipos y {flechas} relaciones: cada servicio con los repositorios y DAO '
     'que usa. PedidoFacade aparece aquí como el único punto que agrupa varios '
     'servicios, que es el patrón Facade que ya teníamos implementado.'),
    ('puml_clases/11_rel_service_modelo.puml',
     'DiagramaClases_Relaciones_Service_Modelo',
     'Relaciones: servicios hacia el modelo',
     'Completa la vista anterior con las entidades que cada servicio construye '
     'o modifica.',
     'Son {tipos} tipos. Muestra qué entidades toca cada servicio y con qué enums '
     'trabaja, entre ellos EstadoPedido y EstadoMesa.'),
    ('puml_clases/12_rel_repository_capas.puml',
     'DiagramaClases_Relaciones_Repository_Capas',
     'Relaciones: repositorios hacia las otras capas',
     'Un repositorio no debería llamar a otra capa. Esta lámina existe '
     'precisamente para poder comprobarlo de un vistazo.',
     'Son {tipos} tipos. La lámina está prácticamente vacía de líneas, y ese es el '
     'resultado: los repositorios no llaman hacia arriba. La única relación '
     'entre iguales que sí existe, de ProductoDAO hacia DisponibilidadDAO, la '
     'recogemos en la lámina de la capa de datos.'),
    ('puml_clases/13_rel_repository_modelo.puml',
     'DiagramaClases_Relaciones_Repository_Modelo',
     'Relaciones: repositorios hacia el modelo',
     'Es el mapa de qué entidad lee y escribe cada repositorio, que es lo que '
     'hace falta para rastrear quién toca cada tabla.',
     'Son {tipos} tipos y {flechas} relaciones, entre ellas las de herencia hacia JpaRepository. Junto con el '
     'diagrama de componentes de datos permite detectar tablas escritas desde '
     'más de un repositorio.'),
    ('puml_clases/14_rel_modelo.puml', 'DiagramaClases_Relaciones_Modelo',
     'Relaciones dentro del modelo',
     'Las relaciones entre entidades son las que sostienen el modelo de datos, '
     'y conviene verlas sin el ruido de las otras capas.',
     'Son {tipos} tipos y {flechas} relaciones: composiciones, agregaciones y asociaciones '
     'entre entidades, con la multiplicidad deducida del tipo declarado de cada '
     'campo. Solo las composiciones llevan multiplicidad en el extremo de '
     'origen, porque es la única que el código justifica.'),
    ('puml_clases/15_pruebas.puml', 'DiagramaClases_Pruebas',
     'Clases de prueba',
     'Las pruebas son parte de la entrega y tienen sus propias dependencias. '
     'Documentarlas aparte evita mezclarlas con el código de producción en las '
     'otras láminas.',
     'Son {tipos} tipos y {flechas} relaciones: 10 clases de prueba repartidas en 7 '
     'archivos, con las clases de producción que cada una ejercita. Incluye las '
     'clases anidadas que JUnit usa para agrupar casos.'),
]

COMPONENTES = [
    ('puml_componentes/01_Componentes_general.puml',
     'DiagramaComponentes_General', 'Vista general',
     'Es el punto de entrada al resto de la serie. Un solo diagrama con los 49 '
     'componentes resultaba ilegible, así que esta lámina da la vista de '
     'conjunto y las siete siguientes la desarrollan por capas.',
     'Muestra los cinco grupos de componentes (interfaz, negocio, datos, '
     'conexión y apoyo) y las dependencias entre grupos. Las interfaces '
     'provistas no llevan nombres inventados: cada una se dedujo de las '
     'llamadas que de verdad cruzan el límite entre un grupo y otro, medidas '
     'sobre el código.'),
    ('puml_componentes/02_Componentes_ui.puml', 'DiagramaComponentes_UI',
     'Capa de interfaz de usuario',
     'La capa de presentación es la que consume más interfaces y la que hay que '
     'revisar cuando se agrega una pantalla.',
     'Son {componentes} componentes de interfaz y {interfaces} interfaces '
     'consumidas, con {flechas} conexiones. Cada componente agrupa su .fxml, su hoja de estilos y su '
     'controlador. Las hojas CSS aparecen cargadas por Main, que es donde el '
     'código las carga de verdad.'),
    ('puml_componentes/03_Componentes_negocio.puml',
     'DiagramaComponentes_Negocio', 'Capa de lógica de negocio',
     'Es la capa donde se decide si una operación es válida, y por tanto donde '
     'más interesa ver con precisión qué depende de qué.',
     'Son {componentes} componentes de negocio y {interfaces} interfaces, con {flechas} conexiones. '
     'Incluye SesionUsuario, que es el Singleton del proyecto, y marca '
     'PagoService como excluida de la compilación en el pom.xml.'),
    ('puml_componentes/04_Componentes_datos.puml', 'DiagramaComponentes_Datos',
     'Capa de datos y tablas propias',
     'Sin ORM, la relación entre repositorio y tabla solo existe dentro de las '
     'cadenas de SQL. Este diagrama la hace explícita.',
     'Son {componentes} componentes de datos y {tablas} tablas, con {flechas} conexiones. Cada flecha '
     'sale de una consulta real leída del código. Aquí aparecen también los '
     'tres DAO, que ejecutan SQL igual que los repositorios.'),
    ('puml_componentes/05_Componentes_datos_compartidos.puml',
     'DiagramaComponentes_Datos_Compartidos', 'Tablas compartidas',
     'Una tabla escrita desde varios sitios es un riesgo de consistencia. '
     'Separarlas en su propia lámina es lo que permite verlas.',
     'Son {tablas} tablas que más de un componente consulta o modifica, con {flechas} '
     'conexiones. Entre ellas movimientos_inventario, escrita desde tres '
     'repositorios distintos.'),
    ('puml_componentes/06_Componentes_artefactos.puml',
     'DiagramaComponentes_Artefactos', 'Artefactos: qué archivo contiene qué',
     'Las láminas anteriores hablan de componentes lógicos. Esta baja al '
     'archivo concreto, que es lo que hace falta para localizar el código.',
     'Son {artefactos} artefactos y {flechas} relaciones: los paquetes de código fuente, los '
     '.fxml, las hojas de estilo y los recursos, con el componente lógico al '
     'que pertenece cada uno.'),
    ('puml_componentes/07_Componentes_infraestructura.puml',
     'DiagramaComponentes_Infraestructura', 'Infraestructura transversal',
     'Hay tres piezas que usa medio sistema y que, dibujadas en las otras '
     'láminas, generaban líneas cruzando el diagrama entero.',
     'ConexionBD, SesionUsuario y TextoBusqueda con sus {flechas} consumidores. '
     'Sacarlas aquí es lo que permitió que las láminas por capa quedaran '
     'legibles.'),
    ('puml_componentes/08_Componentes_spring_sin_compilar.puml',
     'DiagramaComponentes_Spring_Sin_Compilar',
     'Código Spring que no compila',
     'El repositorio conserva seis clases que importan Spring o Jakarta sin que '
     'el pom.xml declare esas dependencias. No forman parte del sistema que se '
     'ejecuta, pero están en el código, y documentarlas evita que alguien las '
     'dé por funcionales.',
     'Son {artefactos} artefactos. Muestra exactamente las clases que el '
     'maven-compiler-plugin excluye y por qué, junto con los archivos de '
     'configuración y los scripts que tampoco participan en la compilación.'),
]

ARQUITECTURA = [
    ('puml/01_Arquitectura.puml', 'DiagramaArquitectura_Capas',
     'Arquitectura en capas del sistema',
     'La decisión de arquitectura registrada en docs/architecture.md es JavaFX '
     'con JDBC y sin Spring. Este diagrama es la comprobación de que el código '
     'que hay en la rama responde a esa decisión, y es el que resuelve la '
     'HU-128.',
     'Son {elementos} elementos y {flechas} dependencias entre capas. Están los paquetes de '
     'src/main/java agrupados en presentación, negocio, datos e '
     'infraestructura, los recursos de src/main/resources y PostgreSQL. Los '
     'tres DAO están en la capa de datos porque ejecutan SQL, no en la de '
     'negocio. El diagrama marca además los saltos de capa que el código '
     'comete, con su número real de ocurrencias.'),
]

DESPLIEGUE = [
    ('puml/06_Despliegue.puml', 'DiagramaDespliegue_Entorno',
     'Despliegue: entorno de desarrollo e integración continua',
     'GastroFlow no tiene empaquetado de distribución: el pom.xml no declara '
     'ningún plugin que produzca un instalable. Dibujar servidores de '
     'aplicaciones sería inventar una topología que no existe. El diagrama '
     'documenta el despliegue que sí ocurre, que es el del entorno de '
     'desarrollo y el del runner de integración continua.',
     'Son {nodos} nodos y {artefactos} artefactos: el equipo de desarrollo con su JDK, el árbol '
     'del proyecto, la base PostgreSQL, y el repositorio en GitHub con el '
     'runner que ejecuta los pasos declarados en .github/workflows/ci.yml. Las '
     'versiones de las dependencias se leen del pom.xml.'),
]

ESTADOS = [
    ('puml/08_Estados_Pedido.puml', 'DiagramaEstados_Pedido',
     'Estados del pedido',
     'La HU-129 pide saber qué transiciones son válidas para un pedido. El '
     'estado vive en el enum EstadoPedido y se cambia desde varios sitios, así '
     'que sin un diagrama no hay forma de responder esa pregunta sin leerse '
     'todo el código.',
     '{estados} estados y {transiciones} transiciones numeradas. Los cuatro estados no '
     'cerrados van dentro del bloque «activo», y de ese bloque salen las dos '
     'transiciones de cierre: cerrarPedido y cancelarPedido no miran en qué '
     'estado está el pedido, solo que no sea uno de los dos finales. La tabla '
     'da, por cada transición, el origen, el destino, el método que la ejecuta '
     'y la condición que debe cumplirse. ASIGNADO_MESA va en gris porque ningún '
     'código lo asigna, y las tres flechas azules escriben con '
     'pedidoRepository.save() sin pasar por PedidoService, saltándose la guarda '
     'ESTADOS_YA_CERRADOS.'),
    ('puml/09_Estados_Mesa.puml', 'DiagramaEstados_Mesa',
     'Estados de la mesa',
     'La otra mitad de la HU-129. El estado de la mesa no es un enum de Java '
     'sino una fila de la tabla estados_mesa, y se cambia desde cinco sitios '
     'distintos, dos de ellos saltándose la capa de servicio.',
     '{estados} estados y {transiciones} transiciones numeradas. Ninguna '
     'transición consulta el estado actual de la mesa salvo asignarComensales, '
     'que lo exige en su propio SQL, y por eso es la única flecha que sale de '
     'un estado concreto; las demás salen del bloque «cualquier estado del '
     'catálogo». MANTENIMIENTO va en gris porque está en el catálogo y ningún '
     'código lo asigna. LIBRE va en rojo porque el código lo pide y el catálogo '
     'no lo tiene. Las flechas azules escriben sin pasar por MesaService y por '
     'tanto no dejan fila en historial_estado_mesa.'),
]

SECCIONES = [
    ('diagramaClases.md', 'Diagramas de clases', CLASES,
     'Documentación de diseño y consolidación del modelo de datos — trabajo previo, no corresponde a ninguna de las tres HU de esta semana',
     'Documentan el modelo de clases completo de la rama: 98 tipos, 371 '
     'atributos y 663 métodos. Se reparten en quince láminas porque una sola '
     'resultaba ilegible: siete de miembros, agrupadas por capa, y ocho de '
     'relaciones, separadas por origen y destino para que las líneas no se '
     'crucen.'),
    ('diagramaComponentes.md', 'Diagramas de componentes', COMPONENTES,
     'Documentación de diseño y consolidación del modelo de datos — trabajo previo, no corresponde a ninguna de las tres HU de esta semana',
     'Documentan los 49 componentes del sistema y las dependencias entre ellos. '
     'Las interfaces provistas no son nombres inventados: cada una se dedujo de '
     'las llamadas que de verdad cruzan el límite entre dos componentes, '
     'medidas sobre el código.'),
    ('diagramaArquitectura.md', 'Diagrama de arquitectura', ARQUITECTURA,
     'HU-128 (#273)',
     'Documenta la organización en capas del sistema y comprueba que el código '
     'de la rama responde a la decisión de arquitectura registrada en '
     'architecture.md.'),
    ('diagramaDespliegue.md', 'Diagrama de despliegue', DESPLIEGUE,
     'HU-130 (#275)',
     'Documenta dónde se ejecuta cada pieza. El proyecto no produce un '
     'instalable, así que el diagrama recoge el entorno de desarrollo y el de '
     'integración continua, que son los dos despliegues que existen.'),
    ('diagramaEstados.md', 'Diagramas de estados', ESTADOS,
     'HU-129 (#274)',
     'Documentan los estados por los que pasa un pedido y una mesa, y qué '
     'transiciones son válidas entre ellos. Cada flecha lleva un número y el '
     'detalle está en una tabla, para que ninguna etiqueta quede flotando entre '
     'dos líneas.'),
]

NOTA_METODO = """
## Cómo se construyeron

Estos diagramas no se dibujaron a mano. Los genera un conjunto de scripts que
leen el código fuente de la rama y producen el `.puml`, y se comprueban contra
ese mismo código con verificadores independientes que vuelven a leerlo con otro
método. Si el código cambia y el diagrama deja de ser cierto, la comprobación
falla.

| Qué | Dónde |
|---|---|
| Rama | `{rama}` |
| Commit | `{commit}` ({commit_largo}) |
| Fecha del commit | {fecha} |
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
"""


FLECHA = re.compile(
    r'^\s*[\w"\[\]\*\.]+\s+[-.]+(?:\[[^\]]*\])?[|o\*]?[-.]*'
    r'(?:up|down|left|right)?[-.]*[|o\*>]+\s', re.M)


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
        flechas=len([x for x in FLECHA.findall(s) if 'hidden' not in x]),
    )
    c['tipos'] = c['clases'] + c['interfaces'] + c['enums']
    c['elementos'] = c['tipos'] + c['nodos'] + c['artefactos'] + \
        c['componentes'] + c['tablas']
    return c


def render():
    os.makedirs(IMG, exist_ok=True)
    tmp = OUT + '/.tmp'
    os.makedirs(tmp, exist_ok=True)
    todos = CLASES + COMPONENTES + ARQUITECTURA + DESPLIEGUE + ESTADOS
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
    for archivo, titulo, lista, issue, intro in SECCIONES:
        L = [f'# {titulo}', '',
             f'**Issue:** {issue}  ',
             f'**Rama:** `{RAMA}`  ',
             f'**Commit:** `{COMMIT}`  ',
             f'**Fecha del commit:** {FECHA}', '',
             intro, '',
             (f'Son {len(lista)} diagramas.' if len(lista) > 1
              else 'Es un solo diagrama.'),
             '', '---', '']
        for ruta, nombre, tit, just, expl in lista:
            c = conteo(ruta)
            just, expl = just.format(**c), expl.format(**c)
            L += [f'## {tit}', '',
                  f'![{tit}](Imagenes/{nombre}.png)', '',
                  '**Por qué está.** ' + just, '',
                  '**Qué muestra.** ' + expl, '', '---', '']
        L.append(NOTA_METODO.format(rama=RAMA, commit=COMMIT,
                                    commit_largo=COMMIT_LARGO, fecha=FECHA))
        open(f'{OUT}/{archivo}', 'w', encoding='utf-8').write('\n'.join(L))
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
