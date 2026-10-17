"""Version simple de los diagramas de componentes.

Los ocho diagramas detallados siguen existiendo. Estos cuatro son para leer
de un vistazo: una caja por componente con su nombre y nada mas, y una flecha
por dependencia real. Sin operaciones, sin listas de archivos, sin cajas de
interfaz duplicadas.

La simplificacion es de presentacion, no de contenido: las flechas salen de
las mismas 379 llamadas medidas sobre el codigo que alimentan los detallados.
"""
import json, os, re, collections

REPO = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                      'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
SAL = BASE + '/puml_componentes'

T = json.load(open(BASE + '/clases.json', encoding='utf-8'))
LL = json.load(open(BASE + '/llamadas.json', encoding='utf-8'))
E = json.load(open(BASE + '/esquema.json', encoding='utf-8'))
pom = open(REPO + '/pom.xml', encoding='utf-8').read()
EXCL = {l.strip().rsplit('/', 1)[-1].replace('.java', '')
        for l in re.findall(r'<exclude>(.+?)</exclude>', pom)}

# mismas capas que el diagrama de arquitectura y que los detallados
SUB = {'controller': 'ui', '(raiz)': 'ui', 'service': 'negocio',
       'dao': 'datos', 'repository': 'datos', 'ConexionBD': 'conexion',
       'session': 'apoyo', 'util': 'apoyo'}
NOMBRE = {'ui': 'Interfaz de usuario', 'negocio': 'Logica de negocio',
          'datos': 'Acceso a datos', 'conexion': 'Conexion',
          'apoyo': 'Apoyo transversal'}
COLOR = {'ui': '#DBEAFE', 'negocio': '#DCFCE7', 'datos': '#FFEDD5',
         'conexion': '#F1F5F9', 'apoyo': '#F3E8FF'}
ORDEN = ['ui', 'negocio', 'datos', 'conexion', 'apoyo']

COMP = {t['nombre']: SUB[t['paquete']] for t in T
        if t['paquete'] in SUB and not t['anidada_en']}

# dependencias componente -> componente, con cuantas operaciones distintas
dep = collections.defaultdict(set)
for l in LL['llamadas']:
    o, d = l['origen'], l['destino']
    if o in COMP and d in COMP and o != d:
        dep[(o, d)].add(l['metodo'])

ALIAS = {}


def al(n):
    ALIAS.setdefault(n, 'c_' + re.sub(r'\W', '_', n))
    return ALIAS[n]


def cab(titulo, direccion='top to bottom direction'):
    return ['@startuml', direccion,
            'skinparam backgroundColor #FFFFFF', 'skinparam shadowing false',
            'skinparam defaultFontName Helvetica',
            'skinparam roundCorner 6',
            'skinparam nodesep 28', 'skinparam ranksep 70',
            'skinparam ArrowColor #475569', 'skinparam ArrowFontSize 11',
            'skinparam ArrowThickness 1.1',
            'skinparam PackageBorderColor #94A3B8',
            'skinparam PackageFontStyle bold',
            'skinparam PackageFontSize 13',
            'skinparam ComponentBackgroundColor #FFFFFF',
            'skinparam ComponentFontSize 12',
            'skinparam ComponentBorderColor #334155',
            'skinparam legendBackgroundColor #FFFFFF',
            'skinparam legendBorderColor #94A3B8',
            'skinparam legendFontSize 11']


def caja(n, sufijo=''):
    marca = '  <&warning>' if n in EXCL else ''
    return f'  component "{n}{sufijo}{marca}" as {al(n)} #FFFFFF'


def escribe(archivo, lineas):
    open(f'{SAL}/{archivo}', 'w', encoding='utf-8').write(
        '\n'.join(lineas + ['@enduml']) + '\n')


# ================================================== S1. mapa de capas =====
# una caja por capa y una flecha por par de capas, con cuantas llamadas
entre = collections.Counter()
for (o, d), ms in dep.items():
    if COMP[o] != COMP[d]:
        entre[(COMP[o], COMP[d])] += len(ms)
L = cab('capas')
for c in ORDEN:
    n = sum(1 for x in COMP.values() if x == c)
    L.append(f'component "**{NOMBRE[c]}**\\n<size:10>{n} '
             f'componente{"s" if n != 1 else ""}</size>" as k_{c} {COLOR[c]}')
for (a, b), n in sorted(entre.items(), key=lambda x: -x[1]):
    L.append(f'k_{a} --> k_{b} : {n} operaciones')
escribe('09_Simple_Capas.puml', L)

# ============================================ S2. pantallas y servicios ===
L = cab('ui', 'left to right direction')
# sin cajas de paquete, por lo mismo que en el de negocio: el color dice la
# capa y las flechas salen mucho mas cortas
par_ui = [(o, d) for (o, d) in dep if COMP[o] == 'ui']
uis = sorted({o for (o, d) in par_ui} | {d for (o, d) in par_ui if COMP[d] == 'ui'})
for nom in uis:
    L.append(caja(nom).replace('#FFFFFF', COLOR['ui']))
destinos = sorted({d for (o, d) in par_ui if COMP[d] != 'ui'},
                  key=lambda x: (ORDEN.index(COMP[x]), x))
for d in destinos:
    L.append(caja(d).replace('#FFFFFF', COLOR[COMP[d]]))
for (o, d) in sorted(par_ui):
    L.append(f'{al(o)} --> {al(d)}')
# sin texto explicativo: solo la clave de colores, que es lo unico que el
# dibujo no dice por si mismo
sin_salida = sorted(n for n, c in COMP.items() if c == 'ui' and n not in uis)
L += ['legend bottom',
      '<b>Colores</b>  '
      '<back:#DBEAFE>     </back> interfaz    '
      '<back:#DCFCE7>     </back> negocio    '
      '<back:#FFEDD5>     </back> datos    '
      '<back:#F3E8FF>     </back> apoyo']
if sin_salida:
    L.append('<b>Sin dependencias</b>  ' + ', '.join(sin_salida))
L.append('endlegend')
escribe('10_Simple_UI.puml', L)

# ============================================== S3. negocio hacia datos ===
L = cab('negocio', 'left to right direction')
# solo los que participan en una dependencia negocio -> algo: un
# repositorio que solo usa la interfaz no pinta nada en esta vista
# todo lo que llama el negocio, no solo datos: cuatro servicios abren su
# propia conexion JDBC saltandose la capa de datos, y eso es lo que mas
# interesa ver de esta capa
par_neg = [(o, d) for (o, d) in dep if COMP[o] == 'negocio']
vivos = {x for par in par_neg for x in par}
# se declaran emparejados: cada servicio y, justo despues, el repositorio
# que usa. Asi graphviz los coloca enfrentados y las flechas salen cortas
servicios = sorted(n for n, c in COMP.items() if c == 'negocio' and n in vivos)
orden_datos, puestos = [], set()
for sv in servicios:
    for d in sorted(d for (o, d) in par_neg if o == sv and COMP[d] != 'negocio'):
        if d not in puestos:
            puestos.add(d)
            orden_datos.append(d)
# sin cajas de paquete: son las que fuerzan dos columnas altas y obligan a
# las flechas a cruzar el diagrama entero. El color ya dice la capa.
for nom in servicios:
    L.append(caja(nom).replace('#FFFFFF', COLOR['negocio']))
for nom in orden_datos:
    L.append(caja(nom).replace('#FFFFFF', COLOR[COMP[nom]]))
for (o, d) in sorted(par_neg):
    muerta = o in EXCL          # si el origen no compila, la flecha no corre
    L.append(f'{al(o)} -[{"#B91C1C" if muerta else "#475569"}]-> {al(d)}')
L += ['legend bottom',
      '<b>Colores</b>  '
      '<back:#DCFCE7>     </back> negocio    '
      '<back:#FFEDD5>     </back> datos    '
      '<back:#F1F5F9>     </back> conexion    '
      '<back:#F3E8FF>     </back> apoyo',
      '<b>Simbolos</b>  <&warning> excluida de la compilacion en pom.xml   '
      '<back:#B91C1C>     </back> flecha roja: sale de una clase excluida',
      'endlegend']
escribe('11_Simple_Negocio.puml', L)

# ============================================= S4. apoyo transversal ======
# aqui las flechas una a una serian 31: se agrupan por capa de origen
L = cab('apoyo')
apoyos = sorted(n for n, c in COMP.items() if c in ('apoyo', 'conexion'))
L.append('package "Piezas compartidas · apoyo y conexion" as p4 #FFFFFF {')
for n in apoyos:
    L.append(caja(n).replace('#FFFFFF', COLOR[COMP[n]]))
L.append('}')
por_capa = collections.defaultdict(lambda: collections.defaultdict(set))
for (o, d), ms in dep.items():
    if d in apoyos:
        por_capa[d][COMP[o]].add(o)
usadas = sorted({c for d in por_capa for c in por_capa[d]},
                key=ORDEN.index)
for c in usadas:
    n = sum(1 for x, cc in COMP.items() if cc == c)
    L.append(f'component "**{NOMBRE[c]}**\\n<size:10>{n} '
             f'componente{"s" if n != 1 else ""}</size>" as q_{c} {COLOR[c]}')
for d in apoyos:
    for c, quienes in sorted(por_capa[d].items(), key=lambda x: ORDEN.index(x[0])):
        # la etiqueta lleva la unidad para que el numero se entienda solo,
        # sin necesidad de un texto que lo explique debajo
        n = len(quienes)
        L.append(f'q_{c} --> {al(d)} : {n} componente{"s" if n != 1 else ""}')
escribe('12_Simple_Apoyo.puml', L)

# ================================================================ informe =
print(f'componentes: {len(COMP)}   dependencias: {len(dep)}')
for c in ORDEN:
    print(f'  {NOMBRE[c]:22} {sum(1 for x in COMP.values() if x == c)}')
print('\nentre capas:')
for (a, b), n in sorted(entre.items(), key=lambda x: -x[1]):
    print(f'  {a:10} -> {b:10} {n}')
print(f'\nconsumidores de apoyo: '
      f'{ {d: {c: len(q) for c, q in v.items()} for d, v in por_capa.items()} }')
