"""Diagrama de arquitectura de GastroFlow en PlantUML.

Las clases de cada capa salen de clases.json, los archivos de
src/main/resources, las librerias de pom.xml, las tablas de esquema.json y
las flechas que se saltan una capa de relaciones.json. No hay texto escrito
a mano sobre el sistema.
"""
import json, os, re, glob, collections

REPO = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                      'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
T = json.load(open(BASE + '/clases.json', encoding='utf-8'))
TT = json.load(open(BASE + '/clases_test.json', encoding='utf-8'))
R = json.load(open(BASE + '/relaciones.json', encoding='utf-8'))
E = json.load(open(BASE + '/esquema.json', encoding='utf-8'))
POR = {t['nombre']: t for t in T}

# una caja por paquete lista las clases de nivel superior, en orden de archivo
por_paq = collections.defaultdict(list)
for t in T:
    if not t['anidada_en']:
        por_paq[t['paquete']].append(t['nombre'])

pom = open(REPO + '/pom.xml', encoding='utf-8').read()
props = dict(re.findall(r'<(\w[\w.]*)>([^<>]+)</\1>', pom))


def dep_txt(bloque):
    g = re.search(r'<groupId>(.*?)</', bloque).group(1)
    a = re.search(r'<artifactId>(.*?)</', bloque).group(1)
    v = re.search(r'<version>(.*?)</', bloque)
    v = v.group(1) if v else ''
    m = re.fullmatch(r'\$\{(.+?)\}', v)
    if m:
        v = props.get(m.group(1), v)
    return f'{g}:{a}:{v}', ('test' if '<scope>test</scope>' in bloque else 'compile')


deps = collections.defaultdict(list)
for m in re.finditer(r'<dependency>(.*?)</dependency>', pom, re.S):
    txt, scope = dep_txt(m.group(1))
    deps[scope].append(txt)

excluidos = {l.strip() for l in re.findall(r'<exclude>(.+?)</exclude>', pom)}
release = props.get('maven.compiler.release', '?')

SRCJ = REPO + '/src/main/java'
RES = REPO + '/src/main/resources'
otros_res = sorted(os.path.relpath(p, RES) for p in
                   glob.glob(RES + '/**/*', recursive=True)
                   if os.path.isfile(p) and not p.endswith('.gitkeep')
                   and not re.match(r'(views|css)/', os.path.relpath(p, RES)))
views = sorted(os.path.basename(p) for p in glob.glob(REPO + '/src/main/resources/views/*'))
csss = sorted(os.path.basename(p) for p in glob.glob(REPO + '/src/main/resources/css/*'))
tests = sorted({t['archivo'].replace('.java', '') for t in TT})

CAPAS = {'controller': 'presentacion', 'service': 'negocio',
         'dao': 'datos', 'repository': 'datos', 'ConexionBD': 'conexion'}


def plural(n, s='clase'):
    return f'{n} {s}' if n == 1 else f'{n} {s}s'


def caja(alias, titulo, estereotipo, items, color='#FFFFFF'):
    L = [f'  class "{titulo}" as {alias} <<{estereotipo}>> {color} {{']
    for i in items:
        L.append(f'    {i}')
    L.append('  }')
    return L


L = ['@startuml', 'allowmixing',
     'skinparam backgroundColor #FFFFFF',
     'skinparam shadowing false',
     'skinparam defaultFontName Helvetica',
     'skinparam roundCorner 4',
     'skinparam nodesep 45',
     'skinparam ranksep 85',
     'skinparam padding 6',
     'skinparam ArrowColor #1E293B',
     'skinparam ArrowThickness 1.6',
     'skinparam ArrowFontSize 11',
     'skinparam ArrowFontStyle bold',
     'skinparam ArrowFontColor #1E293B',
     'skinparam PackageBorderColor #94A3B8',
     'skinparam PackageFontStyle bold',
     'skinparam PackageFontSize 14',
     'skinparam PackageTitleAlignment left',
     'skinparam ClassBorderColor #334155',
     'skinparam ClassFontSize 14',
     'skinparam ClassAttributeIconSize 0',
     'skinparam ClassAttributeFontSize 12',
     'skinparam ClassStereotypeFontSize 11']

# ------------------------------------------------------ modelo transversal --
L.append('package "Modelo y apoyo" as modelo <<transversal>> #F3E8FF {')
trans = ['entity', 'exceptions', 'dto', 'enums', 'session', 'util']
# directorios de src/main/java que no tienen ninguna clase
vacios_paq = sorted(os.path.basename(d) for d in glob.glob(SRCJ + '/*')
                    if os.path.isdir(d) and not por_paq.get(os.path.basename(d)))
for i, p in enumerate(trans):
    L += caja(f'm{i}', p, plural(len(por_paq[p])), sorted(por_paq[p]))
for i, pv in enumerate(vacios_paq):
    n_arch = len(glob.glob(f'{SRCJ}/{pv}/*.java'))
    vac = len([f for f in glob.glob(f'{SRCJ}/{pv}/*.java')
               if os.path.getsize(f) == 0])
    L += caja(f'm{len(trans)+i}', pv,
              f'{n_arch} .java · {vac} de 0 bytes', [])
todas_m = trans + vacios_paq
for i in range(1, len(todas_m)):
    L.append(f'  m{i-1} -[hidden]right- m{i}')
L.append('}')

# ----------------------------------------------------------- presentacion --
L.append(f'package "Capa de presentacion" as presentacion '
         f'<<controller + resources>> #DBEAFE {{')
L += caja('p1', 'Main', 'javafx.application.Application', [])
L += caja('p2', 'resources/views', plural(len(views), 'archivo'), views)
L += caja('p3', 'resources/css', plural(len(csss), 'archivo'), csss)
L += caja('p4', 'controller', plural(len(por_paq['controller'])),
          sorted(por_paq['controller']))
L += caja('p5', 'resources', plural(len(otros_res), 'archivo'), otros_res)
L += ['  p1 -[hidden]right- p2', '  p2 -[hidden]right- p3',
      '  p3 -[hidden]right- p4', '  p4 -[hidden]right- p5', '}']

# ---------------------------------------------------------------- negocio --
L.append('package "Capa de logica de negocio" as negocio <<service>> #DCFCE7 {')
L += caja('n1', 'service', plural(len(por_paq['service'])), sorted(por_paq['service']))
L.append('}')

# ------------------------------------------------------------------ datos --
L.append('package "Capa de acceso a datos" as datos <<repository + dao · JDBC>> #FFEDD5 {')
L += caja('d1', 'repository', plural(len(por_paq['repository'])),
          sorted(por_paq['repository']))
L += caja('d2', 'dao', plural(len(por_paq['dao'])), sorted(por_paq['dao']))
L += ['  d1 -[hidden]right- d2', '}']

# --------------------------------------------------------------- conexion --
cbd = POR['ConexionBD']
L.append('package "ConexionBD" as conexion <<1 clase>> #F1F5F9 {')
L += caja('c1', 'ConexionBD', 'org.postgresql',
          [f"+ {m['nombre']}() : {m['retorno']}" for m in cbd['metodos']])
L.append('}')

# ----------------------------------------------------------- dependencias --
# cada libreria se dibuja aparte y se conecta con las capas que la importan
PREFIJO = {'javafx-controls': 'javafx.', 'javafx-fxml': 'javafx.fxml',
           'postgresql': 'org.postgresql', 'jbcrypt': 'org.mindrot',
           'junit-jupiter': 'org.junit'}
importa = collections.defaultdict(set)
for art, pfx in PREFIJO.items():
    for ruta in glob.glob(REPO + '/src/*/java/**/*.java', recursive=True):
        if not re.search(rf'^import\s+{re.escape(pfx)}',
                         open(ruta, encoding='utf-8').read(), re.M):
            continue
        rel = ruta.split('/java/', 1)[1]
        paq = rel.split('/')[0] if '/' in rel else '(raiz)'
        if '/src/test/' in ruta:
            importa[art].add('pruebas')
        else:
            importa[art].add(CAPAS.get(paq, 'presentacion'))

# una caja por scope; en el estereotipo, las capas que importan la libreria
L.append('package "Dependencias" as libs <<pom.xml>> #FEF9C3 {')
for i, scope in enumerate(('compile', 'test')):
    items = []
    for txt in deps[scope]:
        art = txt.split(':')[1]
        quien = ', '.join(sorted(importa.get(art, ()))) or '-'
        items.append(f'{txt}   ({quien})')
    L += caja(f'l{i}', scope,
              f'maven.compiler.release {release}' if scope == 'compile'
              else 'scope test', items)
L += ['  l0 -[hidden]right- l1', '}']

# ---------------------------------------------------------------- pruebas --
L.append('package "src/test/java" as pruebas <<JUnit>> #FEF9C3 {')
L += caja('t1', 'test',
          f'{len(TT)} clases en {len(tests)} archivos', tests)
L.append('}')

# ----------------------------------------------------------------- la bd --
L.append(f'database "PostgreSQL\\ngastroflow · {len(E)} tablas\\n'
         f'db/ddl · db/migrations · db/functions" as bd #DCFCE7')

# --------------------------------------------- flechas entre capas, medidas --
pares = set()
for a in R['asociacion']:
    pares.add((a['origen'], a['destino']))
for d in R['dependencia']:
    pares.add((d['origen'], d['destino']))
entre = collections.Counter()
for o, d in pares:
    co, cd = CAPAS.get(POR[o]['paquete']), CAPAS.get(POR[d]['paquete'])
    if co and cd and co != cd:
        entre[(co, cd)] += 1

L += ['presentacion -down-> negocio : <<use>>',
      'negocio -down-> datos : <<use>>',
      'datos -down-> conexion : <<use>>',
      'conexion -down-> bd : JDBC']
SALTO = [('presentacion', 'datos'), ('negocio', 'conexion'),
         ('presentacion', 'conexion')]
for o, d in SALTO:
    if entre[(o, d)]:
        L.append(f'{o} -[#B91C1C]..> {d} : <<use>> salta capa · {entre[(o, d)]}')
for c in ('presentacion', 'negocio', 'datos'):
    L.append(f'{c} -up[#7E22CE]..> modelo : <<use>>')

# pruebas: a que capas llegan, contado sobre los tipos del proyecto que usan
# (no sobre los import: las pruebas viven en el mismo paquete que lo probado)
toca = collections.defaultdict(set)
for t in TT:
    if t['anidada_en']:
        continue
    txt = open(REPO + '/src/test/java/' + t['archivo'], encoding='utf-8').read()
    txt = re.sub(r'/\*.*?\*/|//[^\n]*|"(?:\\.|[^"\\])*"', ' ', txt, flags=re.S)
    for p in set(re.findall(r'\b[A-Z]\w*\b', txt)):
        if p in POR and CAPAS.get(POR[p]['paquete']):
            toca[CAPAS[POR[p]['paquete']]].add(p)
toca = {c: len(v) for c, v in toca.items()}
for capa in sorted(toca):
    L.append(f'pruebas -[#047857]..> {capa} : <<use>> · {toca[capa]}')
L.append('@enduml')

open(BASE + '/puml/01_Arquitectura.puml', 'w', encoding='utf-8').write('\n'.join(L) + '\n')

print('clases por paquete:', {p: len(v) for p, v in sorted(por_paq.items())})
print('total nivel superior:', sum(len(v) for v in por_paq.values()), 'de', len(T))
print('excluidas en pom.xml:', len(excluidos), sorted(excluidos))
print('relaciones entre capas:', dict(entre))
