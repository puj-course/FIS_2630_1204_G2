"""Diagramas de componentes de GastroFlow.

Un componente es una clase de controller, service, dao o repository, mas
ConexionBD y Main: en este proyecto no hay modulos ni jar separados, asi que
la unidad que se compone y se reemplaza es la clase.

La interfaz que un componente PROVEE es su propio nombre, y sus operaciones
son exactamente los metodos que otra clase le llama, resueltos con
tree-sitter en extraer_llamadas.py. Nada esta escrito a mano.
"""
import json, os, re, glob, collections

REPO = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                      'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
DIR = BASE + '/puml_componentes'
os.makedirs(DIR, exist_ok=True)

T = json.load(open(BASE + '/clases.json', encoding='utf-8'))
LL = json.load(open(BASE + '/llamadas.json', encoding='utf-8'))
E = json.load(open(BASE + '/esquema.json', encoding='utf-8'))
POR = {t['nombre']: t for t in T}
pom = open(REPO + '/pom.xml', encoding='utf-8').read()
EXCL = {l.strip().rsplit('/', 1)[-1].replace('.java', '')
        for l in re.findall(r'<exclude>(.+?)</exclude>', pom)}

PAQ_COMP = ('controller', 'service', 'dao', 'repository', 'ConexionBD',
            'session', 'util', '(raiz)')
# Las capas son las mismas que en el diagrama de arquitectura. dao va en
# acceso a datos, no en negocio: los tres DAO ejecutan SQL directamente.
# Ojo: DisponibilidadService y EntradaInventarioService tambien ejecutan SQL
# propio, saltandose la capa de datos. Siguen contando como negocio por su
# paquete, pero aparecen en el mapa componente-tabla.
# Mismas capas que el diagrama de arquitectura: ConexionBD es su propia
# capa y session y util son apoyo transversal.
SUB = {'controller': 'ui', '(raiz)': 'ui', 'service': 'negocio',
       'dao': 'datos', 'repository': 'datos', 'ConexionBD': 'conexion',
       'session': 'apoyo', 'util': 'apoyo'}
COLOR = {'ui': '#DBEAFE', 'negocio': '#DCFCE7', 'datos': '#FFEDD5',
         'conexion': '#F1F5F9', 'apoyo': '#F3E8FF'}

COMPONENTES = [t['nombre'] for t in T
               if t['paquete'] in PAQ_COMP and not t['anidada_en']]

# operaciones que cada componente provee: las que alguien de fuera le llama
provee = collections.defaultdict(set)
requiere = collections.defaultdict(lambda: collections.defaultdict(set))
for l in LL['llamadas']:
    o, d = l['origen'], l['destino']
    if o in COMPONENTES and d in COMPONENTES and o != d:
        provee[d].add(l['metodo'])
        requiere[o][d].add(l['metodo'])

CAB = '''@startuml
allowmixing
skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam defaultFontName Helvetica
skinparam roundCorner 4
skinparam nodesep 30
skinparam ranksep 60
skinparam ArrowColor #475569
skinparam ArrowFontSize 10
skinparam PackageBorderColor #94A3B8
skinparam PackageFontStyle bold
skinparam ComponentBackgroundColor #FFFFFF
skinparam ComponentFontSize 12
skinparam InterfaceBackgroundColor #FFFFFF
skinparam InterfaceFontSize 11
skinparam ArtifactBackgroundColor #FFFFFF
skinparam ArtifactFontSize 11
skinparam DatabaseBackgroundColor #FFFFFF
'''

# que .fxml carga cada controller: lo dice el atributo fx:controller
VISTAS = REPO + '/src/main/resources/views'
PAREJA, FXID = {}, {}
for ruta in sorted(glob.glob(VISTAS + '/*.fxml')):
    x = open(ruta, encoding='utf-8').read()
    m = re.search(r'fx:controller="[\w.]*\.(\w+)"', x)
    if not m:
        continue
    PAREJA[m.group(1)] = os.path.basename(ruta)
    FXID[m.group(1)] = (len(set(re.findall(r'fx:id="(\w+)"', x))),
                        len(set(re.findall(r'onAction="#(\w+)"', x))))

# que .css lleva cada pantalla. Dos sitios lo dicen, los dos en Main:
#   - la lista MODULOS, que empareja fxml y css de cada modulo
#   - un metodo que carga un .fxml y anade una hoja a su Scene (el login)
# En los dos casos quien carga la hoja es Main, no el controller.
main_src = open(REPO + '/src/main/java/Main.java', encoding='utf-8').read()
CSS = {}


def _ctrl_de(fxml):
    return next((c for c, v in PAREJA.items() if v == os.path.basename(fxml)), None)


for fxml, css in re.findall(r'"(/views/[^"]+)"\s*,\s*(?:"(/css/[^"]+)"|null)',
                            main_src):
    c = _ctrl_de(fxml)
    if c and css:
        CSS[c] = os.path.basename(css)
for cuerpo in re.findall(r'\n    (?:private|public)[^\n]*\([^)]*\)\s*\{(.*?)\n    \}',
                         main_src, re.S):
    fx = re.search(r'"(/views/[^"]+)"', cuerpo)
    hoja = re.search(r'getStylesheets\(\)\.add\([^)]*"(/css/[^"]+)"', cuerpo)
    if fx and hoja:
        c = _ctrl_de(fx.group(1))
        if c:
            CSS[c] = os.path.basename(hoja.group(1))
# hojas que Main anade a una escena suya, sin .fxml detras
CSS_MAIN = sorted({os.path.basename(h) for cuerpo in
                   re.findall(r'\n    (?:private|public)[^\n]*\([^)]*\)\s*\{(.*?)\n    \}',
                              main_src, re.S)
                   if not re.search(r'"(/views/[^"]+)"', cuerpo)
                   for h in re.findall(r'getStylesheets\(\)\.add\([^)]*"(/css/[^"]+)"',
                                       cuerpo)})


def ali(s):
    return re.sub(r'\W', '_', s)


def firmas(n):
    """Las operaciones que provee n, con los tipos de sus parametros.

    Cuando hay sobrecargas se listan todas: la llamada solo da el nombre del
    metodo, asi que no se puede saber cual de las dos usa el cliente, y
    dibujar una sola seria afirmar de mas."""
    out = []
    for op in sorted(provee[n]):
        for m in POR[n]['metodos']:
            if m['nombre'] != op:
                continue
            ps = ', '.join(p['tipo'].split('<')[0].strip()
                           for p in m['params'])
            out.append(f'{op}({ps})')
    return out


def marca(n):
    return '\\n<size:9>excluida en pom.xml</size>' if n in EXCL else ''


def comp(n, extra=()):
    lineas = list(extra)
    cuerpo = ''.join(f'\\n<size:9>{x}</size>' for x in lineas)
    return f'  component "**{n}**{cuerpo}{marca(n)}" as {ali(n)}'


# ===========================================================================
# 01. vista general: los cuatro subsistemas y lo que se piden entre si
# ===========================================================================
entre = collections.Counter()
for o, dests in requiere.items():
    for d, ops in dests.items():
        a, b = SUB[POR[o]['paquete']], SUB[POR[d]['paquete']]
        if a != b:
            entre[(a, b)] += len(ops)
cuantos = collections.Counter(SUB[POR[n]['paquete']] for n in COMPONENTES)

L = [CAB]
NOMBRE_SUB = {'ui': 'Interfaz de usuario · controller + Main',
              'negocio': 'Logica de negocio · service',
              'datos': 'Acceso a datos · repository + dao',
              'conexion': 'ConexionBD',
              'apoyo': 'Apoyo transversal · session + util'}
for s in ('ui', 'negocio', 'datos', 'conexion', 'apoyo'):
    L.append(f'component "**{NOMBRE_SUB[s]}**\\n'
             f'<size:10>{cuantos[s]} '
             f'componente{"s" if cuantos[s] != 1 else ""}</size>" '
             f'as {s} {COLOR[s]}')
L.append(f'database "**gastroflow**\\n<size:10>{len(E)} tablas</size>" as bd #FFFFFF')
for (a, b), n in sorted(entre.items()):
    L.append(f'{a} ..> {b} : <<use>> {n} operaciones')
L.append('conexion ..> bd : JDBC')   # el alias es conexion, no infra
L.append('@enduml')
open(f'{DIR}/01_Componentes_general.puml', 'w', encoding='utf-8').write(
    '\n'.join(L) + '\n')


# ===========================================================================
# 02-04. un diagrama por subsistema: lo que provee y lo que requiere
# ===========================================================================
def diagrama(archivo, titulo, paquetes, extras=None, con_clientes=False):
    propios = [n for n in COMPONENTES if POR[n]['paquete'] in paquetes]
    # proveedores de fuera a los que este subsistema llama
    fuera = sorted({d for o in propios for d in requiere[o]
                    if d not in propios})
    L = [CAB]
    L.append(f'package "{titulo}" {COLOR[SUB[POR[propios[0]]["paquete"]]]} {{')
    for n in sorted(propios):
        L += [comp(n, (extras or {}).get(n, ()))]
    COLS = 4
    pr = sorted(propios)
    for i in range(len(pr)):
        if i % COLS and i:
            L.append(f'  {ali(pr[i-1])} -[hidden]right- {ali(pr[i])}')
        if i >= COLS:
            L.append(f'  {ali(pr[i-COLS])} -[hidden]down- {ali(pr[i])}')
    L.append('}')

    # interfaz provista: el nombre del componente, con las operaciones que
    # otro componente le llama de verdad
    usados = {d for o in COMPONENTES for d in requiere[o]}
    for n in sorted(set(propios) | set(fuera)):
        if n not in usados:
            continue
        # cada linea lleva su propia etiqueta de tamano: si <size> abarca un
        # salto de linea, PlantUML deja el cierre </size> escrito en el dibujo
        ops = '\\n'.join(f'<size:9>{f}</size>' for f in firmas(n))
        L.append(f'interface "{n}\\n{ops}" as I_{ali(n)}')
    if fuera:
        L.append('package "fuera del subsistema" #FAFAFA {')
        for n in fuera:
            L.append(f'  component "**{n}**\\n<size:9>{POR[n]["paquete"]}'
                     f'</size>{marca(n)}" as {ali(n)} '
                     f'{COLOR[SUB[POR[n]["paquete"]]]}')
        COLS = 5
        for i in range(len(fuera)):
            if i % COLS and i:
                L.append(f'  {ali(fuera[i-1])} -[hidden]right- {ali(fuera[i])}')
            if i >= COLS:
                L.append(f'  {ali(fuera[i-COLS])} -[hidden]down- {ali(fuera[i])}')
        L.append('}')
    # rejilla sobre las interfaces: sin ella PlantUML las pone en una fila
    ifs = [f'I_{ali(n)}' for n in sorted(set(propios) | set(fuera))
           if n in usados]
    COLS = 5
    for i in range(len(ifs)):
        if i % COLS and i:
            L.append(f'{ifs[i-1]} -[hidden]right- {ifs[i]}')
        if i >= COLS:
            L.append(f'{ifs[i-COLS]} -[hidden]down- {ifs[i]}')
    for n in sorted(set(propios) | set(fuera)):
        if n in usados:
            L.append(f'{ali(n)} -up- I_{ali(n)}')
    for o in sorted(propios):
        for d, ops in sorted(requiere[o].items()):
            L.append(f'{ali(o)} ..> I_{ali(d)} : <<use>> {len(ops)}')
    if con_clientes:
        # quien consume este subsistema desde fuera
        clientes = sorted({o for o in requiere if o not in propios
                           and any(d in propios for d in requiere[o])})
        if clientes:
            L.append('package "componentes que lo consumen" #FAFAFA {')
            for n in clientes:
                L.append(f'  component "**{n}**\\n<size:9>{POR[n]["paquete"]}'
                         f'</size>{marca(n)}" as {ali(n)} '
                         f'{COLOR[SUB[POR[n]["paquete"]]]}')
            COLS = 6
            for i in range(len(clientes)):
                if i % COLS and i:
                    L.append(f'  {ali(clientes[i-1])} -[hidden]right- '
                             f'{ali(clientes[i])}')
                if i >= COLS:
                    L.append(f'  {ali(clientes[i-COLS])} -[hidden]down- '
                             f'{ali(clientes[i])}')
            L.append('}')
            for o in clientes:
                for d, ops in sorted(requiere[o].items()):
                    if d in propios:
                        L.append(f'{ali(o)} ..> I_{ali(d)} : <<use>> {len(ops)}')
    L.append('@enduml')
    open(f'{DIR}/{archivo}.puml', 'w', encoding='utf-8').write('\n'.join(L) + '\n')
    return len(propios), len(fuera), sum(len(v) for v in
                                         (requiere[o] for o in propios) for v in [v])


extras_ui = {}
for c, fx in PAREJA.items():
    n_id, n_act = FXID[c]
    extras_ui[c] = (f'{fx} · {n_id} fx:id · {n_act} onAction',) + \
                   ((CSS[c],) if c in CSS else ())
extras_ui['Main'] = ((f'{len(re.findall(r"new Modulo\(", main_src))} modulos '
                      f'en MODULOS',) + tuple(CSS_MAIN))

diagrama('02_Componentes_ui', 'Interfaz de usuario · controller + Main',
         ('controller', '(raiz)'), extras_ui)
diagrama('03_Componentes_negocio', 'Logica de negocio · service', ('service',))
diagrama('07_Componentes_infraestructura',
         'ConexionBD · session · util',
         ('ConexionBD', 'session', 'util'), con_clientes=True)

# ===========================================================================
# 05. acceso a datos: cada repositorio con las tablas que lee y escribe
# ===========================================================================
sql = LL['sql']
usadas = sorted({t for d in sql.values() for t in d['tablas']})
sueltas = sorted(set(E) - set(usadas))
quien = collections.defaultdict(list)
for n, d in sql.items():
    for t in d['tablas']:
        quien[t].append(n)
COMPARTIDAS = sorted(t for t, v in quien.items() if len(v) > 1)
PROPIAS = sorted(t for t, v in quien.items() if len(v) == 1)

# --- 04: cada repositorio con sus tablas exclusivas -------------------------
L = [CAB]
L.append('package "Acceso a datos · repository + dao" #FFEDD5 {')
for n in sorted(COMPONENTES):
    # repository y dao siempre; de service, solo los dos que ejecutan SQL
    # propio: los otros diez no pintan nada en un mapa componente-tabla
    if POR[n]['paquete'] not in ('repository', 'dao') and \
            not (POR[n]['paquete'] == 'service' and n in sql):
        continue
    d = sql.get(n, {})
    extra = []
    comp_n = [t for t in d.get('tablas', []) if t in COMPARTIDAS]
    if comp_n:
        extra.append('comparte: ' + ', '.join(comp_n))
    if d.get('dinamicas'):
        extra.append('elige en ejecucion: ' + ', '.join(d['dinamicas']))
    extra += [f'llama {f}()' for f in d.get('funciones', [])]
    if not d:
        extra.append('sin SQL propio')
    L += [comp(n, extra)]
# tambien los service con SQL propio: si no, el mapa componente-tabla
# esconde quien escribe de verdad en productos e ingredientes
nm = [n for n in sorted(COMPONENTES)
      if POR[n]['paquete'] in ('repository', 'dao')
      or (POR[n]['paquete'] == 'service' and n in sql)]
COLS = 4
for i in range(len(nm)):
    if i % COLS and i:
        L.append(f'  {ali(nm[i-1])} -[hidden]right- {ali(nm[i])}')
    if i >= COLS:
        L.append(f'  {ali(nm[i-COLS])} -[hidden]down- {ali(nm[i])}')
L.append('}')
L.append(f'package "tablas con un unico repositorio · {len(PROPIAS)}" #DCFCE7 {{')
for t in PROPIAS:
    L.append(f'  database "{t}" as T_{t} #FFFFFF')
COLS = 4
for i in range(len(PROPIAS)):
    if i % COLS and i:
        L.append(f'  T_{PROPIAS[i-1]} -[hidden]right- T_{PROPIAS[i]}')
    if i >= COLS:
        L.append(f'  T_{PROPIAS[i-COLS]} -[hidden]down- T_{PROPIAS[i]}')
L.append('}')
if sueltas:
    L.append(f'package "tablas que ningun repositorio consulta · {len(sueltas)}"'
             f' #FEF2F2 {{')
    for t in sueltas:
        L.append(f'  database "{t}" as S_{t} #FFFFFF')
    for i in range(1, len(sueltas)):
        if i % 3:
            L.append(f'  S_{sueltas[i-1]} -[hidden]right- S_{sueltas[i]}')
        else:
            L.append(f'  S_{sueltas[i-3]} -[hidden]down- S_{sueltas[i]}')
    L.append('}')
for t in PROPIAS:
    n = quien[t][0]
    escribe = t in sql[n]['escribe']
    din = ' (en ejecucion)' if t in sql[n].get('dinamicas', []) else ''
    L.append(f'{ali(n)} {"-->" if escribe else "..>"} T_{t} : '
             f'{"lee y escribe" if escribe else "lee"}{din}')
# ConexionBD: todo repositorio pide la conexion por getConnection()
L.append('package "Conexion" #F1F5F9 {')
L.append('  component "**ConexionBD**" as ConexionBD')
L.append('}')
ops_cbd = '\\n'.join(f'<size:9>{o}()</size>' for o in sorted(provee['ConexionBD']))
L.append(f'interface "ConexionBD\\n{ops_cbd}" as I_ConexionBD')
L.append('ConexionBD -up- I_ConexionBD')
for n in sorted(requiere):
    if 'ConexionBD' in requiere[n] and POR[n]['paquete'] in ('repository', 'dao'):
        L.append(f'{ali(n)} ..> I_ConexionBD : <<use>> '
                 f'{len(requiere[n]["ConexionBD"])}')
# dependencias entre componentes de la propia capa de datos
datos_comp = {n for n in COMPONENTES
              if POR[n]['paquete'] in ('repository', 'dao')
              or (POR[n]['paquete'] == 'service' and n in sql)}
for o in sorted(datos_comp):
    for d, ops in sorted(requiere[o].items()):
        if d in datos_comp:
            opsl = '\\n'.join(f'<size:9>{f}</size>' for f in firmas(d))
            L.append(f'interface "{d}\\n{opsl}" as I_{ali(d)}')
            L.append(f'{ali(d)} -up- I_{ali(d)}')
            L.append(f'{ali(o)} ..> I_{ali(d)} : <<use>> {len(ops)}')
L.append('@enduml')
open(f'{DIR}/04_Componentes_datos.puml', 'w', encoding='utf-8').write(
    '\n'.join(L) + '\n')

# --- 05: las tablas que comparten varios repositorios -----------------------
L = [CAB, 'left to right direction']
tocan = sorted({n for t in COMPARTIDAS for n in quien[t]})
L.append('package "repository + dao" #FFEDD5 {')
for n in tocan:
    L += [comp(n)]
L.append('}')
L.append(f'package "tablas que tocan dos o mas repositorios · '
         f'{len(COMPARTIDAS)}" #DCFCE7 {{')
for t in COMPARTIDAS:
    escritores = [n for n in quien[t] if t in sql[n]['escribe']]
    pie = (f'\\n<size:9>escriben {len(escritores)}</size>' if escritores
           else '\\n<size:9>solo lectura</size>')
    L.append(f'  database "{t}{pie}" as T_{t} #FFFFFF')
L.append('}')
for t in COMPARTIDAS:
    for n in quien[t]:
        escribe = t in sql[n]['escribe']
        din = ' (en ejecucion)' if t in sql[n].get('dinamicas', []) else ''
        L.append(f'{ali(n)} {"-->" if escribe else "..>"} T_{t} : '
                 f'{"escribe" if escribe else "lee"}{din}')
L.append('@enduml')
open(f'{DIR}/05_Componentes_datos_compartidos.puml', 'w',
     encoding='utf-8').write('\n'.join(L) + '\n')

# ===========================================================================
# 06. artefactos: que archivos realizan cada parte y como se enlazan
# ===========================================================================
L = [CAB]
porpaq = collections.defaultdict(list)
for n in COMPONENTES:
    porpaq[POR[n]['paquete']].append(n)
# el titulo decia src/main/java pero solo se dibujan los paquetes que
# aportan componentes: entity, dto, enums, exceptions y database quedan
# fuera, y conviene decirlo en vez de dar a entender que estan todos
otros = sorted({os.path.basename(d) for d in glob.glob(REPO + "/src/main/java/*")
                if os.path.isdir(d)} - set(porpaq))
L.append('package "src/main/java · paquetes con componentes'
         '\\n<size:9>no se dibujan ' + ', '.join(otros)
         + '</size>" #F1F5F9 {' if otros else
         'package "src/main/java" #F1F5F9 {')
for p_ in sorted(porpaq):
    dir_ = '(raiz)' if p_ == '(raiz)' else p_ + '/'
    # se cuentan los .java del directorio, no los componentes: controller
    # tiene 8 archivos y uno de ellos (PagoController) esta vacio
    d_real = (REPO + "/src/main/java") if p_ == "(raiz)" else f"{REPO}/src/main/java/{p_}"
    njava = len(glob.glob(f'{d_real}/*.java')) if os.path.isdir(d_real) \
        else len(porpaq[p_])
    L.append(f'  artifact "{dir_}\\n<size:9>{njava} .java</size>"'
             f' as J_{ali(p_)} {COLOR[SUB[p_]]}')
js = [f'J_{ali(p_)}' for p_ in sorted(porpaq)]
for i_ in range(1, len(js)):
    L.append(f'  {js[i_-1]} -[hidden]right- {js[i_]}')
L.append('}')

L.append('package "src/main/resources" #FEF9C3 {')
for c, fx in sorted(PAREJA.items()):
    L.append(f'  artifact "views/{fx}" as A_{ali(fx)}')
for css in sorted(set(CSS.values()) | set(CSS_MAIN)):
    L.append(f'  artifact "css/{css}" as A_{ali(css)}')
res = [f'A_{ali(fx)}' for _c, fx in sorted(PAREJA.items())] + \
      [f'A_{ali(c)}' for c in sorted(set(CSS.values()) | set(CSS_MAIN))]
COLS = 4
for i in range(len(res)):
    if i % COLS and i:
        L.append(f'  {res[i-1]} -[hidden]right- {res[i]}')
    if i >= COLS:
        L.append(f'  {res[i-COLS]} -[hidden]down- {res[i]}')
L.append('}')

L.append('package "db" #DCFCE7 {')
L.append(f'  artifact "ddl\\n<size:9>{len(glob.glob(REPO + "/db/ddl/*.ddl"))}'
         f' .ddl</size>" as A_ddl')
L.append(f'  artifact "migrations\\n<size:9>'
         f'{len(glob.glob(REPO + "/db/migrations/*.sql"))} .sql</size>" as A_mig')
L.append(f'  artifact "functions\\n<size:9>'
         f'{len(glob.glob(REPO + "/db/functions/*"))} archivos</size>" as A_fun')
L.append('  artifact "scripts/setup.sh" as A_setup')
L.append('  A_ddl -[hidden]right- A_mig')
L.append('  A_mig -[hidden]right- A_fun')
L.append('  A_fun -[hidden]right- A_setup')
L.append('}')
L.append(f'database "**gastroflow**\\n<size:9>{len(E)} tablas</size>" as bd #FFFFFF')

# cada .fxml declara su controller en fx:controller
for c, fx in sorted(PAREJA.items()):
    L.append(f'A_{ali(fx)} ..> J_controller : <<fx:controller>> {c}')
# Main es quien anade cada hoja a la Scene; el controller no carga ninguna
for c, css in sorted(CSS.items()):
    L.append(f'J_{ali("(raiz)")} ..> A_{ali(css)} : <<carga>> pantalla de {c}')
for css in CSS_MAIN:
    L.append(f'J_{ali("(raiz)")} ..> A_{ali(css)} : <<carga>>')
L.append('A_setup ..> A_ddl : <<ejecuta>>')
L.append('A_setup ..> A_mig : <<ejecuta>>')
L.append('A_setup ..> A_fun : <<ejecuta>>')
L.append('A_setup --> bd : <<crea>>')
L.append('J_ConexionBD --> bd : <<JDBC>>')
L.append('@enduml')
open(f'{DIR}/06_Componentes_artefactos.puml', 'w', encoding='utf-8').write(
    '\n'.join(L) + '\n')

print(f'componentes: {len(COMPONENTES)}')
for s in ('ui', 'negocio', 'datos', 'conexion', 'apoyo'):
    print(f'  {s:8} {cuantos[s]}')
print(f'interfaces provistas (componentes a los que alguien llama): {len(provee)}')
print(f'operaciones distintas en esas interfaces: {sum(len(v) for v in provee.values())}')
print(f'dependencias componente -> componente: '
      f'{sum(len(v) for v in requiere.values())}')
nadie = sorted(n for n in COMPONENTES if n not in provee)
print(f'\ncomponentes a los que no llama nadie ({len(nadie)}):')
for n in nadie:
    print(f'   {POR[n]["paquete"]:11} {n}{"  (excluida en pom.xml)" if n in EXCL else ""}')
print(f'\ntablas consultadas desde el codigo: {len(usadas)} de {len(E)}')
print('tablas sin repositorio:', sueltas or 'ninguna')

# ===========================================================================
# 13 y 14. las versiones resumidas de los diagramas 04 y 06
#
# Van aqui y no en gen_componentes_simple.py porque usan las mismas tablas y
# los mismos artefactos ya medidos arriba: duplicar la extraccion en el otro
# script seria arriesgarse a que las dos versiones de un diagrama dejen de
# decir lo mismo.
# ===========================================================================
CAB_SIMPLE = '''@startuml
allowmixing
left to right direction
skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam defaultFontName Helvetica
skinparam roundCorner 6
skinparam nodesep 28
skinparam ranksep 70
skinparam ArrowColor #475569
skinparam ArrowThickness 1.1
skinparam ComponentBackgroundColor #FFFFFF
skinparam ComponentFontSize 12
skinparam ComponentBorderColor #334155
skinparam ArtifactBackgroundColor #FFFFFF
skinparam ArtifactFontSize 12
skinparam DatabaseBackgroundColor #FFFFFF
'''
COLOR_CAPA = {'ui': '#DBEAFE', 'negocio': '#DCFCE7', 'datos': '#FFEDD5',
              'conexion': '#F1F5F9', 'apoyo': '#F3E8FF'}

# --- 13: resumen de 04, una caja por componente y una por tabla ------------
L = [CAB_SIMPLE]
puesta = set()
for n in sorted(sql):
    L.append(f'component "{n}" as {ali(n)} '
             f'{COLOR_CAPA[SUB[POR[n]["paquete"]]]}')
    # la tabla se declara justo detras del componente que la toca: asi
    # graphviz los pone enfrentados y las flechas salen cortas
    for t in sorted(sql[n]['tablas']):
        if t not in puesta:
            puesta.add(t)
            L.append(f'database "{t}" as T_{t}')
for n in sorted(sql):
    for t in sorted(sql[n]['tablas']):
        L.append(f'{ali(n)} {"-->" if t in sql[n]["escribe"] else "..>"} T_{t}')
L.append('@enduml')
open(f'{DIR}/13_Simple_Datos.puml', 'w', encoding='utf-8').write(
    '\n'.join(L) + '\n')

# --- 14: resumen de 06, los archivos agrupados por carpeta -----------------
# aqui si se dibujan las cajas de carpeta: son tres y sin ellas los paquetes
# que no tienen flecha quedan sueltos por el dibujo sin decir donde viven
L = [CAB_SIMPLE.replace('left to right direction', 'top to bottom direction')]
L.append('package "src/main/java" #F8FAFC {')
for p_ in sorted(porpaq):
    dir_ = '(raiz)' if p_ == '(raiz)' else p_ + '/'
    d_real = (REPO + '/src/main/java') if p_ == '(raiz)' \
        else f'{REPO}/src/main/java/{p_}'
    njava = len(glob.glob(f'{d_real}/*.java')) if os.path.isdir(d_real) \
        else len(porpaq[p_])
    L.append(f'  artifact "{dir_}\\n<size:10>{njava} .java</size>" as '
             f'J_{ali(p_)} {COLOR_CAPA[SUB[p_]]}')
js = [f'J_{ali(p_)}' for p_ in sorted(porpaq)]
for i_ in range(1, len(js)):
    L.append(f'  {js[i_-1]} -[hidden]right- {js[i_]}')
L.append('}')
n_fxml, n_css = len(PAREJA), len(set(CSS.values()) | set(CSS_MAIN))
L.append('package "src/main/resources" #FEF9C3 {')
L.append(f'  artifact "views/\\n<size:10>{n_fxml} .fxml</size>" as A_fxml')
L.append(f'  artifact "css/\\n<size:10>{n_css} .css</size>" as A_css')
L.append('  A_fxml -[hidden]right- A_css')
L.append('}')
n_db = len(glob.glob(REPO + '/db/ddl/*.ddl')) \
    + len(glob.glob(REPO + '/db/migrations/*.sql')) \
    + len(glob.glob(REPO + '/db/functions/*')) + 1
L.append('package "db" #DCFCE7 {')
L.append(f'  artifact "scripts\\n<size:10>{n_db} archivos</size>" as A_db')
L.append('}')
L.append(f'database "**gastroflow**\\n<size:10>{len(E)} tablas</size>" as bd')
L.append(f'A_fxml ..> J_controller : {n_fxml}')
L.append(f'J_{ali("(raiz)")} ..> A_css : {len(CSS) + len(CSS_MAIN)}')
L.append('A_db --> bd')
L.append('J_ConexionBD --> bd')
L.append('@enduml')
open(f'{DIR}/14_Simple_Artefactos.puml', 'w', encoding='utf-8').write(
    '\n'.join(L) + '\n')
print(f'resumenes: 13_Simple_Datos ({len(sql)} componentes, {len(puesta)} '
      f'tablas)   14_Simple_Artefactos ({len(porpaq) + 4} cajas)')
