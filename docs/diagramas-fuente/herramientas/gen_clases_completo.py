"""Genera los diagramas de clases completos de GastroFlow en PlantUML.

Todo sale de clases.json y relaciones.json, que a su vez salen del codigo
con tree-sitter. No se escribe ningun texto que no este en el repositorio.
"""
import json, os, re, collections

import os
REPO = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                      'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
DIR = BASE + '/puml_clases'
os.makedirs(DIR, exist_ok=True)
T = json.load(open(BASE + '/clases.json', encoding='utf-8'))
R = json.load(open(BASE + '/relaciones.json', encoding='utf-8'))
POR = {t['nombre']: t for t in T}

COLOR = {'entity': '#F3E8FF', 'controller': '#DBEAFE', 'repository': '#FFEDD5',
         'service': '#DCFCE7', 'dao': '#DCFCE7', 'dto': '#FEF9C3',
         'enums': '#FEF9C3', 'exceptions': '#FEF9C3', 'session': '#FEF9C3',
         'util': '#FEF9C3', 'ConexionBD': '#F1F5F9', '(raiz)': '#F1F5F9'}

CAB = '''@startuml
skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam defaultFontName Helvetica
skinparam roundCorner 4
skinparam nodesep 35
skinparam ranksep 60
skinparam padding 3
skinparam ClassFontSize 13
skinparam ClassAttributeFontSize 11
skinparam ClassStereotypeFontSize 10
skinparam ClassBorderColor #334155
skinparam ArrowColor #1E293B
skinparam ArrowFontSize 10
skinparam PackageBorderColor #94A3B8
skinparam PackageFontStyle bold
skinparam ClassAttributeIconSize 0
hide empty members
'''


def esc(s):
    return (s or '').replace('{', '(').replace('}', ')')


def firma(m):
    ps = ', '.join(f"{p['nombre']} : {esc(p['tipo'])}" for p in m['params'])
    ret = '' if m.get('constructor') or not m['retorno'] else f" : {esc(m['retorno'])}"
    mod = ''
    if m['static']:
        mod += '{static} '
    if m['abstract']:
        mod += '{abstract} '
    return f"{m['vis']} {mod}{m['nombre']}({ps}){ret}"


# archivos que pom.xml excluye de la compilacion
EXCLUIDOS = set()
for _l in open(REPO + '/pom.xml', encoding='utf-8'):
    _m = re.search(r'<exclude>(.+?)</exclude>', _l)
    if _m:
        EXCLUIDOS.add(_m.group(1).strip())


def caja(t, sangria='  '):
    kind = {'class': 'class', 'interface': 'interface', 'enum': 'enum',
            'record': 'class', 'annotation': 'annotation'}[t['kind']]
    est = []
    if t['kind'] == 'record':
        est.append('record')
    if t['anidada_en']:
        est.append(f"anidada en {t['anidada_en']}")
    if t['archivo'] in EXCLUIDOS:
        est.append('excluida en pom.xml')
    e = ' <<' + ' · '.join(est) + '>>' if est else ''
    L = [f"{sangria}{kind} {t['nombre']}{e} {COLOR[t['paquete']]} {{"]
    for lit in t['literales']:
        L.append(f'{sangria}  {lit}')
    if t['literales'] and (t['campos'] or t['metodos']):
        L.append(f'{sangria}  --')
    for c in t['campos']:
        mod = '{static} ' if c['static'] else ''
        L.append(f"{sangria}  {c['vis']} {mod}{c['nombre']} : {esc(c['tipo'])}")
    if t['campos'] and t['metodos']:
        L.append(f'{sangria}  --')
    for m in t['metodos']:
        L.append(f'{sangria}  {firma(m)}')
    L.append(f'{sangria}}}')
    return '\n'.join(L)


# ------------------------------------------------------------- el reparto --
# entity se reparte por peso (campos+metodos), no por letra, para que las
# tres partes queden parecidas
_ent = sorted([t for t in T if t['paquete'] == 'entity'], key=lambda x: x['nombre'])
_peso = lambda t: len(t['campos']) + len(t['metodos']) + 3
_lim = sum(_peso(t) for t in _ent) / 3
_partes, _act, _ac = [], [], 0
for _t in _ent:
    if _ac + _peso(_t) > _lim and len(_partes) < 2 and _act:
        _partes.append(_act); _act, _ac = [], 0
    _act.append(_t); _ac += _peso(_t)
_partes.append(_act)
_N = [{(t['paquete'], t['nombre']) for t in parte} for parte in _partes]
_R = [f"{p[0]['nombre']}–{p[-1]['nombre']}" for p in _partes]

GRUPOS = [
 ('01_entity_A',     f'entity · {_R[0]}',     lambda t: (t['paquete'], t['nombre']) in _N[0]),
 ('02_entity_B',     f'entity · {_R[1]}',     lambda t: (t['paquete'], t['nombre']) in _N[1]),
 ('03_entity_C',     f'entity · {_R[2]}',     lambda t: (t['paquete'], t['nombre']) in _N[2]),
 ('04_controller',   'controller',            lambda t: t['paquete'] == 'controller'),
 ('05_repository',   'repository',            lambda t: t['paquete'] == 'repository'),
 ('06_service_dao',  'service · dao',         lambda t: t['paquete'] in ('service', 'dao')),
 ('07_apoyo',        'dto · enums · exceptions · session · util · ConexionBD · Main',
                      lambda t: t['paquete'] in ('dto', 'enums', 'exceptions', 'session',
                                                 'util', 'ConexionBD', '(raiz)')),
]

EXTERNOS = {}
for g in R['generalizacion']:
    pass
# generalizaciones hacia tipos de fuera del proyecto (RuntimeException, Application)
# generalizaciones hacia clases de fuera del proyecto. Se dibuja una caja por
# clase padre (sin los argumentos genericos) y el argumento va en la flecha.
gen_externas = []
for t in T:
    if not t['extends']:
        continue
    padre = t['extends'].strip()
    raiz = padre.split('<')[0].strip()
    if raiz in POR:
        continue
    arg = padre[len(raiz):].strip()
    gen_externas.append((t['nombre'], raiz, arg))

asoc_por_par = {(a['origen'], a['destino']): a for a in R['asociacion']}
resumen = []


DATOS = {'entity', 'dto', 'enums', 'exceptions'}

# color de cada linea segun el paquete al que llega: asi una linea que se
# cruza con otra se distingue por el color de su destino
COLOR_LINEA = {'entity': '#7E22CE', 'controller': '#1D4ED8',
               'repository': '#C2410C', 'service': '#047857', 'dao': '#047857',
               'dto': '#A16207', 'enums': '#A16207', 'exceptions': '#A16207',
               'session': '#A16207', 'util': '#A16207',
               'ConexionBD': '#475569', '(raiz)': '#475569'}


def color(n):
    return COLOR_LINEA.get(POR[n]['paquete'], '#475569') if n in POR else '#475569'


def es_dato(n):
    return n in POR and POR[n]['paquete'] in DATOS


def flecha(a):
    """Composicion si el dueno la crea con new y no la expone; agregacion si
    la recibe por constructor; asociacion simple en los demas casos.
    El color es el del paquete de destino."""
    c = color(a['destino'])
    if a['composicion']:
        return f'*-[{c}]->'
    if a.get('agregacion'):
        return f'o-[{c}]->'
    return f'-[{c}]->'


def asociacion(a, o=None, d=None):
    """El rol va pegado al extremo de destino, junto a la multiplicidad: asi
    no queda una etiqueta suelta en medio de varias lineas cruzadas.

    En el extremo del origen solo se pone "1" cuando la composicion lo
    garantiza: un objeto compuesto pertenece a un unico todo. En una
    agregacion o en una asociacion simple el codigo no dice cuantos objetos
    del origen apuntan al mismo destino, asi que ese extremo se deja sin
    multiplicidad en lugar de afirmar algo que no esta en el codigo."""
    extremo = a['multiplicidad'] + ' ' + '\\n'.join(a['roles'])
    origen = f'{o or a["origen"]} "1" ' if a['composicion'] else f'{o or a["origen"]} '
    return f'{origen}{flecha(a)} "{extremo}" {d or a["destino"]}'

for archivo, titulo, filtro in GRUPOS:
    miembros = [t for t in T if filtro(t)]
    nombres = {t['nombre'] for t in miembros}
    L = [CAB]
    L.append(f'package "{titulo}" {{')
    ordenm = sorted(miembros, key=lambda x: x['nombre'])
    for t in ordenm:
        L.append(caja(t))
    # Rejilla oculta solo cuando las cajas son pequenas (pocos miembros): en
    # clases grandes fija filas y obliga a las lineas a rodear el diagrama.
    if len(ordenm) > 8:
        grande = max(len(t['campos']) + len(t['metodos']) for t in ordenm) > 25
        COLSM = 3 if grande else 5
        nm = [t['nombre'] for t in ordenm]
        for i in range(len(nm)):
            if i % COLSM and i:
                L.append(f'  {nm[i-1]} -[hidden]right- {nm[i]}')
            if i >= COLSM:
                L.append(f'  {nm[i-COLSM]} -[hidden]down- {nm[i]}')
    L.append('}')

    # Relaciones. Se dibujan todas las que tocan al grupo; las clases del otro
    # extremo que no pertenecen al grupo entran como caja de referencia sin
    # miembros, para que la linea y su multiplicidad se vean en contexto.
    rel, referencias = [], {}

    def ref(n):
        if n not in nombres and n in POR:
            referencias[n] = POR[n]

    for a in R['asociacion']:
        if a['origen'] in nombres or a['destino'] in nombres:
            ref(a['origen']); ref(a['destino'])
            rel.append(asociacion(a))
    # Dependencias de uso. Se dibujan las que tocan una clase de datos
    # (entity, dto, enums, exceptions); las demas -sobre todo las 27 que van
    # contra ConexionBD- taparian los miembros y estan completas en 08.
    for d in R['dependencia']:
        if d['origen'] not in nombres and d['destino'] not in nombres:
            continue
        if not (es_dato(d['origen']) or es_dato(d['destino'])):
            continue
        ref(d['origen']); ref(d['destino'])
        rel.append(f"{d['origen']} -[{color(d['destino'])},dashed]-> {d['destino']}")
    for h, pp, arg in gen_externas:
        if h in nombres:
            rel.append(f"class {pp} <<externa>> #FFFFFF")
            rel.append(f"{h} --|> {pp}" + (f" : {arg}" if arg else ''))

    if referencias:
        L.append('package "referencias" <<otros paquetes>> #FAFAFA {')
        orden = sorted(referencias)
        for n in orden:
            t2 = referencias[n]
            kind = {'class': 'class', 'interface': 'interface', 'enum': 'enum',
                    'record': 'class', 'annotation': 'annotation'}[t2['kind']]
            L.append(f"  {kind} {n} <<{t2['paquete']}>> {COLOR[t2['paquete']]} {{\n  }}")
        # rejilla de 4 columnas con enlaces invisibles, si no PlantUML las pone
        # todas en una sola fila y el diagrama se vuelve larguisimo
        COLS = 4
        for i in range(len(orden)):
            if i % COLS and i:
                L.append(f'  {orden[i-1]} -[hidden]right- {orden[i]}')
            if i >= COLS:
                L.append(f'  {orden[i-COLS]} -[hidden]down- {orden[i]}')
        L.append('}')
    L += sorted(set(rel))
    L.append('@enduml')
    open(f'{DIR}/{archivo}.puml', 'w', encoding='utf-8').write('\n'.join(L) + '\n')
    resumen.append((archivo, len(miembros), len(set(rel))))

# ------------------------------------- 08: mapa de todas las relaciones --
# Hay dos tipos llamados EstadoMesa (entity y enums). PlantUML identifica por
# nombre, asi que aqui cada tipo lleva un alias unico paquete_Nombre.
import re as _re
_dup = {n for n in [t['nombre'] for t in T]
        if [t['nombre'] for t in T].count(n) > 1}
_FUENTE = {}


def _id(p):
    return _re.sub(r'\W', '', p) or 'x'


def _alias(nombre, origen=None):
    cands = [t for t in T if t['nombre'] == nombre]
    if len(cands) == 1:
        return _id(cands[0]['paquete']) + "_" + nombre
    # ambiguo: se resuelve por el import del archivo que lo usa
    if origen:
        t0 = [x for x in T if x['nombre'] == origen][0]
        if t0['archivo'] not in _FUENTE:
            _FUENTE[t0['archivo']] = open(
                os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                               'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
                + '/src/main/java/' + t0['archivo'], encoding='utf-8').read()
        src0 = _FUENTE[t0['archivo']]
        for c in cands:
            if _re.search(rf"import\s+{c['paquete']}\.{nombre}\s*;", src0):
                return _id(c['paquete']) + "_" + nombre
        for c in cands:
            if c['paquete'] == t0['paquete']:
                return _id(c['paquete']) + "_" + nombre
    return _id(cands[0]['paquete']) + "_" + nombre


# El mapa completo de 158 relaciones sobre 98 clases no se puede leer: las
# lineas se cruzan y no se distingue cual es cual. Se parte en cuatro mapas
# por el paquete de ORIGEN, de modo que cada relacion aparece en uno y solo
# uno. Cada mapa dibuja solo las clases que sus relaciones tocan.
CAPAS_PAQ = {'controller', 'service', 'dao', 'repository', 'ConexionBD', '(raiz)'}
MODELO_PAQ = {'entity', 'dto', 'enums', 'exceptions', 'session', 'util'}
MAPAS = [
    ('08_rel_controller_capas', {'controller', '(raiz)'}, CAPAS_PAQ, False),
    ('09_rel_controller_modelo', {'controller', '(raiz)'}, MODELO_PAQ, True),
    ('10_rel_service_capas', {'service', 'dao'}, CAPAS_PAQ, False),
    ('11_rel_service_modelo', {'service', 'dao'}, MODELO_PAQ, True),
    ('12_rel_repository_capas', {'repository'}, CAPAS_PAQ, False),
    ('13_rel_repository_modelo', {'repository'}, MODELO_PAQ, True),
    ('14_rel_modelo', MODELO_PAQ | {'ConexionBD'}, CAPAS_PAQ | MODELO_PAQ, True),
]
repartidas = 0
for archivo, paquetes, destinos, con_gen in MAPAS:
    asoc = [a for a in R['asociacion'] if POR[a['origen']]['paquete'] in paquetes
            and POR[a['destino']]['paquete'] in destinos]
    deps = [d for d in R['dependencia'] if POR[d['origen']]['paquete'] in paquetes
            and POR[d['destino']]['paquete'] in destinos]
    # la herencia hacia una clase de fuera va en el mapa "modelo" del paquete
    # de origen, para que cada una aparezca una sola vez
    gens = [g for g in gen_externas if POR[g[0]]['paquete'] in paquetes] \
        if con_gen else []
    repartidas += len(asoc) + len(deps) + len(gens)

    tocadas = {x for a in asoc for x in (a['origen'], a['destino'])} | \
              {x for d in deps for x in (d['origen'], d['destino'])} | \
              {h for h, _p, _a in gens}
    L = [CAB, 'hide members', 'hide circle',
         'skinparam linetype polyline', 'skinparam nodesep 25',
         'skinparam ranksep 70']
    porpaq = collections.defaultdict(list)
    for t in T:
        if t['nombre'] in tocadas:
            porpaq[t['paquete']].append(t)
    # los paquetes se apilan en el orden de las capas: si se dejan uno al lado
    # del otro el dibujo sale larguisimo y las lineas se superponen
    ORDEN = ['(raiz)', 'controller', 'service', 'dao', 'repository',
             'ConexionBD', 'entity', 'dto', 'enums', 'exceptions',
             'session', 'util']
    secuencia = [p for p in ORDEN if p in porpaq]
    primeros = []
    for p in secuencia:
        L.append(f'package "{p}" {COLOR[p]} {{')
        cajas = sorted(porpaq[p], key=lambda x: x['nombre'])
        for t in cajas:
            kind = {'class': 'class', 'interface': 'interface', 'enum': 'enum',
                    'record': 'class', 'annotation': 'annotation'}[t['kind']]
            L.append(f"  {kind} \"{t['nombre']}\" as {_id(p)}_{t['nombre']}")
        # rejilla oculta: sin ella PlantUML pone el paquete en una sola fila
        al = [f"{_id(p)}_{t['nombre']}" for t in cajas]
        primeros.append(al[0])
        if len(cajas) > 4:
            COLS = 3
            for i in range(len(al)):
                if i % COLS and i:
                    L.append(f'  {al[i-1]} -[hidden]right- {al[i]}')
                if i >= COLS:
                    L.append(f'  {al[i-COLS]} -[hidden]down- {al[i]}')
        L.append('}')
    for i in range(1, len(primeros)):
        L.append(f'{primeros[i-1]} -[hidden]down- {primeros[i]}')
    for pext in sorted({p for _h, p, _a in gens}):
        L.append(f'class {pext} <<externa>> #FFFFFF')
    for h, pext, arg in gens:
        L.append(f'{_alias(h)} --|> {pext}' + (f' : {arg}' if arg else ''))
    for a in asoc:
        L.append(asociacion(a, _alias(a['origen']),
                            _alias(a['destino'], a['origen'])))
    for d in deps:
        L.append(f"{_alias(d['origen'])} -[{color(d['destino'])},dashed]-> "
                 f"{_alias(d['destino'], d['origen'])}")
    L.append('@enduml')
    open(f'{DIR}/{archivo}.puml', 'w', encoding='utf-8').write('\n'.join(L) + '\n')
    resumen.append((archivo, len(tocadas), len(asoc) + len(deps) + len(gens)))

total = len(R['asociacion']) + len(R['dependencia']) + len(gen_externas)
assert repartidas == total, f'{repartidas} relaciones repartidas de {total}'

print(f"{'diagrama':22} {'tipos':>6} {'relaciones':>11}")
for a, n, r in resumen:
    print(f'  {a:22} {n:6} {r:11}')
print(f"\ntipos con miembros en 01-07: "
      f"{sum(n for a, n, _ in resumen if '_rel_' not in a)} de {len(T)}")
print(f'relaciones repartidas en los {len(MAPAS)} mapas: {repartidas} de {total}')
