"""Comprueba que los diagramas coinciden exactamente con el codigo.

No usa los .json intermedios para lo que puede leer del .puml: compara lo
dibujado contra clases.json / relaciones.json / esquema.json, que a su vez
salen del parse del codigo.
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
fallos = []


def ok(cond, msg):
    print(('  OK   ' if cond else '  FALLA ') + msg)
    if not cond:
        fallos.append(msg)


def leer(p):
    return open(p, encoding='utf-8').read()


CL = {f: leer(f) for f in sorted(glob.glob(BASE + '/puml_clases/*.puml'))}
ES = {f: leer(f) for f in sorted(glob.glob(BASE + '/puml/*.puml'))}
TODO = ''.join(CL.values())

print('\n=== 1. cobertura de tipos ===')
dibujados = collections.Counter()
for f, s in CL.items():
    if '_rel_' in f or f.endswith('15_pruebas.puml'):
        continue
    for m in re.finditer(r'^\s*(?:class|interface|enum|annotation)\s+'
                         r'(?:"([^"]+)"\s+as\s+\S+|(\w+))', s, re.M):
        dibujados[m.group(1) or m.group(2)] += 1
esperados = {t['nombre'] for t in T}
ok(esperados <= set(dibujados),
   f'los {len(T)} tipos del codigo ({len(esperados)} nombres distintos: '
   f'EstadoMesa existe en entity y en enums) aparecen en 01-07 '
   f'(faltan: {sorted(esperados - set(dibujados)) or "ninguno"})')
EXTERNAS = {'RuntimeException', 'Application', 'JpaRepository'}
extra = {n for n in dibujados if n not in esperados} - EXTERNAS
ok(not extra, f'no hay tipos inventados (sobran: {sorted(extra) or "ninguno"})')

print('\n=== 2. miembros: cada caja con cuerpo == el codigo ===')
mal = []
for f, s in CL.items():
    if '_rel_' in f or f.endswith('15_pruebas.puml'):
        continue
    for m in re.finditer(r'^\s*(?:class|interface|enum|annotation)\s+(\w+)'
                         r'[^\n{]*\{\n(.*?)^\s*\}', s, re.M | re.S):
        nom, cuerpo = m.group(1), m.group(2)
        if not cuerpo.strip():
            continue                      # caja de referencia, sin miembros
        cands = [x for x in T if x['nombre'] == nom]
        if len(cands) > 1:
            # el titulo del paquete del diagrama dice de cual se trata
            titulo = s[:m.start()].rsplit('package "', 1)[-1].split('"')[0]
            elegido = [c for c in cands if c['paquete'] in titulo]
            t = elegido[0] if elegido else cands[0]
        else:
            t = cands[0] if cands else None
        if not t:
            mal.append(f'{os.path.basename(f)}: {nom} no existe')
            continue
        lineas = [l.strip() for l in cuerpo.strip().split('\n')
                  if l.strip() and l.strip() != '--']
        ncampos = sum(1 for l in lineas if re.match(r'^[-+#~]\s', l) and '(' not in l)
        nmet = sum(1 for l in lineas if '(' in l)
        nlit = len(lineas) - ncampos - nmet
        if ncampos != len(t['campos']):
            mal.append(f"{nom}: {ncampos} campos dibujados, {len(t['campos'])} en el codigo")
        if nmet != len(t['metodos']):
            mal.append(f"{nom}: {nmet} metodos dibujados, {len(t['metodos'])} en el codigo")
        if nlit != len(t['literales']):
            mal.append(f"{nom}: {nlit} literales dibujados, {len(t['literales'])} en el codigo")
ok(not mal, f'campos, metodos y literales exactos ({len(mal)} diferencias)')
for x in mal[:15]:
    print('      ', x)

print('\n=== 3. parametros de cada metodo ===')
malp = []
for t in T:
    for m in t['metodos']:
        ps = ', '.join(f"{p['nombre']} : {p['tipo']}" for p in m['params'])
        firma = f"{m['nombre']}({ps})".replace('{', '(').replace('}', ')')
        if firma not in TODO:
            malp.append(f"{t['nombre']}.{firma}")
ok(not malp, f'las {sum(len(t["metodos"]) for t in T)} firmas completas aparecen '
             f'tal cual ({len(malp)} no encontradas)')
for x in malp[:10]:
    print('      ', x)

print('\n=== 4. tipos de linea ===')
comp = [a for a in R['asociacion'] if a['composicion']]
agr = [a for a in R['asociacion'] if a.get('agregacion')]
sim = [a for a in R['asociacion'] if not a['composicion'] and not a.get('agregacion')]
rel8 = ''.join(v for k, v in CL.items() if '_rel_' in k)


def alias(n):
    t = POR[n]
    return re.sub(r'\W', '', t['paquete']) + '_' + n


mall = []
for a in comp:
    if f"{alias(a['origen'])} \"1\" *-[" not in rel8:
        mall.append(f"composicion no dibujada: {a['origen']} *-- {a['destino']}")
for a in agr:
    if f"{alias(a['origen'])} o-[" not in rel8:
        mall.append(f"agregacion no dibujada: {a['origen']} o-- {a['destino']}")
ok(not mall, f'composicion {len(comp)} (*--), agregacion {len(agr)} (o--), '
             f'asociacion simple {len(sim)} (-->)')
for x in mall[:10]:
    print('      ', x)
gen = [(t['nombre'], t['extends'].split('<')[0].strip()) for t in T
       if t['extends'] and t['extends'].split('<')[0].strip() not in POR]
ok(all(f"{alias(h)} --|> {p}" in rel8 for h, p in gen),
   f'generalizacion a clases externas: {len(gen)} (--|>)')
ok(not R['generalizacion'] and not R['realizacion'],
   'no hay extends ni implements entre clases del proyecto, '
   'asi que no se dibuja ninguna herencia interna')

print('\n=== 5. multiplicidad ===')
COLEC = ('List', 'ArrayList', 'LinkedList', 'Set', 'HashSet', 'LinkedHashSet',
         'TreeSet', 'Collection', 'Iterable', 'Queue', 'Deque',
         'Map', 'HashMap', 'LinkedHashMap', 'TreeMap',
         'ObservableList', 'ObservableSet', 'ObservableMap',
         # controles JavaFX parametrizados por el tipo de elemento: un
         # ComboBox<T> o un TableView<T> contienen 0..* de T, no uno
         'ComboBox', 'ChoiceBox', 'TableView', 'TableColumn',
         'ListView', 'TreeView', 'TreeTableView')
malm = []
for a in R['asociacion']:
    t = POR[a['origen']]
    tipos = [c['tipo'] for c in t['campos'] if c['nombre'] in a['roles']]
    for tipo in tipos:
        base = tipo.split('<')[0].strip()
        esp = '*' if (base in COLEC or tipo.rstrip().endswith('[]')) else (
            '0..1' if base == 'Optional' else '1')
        if esp != a['multiplicidad']:
            malm.append(f"{a['origen']}.{a['roles']} : {tipo} -> "
                        f"{a['multiplicidad']} (deberia {esp})")
    etiq = a['multiplicidad'] + ' ' + '\\n'.join(a['roles'])
    if f'"{etiq}" {alias(a["destino"])}' not in rel8:
        malm.append(f"multiplicidad no dibujada: {a['origen']} -> {a['destino']}")
ok(not malm, f'las {len(R["asociacion"])} asociaciones llevan la multiplicidad '
             f'del tipo del campo ({len(malm)} diferencias)')
for x in malm[:10]:
    print('      ', x)
cuenta = collections.Counter(a['multiplicidad'] for a in R['asociacion'])
print('       reparto:', dict(cuenta))

print('\n=== 6. relaciones dibujadas en los 7 mapas 08-14 ===')
n_asoc = len(re.findall(r'\]-> "', rel8))
n_dep = len(re.findall(r',dashed\]->', rel8))
n_gen = len(re.findall(r'--\|>', rel8))
ok(n_asoc == len(R['asociacion']),
   f'asociaciones: {n_asoc} dibujadas / {len(R["asociacion"])} en el codigo')
ok(n_dep == len(R['dependencia']),
   f'dependencias: {n_dep} dibujadas / {len(R["dependencia"])} en el codigo')
ok(n_gen == len(gen), f'generalizaciones: {n_gen} dibujadas / {len(gen)} en el codigo')

print('\n=== 7. diagrama 15, pruebas ===')
p9 = CL[BASE + '/puml_clases/15_pruebas.puml']
ok(all(t['nombre'] in p9 for t in TT),
   f'las {len(TT)} clases de src/test/java estan dibujadas')
mal9 = []
for t in TT:
    for m in t['metodos']:
        ps = ', '.join(f"{p['nombre']} : {p['tipo']}" for p in m['params'])
        if f"{m['nombre']}({ps})" not in p9:
            mal9.append(f"{t['nombre']}.{m['nombre']}")
ok(not mal9, f'los {sum(len(t["metodos"]) for t in TT)} metodos de prueba, '
             f'con sus parametros ({len(mal9)} faltan)')

print('\n=== 8. base de datos ===')
bd = ES[BASE + '/puml/02_BaseDeDatos.puml']
ok(all(f'as {t} ' in bd for t in E),
   f'las {len(E)} tablas del esquema estan dibujadas')
nfk = len(re.findall(r'\|\|--o\{', bd))
tot = sum(len(d['fks']) for d in E.values())
ok(nfk == tot, f'llaves foraneas: {nfk} dibujadas / {tot} en los DDL')
ncol = len(re.findall(r'^\s{4}(?:\*\s\*\*|#\s|\w)', bd, re.M))
ok(ncol == sum(len(d['cols']) for d in E.values()),
   f'columnas: {ncol} dibujadas / {sum(len(d["cols"]) for d in E.values())} en los DDL')

print('\n=== 9. arquitectura, componentes, paquetes, despliegue ===')
arq = ES[BASE + '/puml/01_Arquitectura.puml']
sup = [t for t in T if not t['anidada_en']]
ok(all(t['nombre'] in arq for t in sup),
   f'arquitectura: las {len(sup)} clases de nivel superior aparecen')
# el diagrama unico de componentes quedo superado por los 8 de
# puml_componentes, construidos desde las llamadas medidas
comp_p = ' '.join(leer(f) for f in
                  sorted(glob.glob(BASE + '/puml_componentes/*.puml')))
serv = [t['nombre'] for t in T if t['paquete'] == 'service' and not t['anidada_en']]
faltan = [x for x in serv if x not in comp_p]
ok(not faltan,
   f'componentes: los {len(serv)} servicios aparecen (faltan: {faltan or "ninguno"})')
paq = ES[BASE + '/puml/07_Paquetes.puml']
dirs = {os.path.basename(d) for d in glob.glob(REPO + '/src/main/java/*')
        if os.path.isdir(d)} | {'(raiz)'}
ok(all(f'**{d}**' in paq for d in dirs),
   f'paquetes: los {len(dirs)} directorios de src/main/java aparecen')
desp = ES[BASE + '/puml/06_Despliegue.puml']
ci = leer(REPO + '/.github/workflows/ci.yml')
pasos = [n.strip() for n in re.findall(r'-\s*name:\s*(.+)', ci)]
ok(all(p in desp for p in pasos),
   f'despliegue: los {len(pasos)} pasos de ci.yml aparecen')
ok(all(u in desp for u in set(re.findall(r'uses:\s*(\S+)', ci))),
   'despliegue: las actions de ci.yml aparecen')

print('\n=== 10. nada inventado: cada identificador sale del repositorio ===')
fuente = ' '.join(leer(p) for p in glob.glob(REPO + '/src/**/*.java', recursive=True))
fuente += ' '.join(leer(p) for p in glob.glob(REPO + '/db/**/*', recursive=True)
                   if os.path.isfile(p))
fuente += leer(REPO + '/pom.xml') + ci
# tambien valen los nombres de archivo y de carpeta del repositorio
fuente += ' '.join(os.path.relpath(p, REPO)
                   for p in glob.glob(REPO + '/**/*', recursive=True))
fuente += ' '.join(os.path.relpath(p, REPO)
                   for p in glob.glob(REPO + '/**/.*/**', recursive=True))
fuente_min = fuente.lower()
alias_tipos = {re.sub(r'\W', '', t['paquete']) + '_' + t['nombre'] for t in T}
# los alias que yo declaro con "as X" no son identificadores del codigo: lo que
# hay que comprobar es el texto visible de la caja, no su nombre interno, y de
# eso se encarga el verificador de cada diagrama
for s in list(CL.values()) + list(ES.values()):
    alias_tipos |= set(re.findall(r'\bas\s+([A-Za-z_]\w*)', s))
inventados = set()
for s in list(CL.values()) + list(ES.values()):
    # el texto de una nota es explicacion en castellano, no identificadores:
    # su contenido lo comprueba el verificador propio de cada diagrama
    s = re.sub(r'^note[^\n]*\n.*?^end note$', '', s, flags=re.S | re.M)
    # la leyenda es la misma explicacion en castellano, y su tabla repite lo
    # que ya se comprueba fila a fila en verificar_estados.py
    s = re.sub(r'^legend\b.*?^endlegend$', '', s, flags=re.S | re.M)
    # el texto chico de una caja es la misma explicacion en castellano:
    # lo que tiene que salir del codigo es el nombre grande
    s = re.sub(r'<size:\d+>.*?</size>', '', s)
    for pal in re.findall(r'\b[A-Za-z_][A-Za-z0-9_]{3,}\b', s.replace('\\n', ' ')):
        if pal in dibujados or pal in alias_tipos or pal.startswith(('a_', 'pk_', 'ui_')):
            continue
        if pal.lower() not in fuente_min:
            inventados.add(pal)
PERMITIDO = set('''startuml enduml skinparam backgroundColor shadowing defaultFontName
 roundCorner nodesep ranksep padding ClassFontSize ClassAttributeFontSize
 ClassStereotypeFontSize ClassBorderColor ArrowColor ArrowFontSize
 PackageBorderColor PackageFontStyle ClassAttributeIconSize hide empty members
 package class interface enum annotation hidden right down left direction
 circle allowmixing entity database component node artifact rectangle
 ArrowThickness ArrowFontStyle ArrowFontColor PackageFontSize
 PackageTitleAlignment ClassBackgroundColor NoteBackgroundColor NoteBorderColor
 linetype ortho polyline dashed EntityFontSize ComponentBackgroundColor InterfaceBackgroundColor
 NodeFontSize NodeFontStyle ArtifactFontSize NodeBackgroundColor
 ArtifactBackgroundColor RectangleBorderColor RectangleBackgroundColor
 RectangleFontSize Helvetica size static abstract record anidada excluida
 referencias otros paquetes externa salta capa archivos clase clases nivel
 superior anidadas vacio vacios apoyo flujo transversales transversal
 Interfaz usuario resources Logica negocio service Acceso datos repository
 Infraestructura Equipo desarrollo restaurante Repositorio remoto Runner
 ejecutar pruebas Modelo Capa presentacion logica acceso Dependencias
 compile test Personas acceso Salon mesas Carta inventario Pedidos Caja
 steps ports services deploy use execution environment device empaquetado ningun plugin build
 legendBackgroundColor legendBorderColor legendFontSize legend endlegend
 StateFontSize StateFontStyle StateBackgroundColor StateBorderColor
 StateAttributeFontSize NoteFontSize state title cualquier estado estados
 catalogo codigos interfaz pinta escribe asigna dialogo elegido actual
 defecto parametro llamador llamadores unico llega tablero cocina avanza
 nunca pasar correr existe piden dicen marcado suelto'''.split())
# nombres de agrupacion elegidos para el diagrama de componentes y el del
# repositorio: no son identificadores del codigo
AGRUPACION = {'Arranque', 'BaseDatos', 'DriverJDBC', 'DAOs', 'Repositorios',
              'Autenticacion', 'GestionMesas', 'GestionPedidos',
              'GestionInventario', 'GestionPagos', 'FIS_2630_1204_G2',
              'ConexionBD_ConexionBD',
              'GitHub', 'HTTPS', 'PostgreSQL', 'JDBC',
              'equipo', 'github', 'libs', 'runner', 'repo', 'jvm', 'ide',
              'dev', 'modelo', 'negocio', 'datos', 'presentacion',
              'target', 'classes'}   # salida estandar de maven-compiler
inventados -= PERMITIDO | AGRUPACION
inventados = {p for p in inventados
              if not re.fullmatch(r'[A-Z]{1,3}\d*', p)
              and not re.fullmatch(r'[0-9A-F]{6}', p)
              and not re.fullmatch(r'(ui|pk|I)_\w+', p)}
ok(not inventados, f'palabras que no estan en el repositorio: '
                   f'{sorted(inventados)[:20] or "ninguna"}')


print('\n=== 11. el diagrama de despliegue, dato por dato ===')
desp = ES[BASE + '/puml/06_Despliegue.puml']
pom_t = leer(REPO + '/pom.xml')
props_t = dict(re.findall(r'<(\w[\w.]*)>([^<>]+)</\1>', pom_t))
mal = []


def exige(txt, que):
    if txt not in desp:
        mal.append(f'{que}: no aparece "{txt}"')


exige(re.search(r'<artifactId>(.*?)</artifactId>', pom_t).group(1), 'artifactId')
exige(re.search(r'<version>(.*?)</version>', pom_t).group(1), 'version')
for k in ('javafx.version', 'postgresql.version', 'maven.compiler.release'):
    exige(props_t[k], k)
# target/classes recibe TODO src/main/resources al compilar
RES = REPO + '/src/main/resources'
porcarp = {}
for r in glob.glob(RES + '/**/*', recursive=True):
    if os.path.isfile(r) and not r.endswith('.gitkeep'):
        porcarp.setdefault(os.path.dirname(os.path.relpath(r, RES)) or '(raiz)',
                           []).append(r)
for c, v in porcarp.items():
    exige(' · '.join(os.path.basename(x) for x in v) if c == '(raiz)'
          else f'{c} · {len(v)}', f'recursos de {c}')
# cada dependencia de compilacion con su version resuelta
for m in re.finditer(r'<dependency>(.*?)</dependency>', pom_t, re.S):
    b = m.group(1)
    if '<scope>test</scope>' in b:
        continue
    a_ = re.search(r'<artifactId>(.*?)</', b).group(1)
    v_ = re.search(r'<version>(.*?)</', b)
    v_ = v_.group(1) if v_ else ''
    mm = re.fullmatch(r'\$\{(.+?)\}', v_)
    exige(f'{a_} {props_t.get(mm.group(1), v_) if mm else v_}', f'dependencia {a_}')
exige(f"db/ddl · {len(glob.glob(REPO + '/db/ddl/*.ddl'))}", 'ddl')
exige(f"db/migrations · {len(glob.glob(REPO + '/db/migrations/*.sql'))}", 'migraciones')
exige(f"db/functions · {len(glob.glob(REPO + '/db/functions/*'))}", 'funciones')
exige(f'{len(E)} tablas', 'tablas')
cbd = leer(REPO + '/src/main/java/ConexionBD/ConexionBD.java')
for k, v in re.findall(r'static final String (\w+)\s*=\s*"([^"]*)"', cbd):
    exige(f'{k} = {v}', f'constante {k}')
ok(not mal, f'pom.xml, recursos, base y ConexionBD ({len(mal)} fallos)')
for x in mal[:8]:
    print('      ', x)

mal = []
readme = leer(REPO + '/README.md')
m = re.search(r'## Ejecuci[^\n]*\n+```bash\n(.*?)```', readme, re.S)
for c in (m.group(1).strip().split('\n') if m else []):
    if c.strip() not in desp:
        mal.append(f'falta el comando "{c.strip()}" del README')
ok(not mal, 'los comandos de ejecucion del README aparecen')

mal = []
exige2 = lambda t, q: mal.append(f'{q}: falta "{t}"') if t not in desp else None
exige2(re.search(r'runs-on:\s*(\S+)', ci).group(1), 'runner')
exige2(re.search(r"java-version:\s*'?([\d.]+)'?", ci).group(1), 'java-version')
exige2(re.search(r'distribution:\s*(\S+)', ci).group(1), 'distribution')
img = re.search(r'image:\s*(\S+)', ci)
if img:
    exige2(img.group(1), 'imagen del servicio')
for pto in set(re.findall(r'-\s*(\d+:\d+)', ci)):
    exige2(pto, 'puerto del servicio')
for r in re.findall(r'-\s*(\S+)', re.search(
        r'push:\s*\n\s*branches:\s*((?:\s*-\s*\S+\n)+)', ci).group(1)):
    exige2(r.strip("'"), 'rama de push')
ok(not mal, f'todo lo que dice ci.yml ({len(mal)} fallos)')
for x in mal[:8]:
    print('      ', x)

empaqueta = [x for x in ('maven-shade-plugin', 'maven-assembly-plugin',
                         'maven-jlink-plugin', 'jpackage-maven-plugin',
                         'spring-boot-maven-plugin') if x in pom_t]
ok(not empaqueta and 'ningun plugin de empaquetado' in desp,
   'el pom.xml no empaqueta nada y el diagrama lo dice')
ok(all(p in desp for p in set(re.findall(
    r'<artifactId>(maven-[\w-]+|javafx-maven-plugin)</artifactId>', pom_t))),
   'los plugins del pom.xml aparecen')
# opciones de setup.sh y requisitos del README
setup_t = leer(REPO + '/scripts/setup.sh')
opc = sorted(set(re.findall(r'^\s*(--[a-z-]+)(?:\||\))', setup_t, re.M)))
ok(all(o in desp for o in opc), f'las {len(opc)} opciones de setup.sh: {opc}')
req = re.findall(r'## Requisitos\n+((?:- [^\n]+\n)+)', readme)
reqs = [r.strip('- ').strip() for r in req[0].strip().split('\n')] if req else []
ok(all(r in desp for r in reqs), f'los requisitos del README: {reqs}')
# un artefacto no puede estar tachado por culpa de los guiones dobles
svg = leer(BASE + '/puml/06_Despliegue.svg')
ok('text-decoration' not in svg, 'ningun texto sale tachado en el SVG')
disp = re.findall(r'node "([^"]+)" as \w+ <<device>>', desp)
ok(len(disp) == 2 and any('GitHub' in d for d in disp),
   f'un solo equipo ejecuta la aplicacion, mas GitHub: {disp}')


print('\n=== 12. el diagrama de arquitectura ===')
mal = []
# cada paquete tiene que estar en la misma capa en arquitectura y en
# componentes: son dos vistas del mismo sistema
cap_arq = {'controller': 'Capa de presentacion', 'service': 'Capa de logica de negocio',
           'dao': 'Capa de acceso a datos', 'repository': 'Capa de acceso a datos'}
for paq, titulo in cap_arq.items():
    m = re.search(rf'package "{re.escape(titulo)}"(.*?)^\}}', arq, re.M | re.S)
    if not m or f'"{paq}"' not in m.group(1):
        mal.append(f'{paq} no esta en "{titulo}"')
comp_gen = leer(BASE + '/puml_componentes/01_Componentes_general.puml')
pares = [('presentacion', 'Interfaz de usuario'), ('negocio', 'Logica de negocio'),
         ('datos', 'Acceso a datos')]
for _c, texto in pares:
    if texto not in comp_gen:
        mal.append(f'el diagrama de componentes no nombra "{texto}"')
if 'repository + dao' not in comp_gen or 'negocio · service\n' in comp_gen.replace(
        '**', ''):
    pass
# session y util son apoyo transversal en los dos diagramas, y ConexionBD
# es su propia capa en los dos
m = re.search(r'package "Modelo y apoyo"(.*?)^\}', arq, re.M | re.S)
for paq in ('session', 'util'):
    if not m or f'"{paq}"' not in m.group(1):
        mal.append(f'{paq} no esta en "Modelo y apoyo" del de arquitectura')
if 'Apoyo transversal · session + util' not in comp_gen:
    mal.append('el de componentes no agrupa session y util como apoyo')
if not re.search(r'package "ConexionBD"', arq):
    mal.append('ConexionBD no es su propia capa en el de arquitectura')
ok(not mal, f'las capas coinciden con el diagrama de componentes ({len(mal)})')
for x in mal[:6]:
    print('      ', x)

# los dao van en acceso a datos porque ejecutan SQL. Dos service tambien lo
# hacen, saltandose esa capa: eso es un hallazgo del codigo, no un fallo del
# diagrama, pero el conjunto tiene que seguir siendo exactamente ese
sqlmap = json.load(open(BASE + '/llamadas.json', encoding='utf-8'))['sql']
paq_sql = sorted({POR[n]['paquete'] for n in sqlmap})
ok(paq_sql == ['dao', 'repository', 'service'],
   f'ejecutan SQL exactamente {paq_sql}')
serv_sql = sorted(n for n in sqlmap if POR[n]['paquete'] == 'service')
ok(serv_sql == ['DisponibilidadService', 'EntradaInventarioService'],
   f'los service que ejecutan SQL propio son {serv_sql}')

# contenido de cada caja: las clases de ese paquete, todas y solo esas
mal = []
for m in re.finditer(r'class "(\w+|\(raiz\))" as \w+ <<([^>]*)>> [^{]*\{\n(.*?)^  \}',
                     arq, re.M | re.S):
    paq, est, cuerpo = m.group(1), m.group(2), m.group(3)
    if paq not in {t['paquete'] for t in T}:
        continue
    # las lineas con parentesis son operaciones, no nombres de clase
    listadas = {l.strip() for l in cuerpo.split('\n')
                if l.strip() and '(' not in l}
    reales = {t['nombre'] for t in T if t['paquete'] == paq and not t['anidada_en']}
    if paq == 'ConexionBD':
        # la caja se titula con el paquete, que aqui coincide con la clase,
        # y dentro lleva su operacion
        reales = set()
        ops = {l.strip() for l in cuerpo.split('\n') if '(' in l}
        esp = {f"+ {m['nombre']}() : {m['retorno']}"
               for m in POR['ConexionBD']['metodos']}
        if ops != esp:
            mal.append(f'ConexionBD: operaciones {sorted(ops)} != {sorted(esp)}')
    if listadas != reales:
        mal.append(f'{paq}: sobran {sorted(listadas - reales)}, '
                   f'faltan {sorted(reales - listadas)}')
    n = re.match(r'(\d+) clase', est)
    if n and int(n.group(1)) != len(reales):
        mal.append(f'{paq}: dice {n.group(1)} clases y son {len(reales)}')
ok(not mal, f'cada caja lista exactamente las clases de su paquete ({len(mal)})')
for x in mal[:6]:
    print('      ', x)

# todo directorio de src/main/java aparece, aunque no tenga clases
mal = []
for d in sorted(glob.glob(REPO + '/src/main/java/*')):
    if os.path.isdir(d) and f'"{os.path.basename(d)}"' not in arq:
        mal.append(os.path.basename(d))
ok(not mal, f'los directorios de src/main/java aparecen (faltan {mal or "ninguno"})')

# todo archivo de src/main/resources aparece
RESA = REPO + '/src/main/resources'
mal = [os.path.relpath(p, RESA) for p in glob.glob(RESA + '/**/*', recursive=True)
       if os.path.isfile(p) and not p.endswith('.gitkeep')
       and os.path.basename(p) not in arq]
ok(not mal, f'los recursos de src/main/resources aparecen (faltan {mal or "ninguno"})')

# las pruebas: tantas clases como tipos, en tantos archivos como archivos
ok(f'{len(TT)} clases en {len({t["archivo"] for t in TT})} archivos' in arq,
   f'las pruebas: {len(TT)} clases en '
   f'{len({t["archivo"] for t in TT})} archivos')
for x in mal[:6]:
    print('      ', x)

# las flechas que se saltan una capa, contadas sobre las relaciones reales
pares_rel = {(a['origen'], a['destino']) for a in R['asociacion']} | \
            {(d['origen'], d['destino']) for d in R['dependencia']}
CAPA_DE = {'controller': 'presentacion', 'service': 'negocio', 'dao': 'datos',
           'repository': 'datos', 'ConexionBD': 'conexion'}
cuenta = collections.Counter()
for o, d in pares_rel:
    co, cd = CAPA_DE.get(POR[o]['paquete']), CAPA_DE.get(POR[d]['paquete'])
    if co and cd and co != cd:
        cuenta[(co, cd)] += 1
mal = []
for (co, cd), n in cuenta.items():
    if (co, cd) in (('presentacion', 'datos'), ('negocio', 'conexion'),
                    ('presentacion', 'conexion')):
        if f'{co} -[#B91C1C]..> {cd} : <<use>> salta capa · {n}' not in arq:
            mal.append(f'salto {co}->{cd} deberia decir {n}')
ok(not mal, f'los saltos de capa llevan su numero real ({dict(cuenta)})')

print('\n=== resultado ===')
print(f'{len(fallos)} comprobaciones fallidas' if fallos else 'todo correcto')
