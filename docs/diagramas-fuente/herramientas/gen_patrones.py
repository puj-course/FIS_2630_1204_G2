"""Los tres diagramas de patrones, sacados del codigo y no escritos a mano.

Cada uno dibuja solo las clases de su issue:

    #292  Singleton  -> session/SesionUsuario.java
    #293  Facade     -> service/PedidoFacade.java
    #294 y #281  Strategy -> service/descuento/ (el enum TipoDescuento del
         #294 ya no se usa: lo sustituyo el #281, asi que no se dibuja)

Las clases que rodean al patron (quien lo usa, que subsistema envuelve)
salen como cajas sin miembros: estan para que la relacion se entienda, no
son parte del codigo del issue.

Se escribe PlantUML directamente, no Mermaid traducido: la traduccion era
un paso de mas donde se perdian los estereotipos, los genericos y alguna
etiqueta. De cada diagrama salen tres archivos, igual que los demas del
repositorio: el .puml fuente, la imagen y el .md que la muestra.

Los miembros y las flechas salen de clases.json y de llamadas.json, que se
extraen del codigo.
"""
import json
import os
import re
import shutil
import subprocess

BASE = os.environ.get('GF_PAT') or os.environ.get('GF_OUT', '.')
OUT = os.environ.get('GF_DOCS') or (BASE + '/docs_entrega')
IMG = OUT + '/Imagenes'
FUENTE = OUT + '/diagramas-fuente'
# los .md van en la raiz de docs, con los demas diagramas del proyecto
CARPETA = OUT
JAR = os.environ.get('PLANTUML_JAR',
                     '/home/claude/pl/package/vendor/plantuml.jar')

T = {t['nombre']: t for t in
     json.load(open(BASE + '/clases.json', encoding='utf-8'))}
LL = json.load(open(BASE + '/llamadas.json', encoding='utf-8'))

FALTAN = [n for n in ('SesionUsuario', 'PedidoFacade', 'DescuentoStrategy',
                      'DescuentoMontoFijo', 'DescuentoPorcentaje',
                      'CalculadoraDescuento', 'ComandaCocina',
                      'PedidoComandaAdapter', 'AlertaStock',
                      'AlertaStockFactory') if n not in T]
if FALTAN:
    raise SystemExit(
        'El codigo analizado no tiene ' + ', '.join(FALTAN) + '. Los '
        'diagramas de patrones se generan del codigo, asi que hay que correr '
        'la herramienta contra un clon al dia de develop.')

LLAMA = {}
for l in LL['llamadas']:
    LLAMA.setdefault(l['origen'], {}).setdefault(l['destino'], set()).add(
        l['metodo'])

CAB = """@startuml
skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam defaultFontName Helvetica
skinparam roundCorner 6
skinparam nodesep 30
skinparam ranksep 60
skinparam ClassBorderColor #334155
skinparam ClassBackgroundColor #FFFFFF
skinparam ClassFontSize 14
skinparam ClassAttributeFontSize 12
skinparam ClassStereotypeFontSize 11
skinparam ArrowColor #1E293B
skinparam ArrowFontSize 11
hide circle"""

# el tipo con el que se declara la caja es el que dice el codigo, no uno
# elegido a mano: una interfaz se dibuja como interfaz
PALABRA = {'class': 'class', 'interface': 'interface', 'enum': 'enum',
           'record': 'class', 'annotation': 'annotation'}


def miembros(nombre, solo_metodos=None, sin_campos=False, solo_campos=None):
    """Las lineas de una caja, tal como estan declaradas en el .java."""
    t = T[nombre]
    out = []
    if not sin_campos:
        for c in t['campos']:
            if solo_campos is not None and c['nombre'] not in solo_campos:
                continue
            out.append(f"  {c['vis']}{c['nombre']} : {c['tipo']}")
    for m in t['metodos']:
        if solo_metodos is not None and m['nombre'] not in solo_metodos:
            continue
        ps = ', '.join(f"{p['nombre']} : {p['tipo']}" for p in m['params'])
        # el extractor marca el constructor solo en algunos tipos; si no
        # viene la marca, se reconoce porque se llama como la clase
        es_ctor = m.get('constructor', m['nombre'] == nombre)
        ret = '' if es_ctor else f" {m['retorno']}"
        out.append(f"  {m['vis']}{m['nombre']}({ps}){ret}")
    return out


def caja(nombre, estereotipo=None, solo_metodos=None, sin_campos=False,
         vacia=False, solo_campos=None):
    palabra = PALABRA[T[nombre]['kind']]
    cab = f'{palabra} {nombre}'
    if estereotipo:
        cab += f' <<{estereotipo}>>'
    if vacia:
        return [cab + ' {', '}']
    return ([cab + ' {']
            + miembros(nombre, solo_metodos, sin_campos, solo_campos)
            + ['}'])


def conteo(lineas):
    """Los numeros que cita el texto, contados sobre el diagrama generado.

    Se cuentan aqui y no se escriben a mano: si manana el codigo cambia y
    el diagrama crece, la frase cambia con el en lugar de quedarse
    mintiendo.
    """
    cajas = [l for l in lineas if re.match(r'(class|interface|enum) ', l)]
    vacias, campos, metodos, dentro = 0, 0, 0, False
    cuerpo = 0
    for l in lineas:
        if re.match(r'(class|interface|enum) ', l):
            dentro, cuerpo = True, 0
        elif l == '}':
            if dentro and cuerpo == 0:
                vacias += 1
            dentro = False
        elif dentro and re.match(r'  [-+#]', l):
            cuerpo += 1
            if re.match(r'  [-+#]\w+\(', l):
                metodos += 1
            else:
                campos += 1
    flechas = [l for l in lineas if re.match(r'\w+ [<>|.\-o*]+ ', l)
               or re.match(r'\w+ [<>|.\-o*]+ "', l)]
    return dict(cajas=len(cajas), vacias=vacias, llenas=len(cajas) - vacias,
                campos=campos, metodos=metodos, flechas=len(flechas),
                realizaciones=len([l for l in flechas if '|' in l]))


def escribe(md, nombre, titulo, porque, muestra, lineas):
    """El .puml fuente, su imagen y el .md que la muestra."""
    os.makedirs(IMG, exist_ok=True)
    os.makedirs(FUENTE, exist_ok=True)
    os.makedirs(CARPETA, exist_ok=True)
    puml = '\n'.join([CAB] + lineas + ['@enduml']) + '\n'
    open(f'{FUENTE}/{nombre}.puml', 'w', encoding='utf-8').write(puml)

    tmp = f'{OUT}/.tmp_patrones'
    os.makedirs(tmp, exist_ok=True)
    # el dpi se fija al renderizar y no se guarda en el fuente, igual que en
    # gen_docs.py: asi el .puml del repositorio es el diagrama y nada mas
    open(f'{tmp}/{nombre}.puml', 'w', encoding='utf-8').write(
        puml.replace('@startuml', '@startuml\nskinparam dpi 96', 1))
    r = subprocess.run(
        f'java -DPLANTUML_LIMIT_SIZE=16384 -jar {JAR} -tpng {nombre}.puml',
        shell=True, cwd=tmp, capture_output=True, text=True)
    err = '\n'.join(x for x in (r.stderr or '').split('\n')
                    if x and 'Picked up' not in x)
    if err:
        print('  plantuml:', err[:300])
    if not os.path.exists(f'{tmp}/{nombre}.png'):
        raise SystemExit(f'no se renderizo {nombre}.png')
    shutil.copy(f'{tmp}/{nombre}.png', f'{IMG}/{nombre}.png')

    c = conteo(lineas)
    open(f'{CARPETA}/{md}', 'w', encoding='utf-8').write(
        f'# {titulo}\n\n'
        f'![{titulo}](Imagenes/{nombre}.png)\n\n'
        f'**Por qué está.** {porque.format(**c)}\n\n'
        f'**Qué muestra.** {muestra.format(**c)}\n')
    print(f'  {md}  +  {nombre}.puml  +  {nombre}.png')


# ===================================================== #292 · Singleton ===
# La unica clase del issue es SesionUsuario. Los cuatro que la consumen van
# como cajas vacias: no son codigo del issue, son quien lo usa.
CLIENTES_SESION = sorted(o for o, d in LLAMA.items() if 'SesionUsuario' in d)
L = caja('SesionUsuario', 'Singleton')
for c in CLIENTES_SESION:
    L += caja(c, vacia=True)
for c in CLIENTES_SESION:
    L.append(f'{c} ..> SesionUsuario : «use»')
L.append('SesionUsuario --> SesionUsuario : INSTANCIA 1')
escribe('diagramaSingleton.md', 'DiagramaPatron_Singleton',
        'Patrón Singleton · HU #292 · la sesión del usuario',
        'GastroFlow abre cada pantalla en su propia ventana y todas '
        'necesitan saber quién entró, pero los controladores los instancia '
        'JavaFX desde el .fxml, así que no hay por dónde pasarles el '
        'usuario. Esta vista responde dónde vive ese estado y quién lo toca.',
        'Son {cajas} cajas y {flechas} flechas, con {campos} atributos y '
        '{metodos} operaciones en la única clase de la historia. La flecha '
        'que sale de SesionUsuario y vuelve a ella misma es lo que distingue '
        'un Singleton de cualquier clase de utilidad: la clase guarda una '
        'instancia de su propio tipo y la reparte por getInstance(), con el '
        'constructor en privado como única puerta. Las otras {vacias} cajas '
        'van vacías a propósito: son quienes consumen la sesión, no código '
        'de la historia. De esas, AutenticacionService es la única que '
        'escribe, llamando a iniciarSesion(...) después de validar la '
        'contraseña; las demás solo leen.', L)

# ======================================================== #293 · Facade ===
# PedidoFacade con todos sus miembros, y de cada clase del subsistema solo
# los metodos que la fachada le llama de verdad.
SUB_FACADE = ['PedidoService', 'InventarioService', 'DisponibilidadDAO']
CLIENTES_FACADE = sorted(o for o, d in LLAMA.items() if 'PedidoFacade' in d)
L = caja('PedidoFacade', 'Facade')
for s in SUB_FACADE:
    L += caja(s, solo_metodos=LLAMA['PedidoFacade'].get(s, set()),
              sin_campos=True)
for c in CLIENTES_FACADE:
    L += caja(c, vacia=True)
for c in CLIENTES_FACADE:
    L.append(f'{c} ..> PedidoFacade : «use»')
for s in SUB_FACADE:
    L.append(f'PedidoFacade --> {s}')
escribe('diagramaFacade.md', 'DiagramaPatron_Facade',
        'Patrón Facade · HU #293 · la confirmación de un pedido',
        'Confirmar un pedido no es una operación sino varias y en orden, '
        'contra dos servicios y un DAO, con una bandera de idempotencia y '
        'una compensación si algo falla. Todo eso vivía en la pantalla del '
        'mesero. Esta vista responde qué quedó escondido detrás de la '
        'fachada.',
        'Son {cajas} cajas y {flechas} flechas, con {campos} atributos y '
        '{metodos} operaciones. De las tres clases del subsistema no se '
        'dibujan todos sus métodos, solo los que la fachada les llama: '
        'cuatro de PedidoService, uno de InventarioService y uno de '
        'DisponibilidadDAO. Eso es justo lo que hace una fachada, estrechar '
        'la superficie. Las tres flechas que bajan son de asociación y no de '
        'uso suelto, porque los tres son atributos suyos declarados en el '
        'constructor. Conviene saber que la fachada cubre confirmar, validar '
        'y calcular el subtotal, pero no todo el subsistema: para obtener el '
        'pedido activo o cancelarlo, la pantalla sigue hablando directamente '
        'con PedidoService.', L)

# ====================================================== #294 · Strategy ===
# La implementacion viva es la del #281, que sustituyo al enum del #294.
# CalculadoraDescuento se dibuja porque es codigo del issue, con la unica
# flecha que recibe: la pantalla le fija la estrategia, pero el calculo se
# lo pide directamente a la estrategia seleccionada en el desplegable.
ESTRATEGIAS = ['DescuentoMontoFijo', 'DescuentoPorcentaje']
L = caja('DescuentoStrategy')
for e in ESTRATEGIAS:
    L += caja(e)
L += caja('CalculadoraDescuento', 'context')
L += caja('CajeroPagoController', vacia=True)
for e in ESTRATEGIAS:
    L.append(f'DescuentoStrategy <|.. {e}')
L.append('CalculadoraDescuento o-- DescuentoStrategy : estrategia')
USOS = {}
for d, ms in LLAMA['CajeroPagoController'].items():
    if d in ('CalculadoraDescuento', 'DescuentoStrategy'):
        USOS[d] = set(ms)
for e in ESTRATEGIAS:
    USOS.setdefault(e, set()).add('new')
# el calculo se pide sobre el tipo del desplegable, que es DescuentoStrategy;
# el extractor no puede resolver esa llamada porque el tipo es generico, asi
# que se anade aqui. Si algun dia si la resuelve, esta linea no la duplica.
USOS.setdefault('DescuentoStrategy', set()).add('calcular')
for d in sorted(USOS):
    L.append(f'CajeroPagoController ..> {d} : {", ".join(sorted(USOS[d]))}')
escribe('diagramaStrategy.md', 'DiagramaPatron_Strategy',
        'Patrón Strategy · HU #294 y #281 · el descuento de la cuenta',
        'El cajero puede descontar por monto fijo o por porcentaje y el '
        'campo donde teclea es el mismo, así que el número significa cosas '
        'distintas según el tipo: misma entrada, misma salida, algoritmo '
        'distinto. Esta vista responde dónde vive cada cálculo y quién '
        'elige.',
        'Son {cajas} cajas y {flechas} flechas, {realizaciones} de ellas de '
        'realización, con {campos} atributos y {metodos} operaciones. La '
        'interfaz declara las dos operaciones que toda estrategia comparte y '
        'debajo cuelgan las dos implementaciones. CalculadoraDescuento es el '
        'contexto y guarda una estrategia por agregación. Lo interesante '
        'está en hacia dónde van las cuatro flechas del controlador: crea '
        'las dos estrategias, le fija una a la calculadora, y después pide '
        'calcular sobre DescuentoStrategy, no sobre la calculadora. Por eso '
        'CalculadoraDescuento no recibe ninguna flecha de cálculo: el '
        'contexto está puenteado y el diagrama lo muestra tal como es.', L)

# ======================================================== Adapter ========
# Target = ComandaCocina, Adaptee = Pedido, Adapter = PedidoComandaAdapter.
# De Pedido solo se dibuja lo que el adaptador le llama: es una entidad
# grande y el resto no tiene que ver con el patron.
L = caja('ComandaCocina', 'Target')
L += caja('PedidoComandaAdapter', 'Adapter')
L += caja('Pedido', 'Adaptee', sin_campos=True,
          solo_metodos=LLAMA['PedidoComandaAdapter'].get('Pedido', set()))
L += caja('CocinaController', vacia=True)
L.append('ComandaCocina <|.. PedidoComandaAdapter')
L.append('PedidoComandaAdapter --> Pedido : pedido')
L.append('CocinaController ..> PedidoComandaAdapter : new')
L.append('CocinaController ..> ComandaCocina : ' + ', '.join(
    sorted(LLAMA['CocinaController'].get('ComandaCocina', set()))))
escribe('diagramaAdapter.md', 'DiagramaPatron_Adapter',
        'Patrón Adapter · el tablero de cocina',
        'El tablero de cocina razona en comandas (columna, minutos de '
        'espera, texto del botón) y la base guarda pedidos, con estados, '
        'fechas e identificadores. Esta vista responde quién hace esa '
        'traducción y qué deja de saber la pantalla.',
        'Son {cajas} cajas y {flechas} flechas, {realizaciones} de '
        'realización, con {campos} atributo y {metodos} operaciones. '
        'ComandaCocina es el Target con las operaciones que el tablero '
        'espera, PedidoComandaAdapter las implementa por composición y de '
        'Pedido, que es el Adaptee, solo se dibujan los métodos que el '
        'adaptador le llama. La prueba de que el patrón manda está en '
        'CocinaController: sale únicamente hacia la interfaz, y hacia el '
        'adaptador solo para construirlo. Queda una grieta visible en la '
        'propia firma del Target: avanzar() devuelve un Pedido, así que el '
        'Adaptee reaparece en la interfaz que decía no conocerlo.', L)

# ======================================================== Factory ========
# Un creador con un metodo que decide cual de los tres productos devolver.
PRODUCTOS = ['AlertaAgotado', 'AlertaStockBajo', 'AlertaNormal']
L = caja('AlertaStockFactory', 'Factory')
L += caja('AlertaStock', 'Product')
for p in PRODUCTOS:
    L += caja(p, 'ConcreteProduct')
L += caja('Ingrediente', sin_campos=True,
          solo_metodos=LLAMA['AlertaStockFactory'].get('Ingrediente', set()))
L += caja('InventarioController', vacia=True)
for p in PRODUCTOS:
    L.append(f'AlertaStock <|.. {p}')
for p in PRODUCTOS:
    L.append(f'AlertaStockFactory ..> {p} : new')
L.append('AlertaStockFactory ..> Ingrediente : ' + ', '.join(
    sorted(LLAMA['AlertaStockFactory'].get('Ingrediente', set()))))
for d in ('AlertaStockFactory', 'AlertaStock'):
    L.append(f'InventarioController ..> {d} : ' + ', '.join(
        sorted(LLAMA['InventarioController'].get(d, set()))))
escribe('diagramaFactory.md', 'DiagramaPatron_Factory',
        'Patrón Factory · las alertas de stock del inventario',
        'La pantalla de inventario decidía en tres sitios distintos el '
        'texto de la columna de alerta y los dos estilos, comparando '
        'cantidades a mano cada vez. Esta vista responde quién decide ahora '
        'qué alerta corresponde a un ingrediente.',
        'Son {cajas} cajas y {flechas} flechas, {realizaciones} de ellas de '
        'realización, con {metodos} operaciones. Las tres flechas new que '
        'salen del creador son la decisión del patrón, y la que va a '
        'Ingrediente muestra en qué se basa para tomarla. '
        'InventarioController queda a un lado pidiendo por crear y '
        'consumiendo por la interfaz, sin nombrar nunca una clase concreta. '
        'Hay que decir una cosa: este creador es un método estático con '
        'constructor privado, lo que se conoce como Simple Factory y no '
        'figura en el catálogo de los 23 patrones GoF, que recoge Factory '
        'Method y Abstract Factory.', L)

# ======================================================= Observer ========
# Vive en la rama de Mariana (HU #288). Mientras no este en develop, el
# codigo analizado no tiene estas clases y el diagrama no se genera: la
# herramienta no puede dibujar lo que no existe en la rama que lee.
OBS = ('InsumoObserver', 'AdministradorAlertaObserver')
if all(n in T for n in OBS):
    SUJETO = ['InventarioService', 'agregarObservador', 'eliminarObservador',
              'notificarObservadores', 'verificarStockMinimo']
    L = caja('InventarioService', 'Subject', solo_metodos=SUJETO,
             solo_campos=['observadores'])
    L += caja('InsumoObserver', 'Observer')
    L += caja('AdministradorAlertaObserver', 'ConcreteObserver')
    L.append('InsumoObserver <|.. AdministradorAlertaObserver')
    L.append('InventarioService o-- "*" InsumoObserver : observadores')
    L.append('InventarioService ..> AdministradorAlertaObserver : new')
    escribe('diagramaObserver.md', 'DiagramaPatron_Observer',
            'Patrón Observer · HU #288 · la alerta de stock bajo',
            'Cuando un insumo baja del mínimo hay que avisar, y el '
            'inventario no tiene por qué saber a quién. Esta vista responde '
            'quién publica el aviso y quién lo escucha.',
            'Son {cajas} cajas y {flechas} flechas, {realizaciones} de '
            'realización, con {campos} atributo y {metodos} operaciones. '
            'InventarioService hace de sujeto: guarda la lista de '
            'observadores, ofrece alta y baja, y notificarObservadores la '
            'recorre; sus demás métodos son de inventario y no se dibujan. '
            'InsumoObserver declara la única operación del observador y '
            'AdministradorAlertaObserver la implementa. Importa lo que no '
            'aparece: no hay ninguna caja de cliente, porque nadie llama a '
            'verificarStockMinimo. La única flecha que entra al observador '
            'concreto es el new del propio constructor del sujeto, que se '
            'suscribe a sí mismo. El sujeto sabe notificar, pero nada lo '
            'pone en marcha.', L)
else:
    print('  (Observer: no esta en esta rama, no se dibuja)')

shutil.rmtree(f'{OUT}/.tmp_patrones', ignore_errors=True)
