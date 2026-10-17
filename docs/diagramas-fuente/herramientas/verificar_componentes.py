"""Comprueba los diagramas de componentes contra el codigo."""
import json, os, re, sys, glob, collections

REPO = sys.argv[1] if len(sys.argv) > 1 else os.environ.get(
    'GF_SRC', '/tmp/claude-0/-home-claude/'
    'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = sys.argv[2] if len(sys.argv) > 2 else os.environ.get(
    'GF_OUT', '/home/claude/diagramas')
SRC = REPO + '/src/main/java'
T = json.load(open(BASE + '/clases.json', encoding='utf-8'))
LL = json.load(open(BASE + '/llamadas.json', encoding='utf-8'))
E = json.load(open(BASE + '/esquema.json', encoding='utf-8'))
POR = {t['nombre']: t for t in T}
pom_txt = open(REPO + '/pom.xml', encoding='utf-8').read()
D = {os.path.basename(f): open(f, encoding='utf-8').read()
     for f in sorted(glob.glob(BASE + '/puml_componentes/*.puml'))}
TODO = ''.join(D.values())
fallos = []


def ok(c, m):
    print(('  OK   ' if c else '  FALLA ') + m)
    if not c:
        fallos.append(m)


PAQ_COMP = ('controller', 'service', 'dao', 'repository', 'ConexionBD',
            'session', 'util', '(raiz)')
COMPONENTES = [t['nombre'] for t in T
               if t['paquete'] in PAQ_COMP and not t['anidada_en']]
provee = collections.defaultdict(set)
requiere = collections.defaultdict(lambda: collections.defaultdict(set))
for l in LL['llamadas']:
    o, d = l['origen'], l['destino']
    if o in COMPONENTES and d in COMPONENTES and o != d:
        provee[d].add(l['metodo'])
        requiere[o][d].add(l['metodo'])

print('=== 1. los componentes son todas las clases que componen el sistema ===')
dib = set(re.findall(r'component "\*\*(\w+)\*\*', TODO))
ok(set(COMPONENTES) <= dib,
   f'los {len(COMPONENTES)} componentes aparecen '
   f'(faltan {sorted(set(COMPONENTES) - dib) or "ninguno"})')
# las cajas agregadas de capa (en 01 y en los simples 09 y 12) no son
# clases: llevan "N componente(s)" debajo del nombre
SUBS = set(re.findall(r'component "\*\*([^*]+)\*\*\\n<size:10>\d+ '
                      r'componentes?</size>', TODO))
# el diagrama 08 dibuja como componente las clases que importan Spring o
# Jakarta, aunque sean de entity: son tipos reales del codigo
SPRING = {t['nombre'] for t in T}
extra = {x for x in dib - set(COMPONENTES)
         if not any(x in s2 for s2 in SUBS) and x not in SPRING}
ok(not extra, f'no hay componentes inventados ({sorted(extra) or "ninguno"})')

print('\n=== 2. cada operacion de una interfaz existe en su clase ===')
mal = []
for m in re.finditer(r'interface "(\w+)\\n(.*?)" as I_', TODO):
    clase, cuerpo = m.group(1), m.group(2)
    ops = re.findall(r'<size:9>(\w+)\(([^)]*)\)</size>', cuerpo)
    reales = collections.defaultdict(set)
    for x in POR[clase]['metodos']:
        reales[x['nombre']].add(', '.join(p['tipo'].split('<')[0].strip()
                                          for p in x['params']))
    for o, ps in ops:
        if o not in reales:
            mal.append(f'{clase}.{o}() no existe en el codigo')
        elif ps not in reales[o]:
            mal.append(f'{clase}.{o}({ps}) no coincide con ninguna firma real')
    if {o for o, _p in ops} != provee[clase]:
        mal.append(f'{clase}: operaciones {sorted({o for o, _p in ops})} != '
                   f'llamadas reales {sorted(provee[clase])}')
    # todas las sobrecargas del nombre llamado tienen que estar listadas
    for o in provee[clase]:
        if len([1 for x, _p in ops if x == o]) != len(reales[o]):
            mal.append(f'{clase}.{o}: faltan sobrecargas en la interfaz')
ok(not mal, f'las {sum(len(v) for v in provee.values())} operaciones provistas '
            f'existen y son exactamente las que alguien llama ({len(mal)} fallos)')
for x in mal[:8]:
    print('      ', x)

print('\n=== 3. cada flecha <<use>> lleva el numero real de operaciones ===')
mal = []
for arch, s in D.items():
    for m in re.finditer(r'^(\w+) \.\.> I_(\w+) : <<use>> (\d+)', s, re.M):
        o, d, n = m.group(1), m.group(2), int(m.group(3))
        if o not in requiere or d not in requiere[o]:
            mal.append(f'{arch}: {o} -> {d} no existe en el codigo')
        elif len(requiere[o][d]) != n:
            mal.append(f'{arch}: {o} -> {d} dice {n}, '
                       f'son {len(requiere[o][d])}')
ok(not mal, f'las flechas entre componentes cuadran con las llamadas ({len(mal)})')
for x in mal[:8]:
    print('      ', x)

print('\n=== 4. ninguna dependencia entre componentes se quedo sin dibujar ===')
falta = []
for o, dests in requiere.items():
    for d in dests:
        if not re.search(rf'^{re.escape(o)} \.\.> I_{re.escape(d)} ', TODO, re.M):
            falta.append(f'{o} -> {d}')
ok(not falta, f'las {sum(len(v) for v in requiere.values())} dependencias '
              f'componente a componente estan dibujadas ({len(falta)} faltan)')
for x in falta[:8]:
    print('      ', x)

print('\n=== 5. cada tabla dibujada sale del SQL de ese repositorio ===')
mal = []
for arch in ('04_Componentes_datos.puml', '05_Componentes_datos_compartidos.puml'):
    s = D[arch]
    for m in re.finditer(r'^(\w+) (-->|\.\.>) T_(\w+) : '
                         r'(lee y escribe|lee|escribe)( \(en ejecucion\))?$',
                         s, re.M):
        comp, _f, tabla, modo = m.group(1), m.group(2), m.group(3), m.group(4)
        d = LL['sql'].get(comp)
        if not d or tabla not in d['tablas']:
            mal.append(f'{arch}: {comp} no consulta {tabla}')
        elif ('escribe' in modo) != (tabla in d['escribe']):
            mal.append(f'{arch}: {comp} -> {tabla} marcado "{modo}"')
        else:
            txt = open(f"{SRC}/{POR[comp]['archivo']}", encoding='utf-8').read()
            if not re.search(rf'\b{tabla}\b', txt, re.I):
                mal.append(f'{arch}: {tabla} no aparece en {POR[comp]["archivo"]}')
            marcada = bool(m.group(5))
            if marcada != (tabla in d.get('dinamicas', [])):
                mal.append(f'{arch}: {comp} -> {tabla} marca de ejecucion mal')
ok(not mal, f'las tablas dibujadas estan en el SQL de su repositorio ({len(mal)})')
for x in mal[:8]:
    print('      ', x)

PAREJA_V = {}
for _r in sorted(glob.glob(REPO + '/src/main/resources/views/*.fxml')):
    _x = open(_r, encoding='utf-8').read()
    _m = re.search(r'fx:controller="[\w.]*\.(\w+)"', _x)
    if _m:
        PAREJA_V[_m.group(1)] = os.path.basename(_r)

print('\n=== 6. las vistas y su controller ===')
mal = []
for ruta in sorted(glob.glob(REPO + '/src/main/resources/views/*.fxml')):
    x = open(ruta, encoding='utf-8').read()
    fx = os.path.basename(ruta)
    m = re.search(r'fx:controller="[\w.]*\.(\w+)"', x)
    if not m:
        continue
    if not re.search(rf'<<fx:controller>> {m.group(1)}\b', TODO):
        mal.append(f'{fx} -> {m.group(1)} no dibujado')
    nid = len(set(re.findall(r'fx:id="(\w+)"', x)))
    if f'{fx} · {nid} fx:id' not in TODO:
        mal.append(f'{fx}: el conteo de fx:id no cuadra ({nid})')
ok(not mal, f'los 7 .fxml estan enlazados con su controller y sus fx:id ({len(mal)})')
for x in mal[:8]:
    print('      ', x)

print('\n=== 7. nada escrito a mano sobre el sistema ===')
fuente = ' '.join(open(p, encoding='utf-8').read() for p in
                  glob.glob(REPO + '/src/**/*.java', recursive=True))
fuente += ' '.join(open(p, encoding='utf-8').read() for p in
                   glob.glob(REPO + '/src/main/resources/**/*', recursive=True)
                   if os.path.isfile(p))
fuente += ' '.join(os.path.relpath(p, REPO) for p in
                   glob.glob(REPO + '/**/*', recursive=True))
fuente += open(REPO + '/pom.xml', encoding='utf-8').read() + ' '.join(E)
fuente = fuente.lower()
PERM = set('''startuml enduml allowmixing skinparam backgroundcolor shadowing
 defaultfontname roundcorner nodesep ranksep arrowcolor arrowfontsize
 packagebordercolor packagefontstyle componentbackgroundcolor componentfontsize
 interfacebackgroundcolor interfacefontsize artifactbackgroundcolor
 artifactfontsize databasebackgroundcolor size component interface artifact
 database package hidden right down left direction use manifest carga ejecuta
 crea excluida pom xml comparte llama sin propio lee escribe tablas tabla
 unico repositorio repositorios ningun consulta tocan dos mas operaciones
 componentes interfaz usuario logica negocio acceso datos infraestructura
 infra escriben lectura solo ejecucion elige consumen
 configuracion recursos otros aplicacion importan vacios archivo exclude apoyo transversal
 ninguna compilar salto linea bytes byte solo
 arrowthickness componentbordercolor packagefontsize legendfontsize
 legendbackgroundcolor legendbordercolor back conexion llamadas
 capa cada caja flecha dependencia real entre numero distintas
 invocan traves ella detalle esta diagrama aviso marca clases
 excluye piezas usa todo sistema para lea consumidores agrupados
 dice cuantos son pantalla aparecen llaman otra compartidas
 pertenecen ninguna casi todo pocos triangulo rojo ejecutarse
 incluye fuera llega codigo ese sus
 fuera subsistema archivos raiz modulos nombre true false'''.split())
# la leyenda es explicacion en castellano y los alias "as X" son nombres
# internos que yo elijo: lo que tiene que salir del repositorio es el texto
# visible de la caja
TEXTO = re.sub(r'^legend\b.*?^endlegend$', '', TODO, flags=re.S | re.M)
ALIAS_MIOS = set(re.findall(r'\bas\s+([A-Za-z_]\w*)', TEXTO))
inventadas = set()
for pal in re.findall(r'\b[A-Za-z_][A-Za-z0-9_]{3,}\b', TEXTO):
    if pal in ALIAS_MIOS:
        continue
    if pal.lower() in fuente or pal.lower() in PERM:
        continue
    if pal in dib or pal.startswith(('A_', 'J_', 'T_', 'S_', 'I_', 'F_')):
        continue
    # alias construidos a partir de una ruta real: el ultimo _ es el punto
    # de la extension y los demas son separadores de carpeta
    if '_' in pal:
        tronco, _s, ext = pal.rpartition('_')
        ruta = tronco.replace('_', '/') + '.' + ext
        if ruta.lower() in fuente or pal.replace('_', '/').lower() in fuente \
           or pal.replace('_', '.').lower() in fuente:
            continue
    if re.fullmatch(r'[0-9A-F]{6}', pal):      # colores
        continue
    inventadas.add(pal)
ok(not inventadas, f'palabras que no estan en el repositorio: '
                   f'{sorted(inventadas)[:15] or "ninguna"}')


print('\n=== 8. el diagrama de la parte Spring que no compila ===')
g = D.get('08_Componentes_spring_sin_compilar.puml', '')
cajas = dict(re.findall(r'component "\*\*(\w+)\*\*((?:\\n<size:9>.*?</size>)*)"', g))
spring = {}
for ruta in sorted(glob.glob(REPO + '/src/**/*.java', recursive=True)):
    if os.path.getsize(ruta) == 0:
        continue
    txt = open(ruta, encoding='utf-8').read()
    imp = re.findall(r'^import\s+((?:org\.springframework|jakarta)[\w.*]+)\s*;',
                     txt, re.M)
    if imp:
        spring[os.path.basename(ruta).replace('.java', '')] = (sorted(set(imp)), txt)
ok(set(spring) <= set(cajas),
   f'las {len(spring)} clases que importan Spring o Jakarta estan dibujadas '
   f'(faltan {sorted(set(spring) - set(cajas)) or "ninguna"})')
mal = []
for n, (imps, txt) in spring.items():
    cuerpo = cajas.get(n, '')
    for i in imps:
        corto = i.replace('org.springframework.', '').replace('jakarta.', '')
        if corto not in cuerpo:
            mal.append(f'{n}: falta el import {i}')
    for a in ('@Entity', '@Table', '@Service', '@Transactional'):
        if bool(re.search(rf'^\s*{a}\b', txt, re.M)) != (a in cuerpo):
            mal.append(f'{n}: la anotacion {a} no cuadra')
    ext = POR[n].get('extends')
    if ext and f'extends {ext}' not in cuerpo:
        mal.append(f'{n}: falta extends {ext}')
ok(not mal, f'imports, anotaciones y extends de esas clases ({len(mal)} fallos)')
for x in mal[:6]:
    print('      ', x)

ceros = sorted(os.path.relpath(p, REPO) for p in
               glob.glob(REPO + '/src/**/*.java', recursive=True)
               if os.path.getsize(p) == 0)
unos = sorted(os.path.relpath(p, REPO) for p in
              glob.glob(REPO + '/conf/*') + glob.glob(REPO + '/scripts/*')
              if os.path.getsize(p) == 1 and not p.endswith('.gitkeep'))
dib0 = re.search(r'package "archivos \.java de 0 bytes · (\d+)"', g)
dib1 = re.search(r'package "archivos de 1 byte[^"]*· (\d+)"', g)
ok(dib0 and int(dib0.group(1)) == len(ceros) and all(c in g for c in ceros),
   f'los {len(ceros)} archivos .java de 0 bytes estan listados')
ok(dib1 and int(dib1.group(1)) == len(unos) and all(u in g for u in unos),
   f'los {len(unos)} archivos de 1 byte estan listados')

props = [l.strip() for l in
         open(REPO + '/src/main/resources/application.properties', encoding='utf-8')
         if '=' in l and not l.startswith('#')]
falta = [p for p in props if p.replace(' ', '') not in g.replace(' ', '')]
ok(not falta, f'las {len(props)} propiedades de application.properties, tal cual')
nh = sum(1 for _ in open(REPO + '/src/main/resources/static/login.html',
                         encoding='utf-8'))
ok(f'{nh} lineas' in g, f'static/login.html dice sus {nh} lineas')
EXCL2 = [l.strip() for l in re.findall(r'<exclude>(.+?)</exclude>', pom_txt)]
ok(len(re.findall(r'A_pom \.\.> \w+ : <<exclude>>', g)) == len(EXCL2),
   f'una flecha exclude por cada uno de los {len(EXCL2)} archivos del pom.xml')
ok(not [d for d in re.findall(r'<artifactId>(.*?)</artifactId>', pom_txt)
        if 'spring' in d.lower()],
   'el pom.xml no declara ninguna dependencia de Spring')


print('\n=== 9. quien carga cada hoja de estilos ===')
main_src = open(REPO + '/src/main/java/Main.java', encoding='utf-8').read()
art = D.get('06_Componentes_artefactos.puml', '')
# en el codigo, todas las hojas se anaden desde Main
hojas_main = set(re.findall(r'getStylesheets\(\)\.add\([^)]*"/css/([^"]+)"', main_src))
hojas_mod = {os.path.basename(c) for _f, c in
             re.findall(r'"(/views/[^"]+)"\s*,\s*(?:"(/css/[^"]+)"|null)', main_src)
             if c}
todas = hojas_main | hojas_mod
otros = [os.path.basename(f) for f in glob.glob(REPO + '/src/main/java/**/*.java',
                                                recursive=True)
         if 'Main.java' not in f and 'getStylesheets' in
         open(f, encoding='utf-8').read()]
ok(not otros, f'solo Main anade hojas de estilo ({otros or "ninguna otra clase"})')
cargas = re.findall(r'^(\w+) \.\.> A_(\w+) : <<carga>>', art, re.M)
ok(all(o == 'J__raiz_' for o, _d in cargas),
   f'las {len(cargas)} flechas <<carga>> salen de Main')
dibujadas = {d.replace('_css', '.css').replace('_', '-').replace('.css', '.css')
             for _o, d in cargas}
faltan = {h for h in todas
          if f'A_{re.sub(chr(92) + "W", "_", h)}' not in art}
ok(not faltan, f'las {len(todas)} hojas de estilo del codigo estan dibujadas '
               f'({sorted(faltan) or "ninguna falta"})')
ui = D.get('02_Componentes_ui.puml', '')
mal = []
for fx, css in re.findall(r'"(/views/[^"]+)"\s*,\s*(?:"(/css/[^"]+)"|null)', main_src):
    c = next((k for k, v in PAREJA_V.items() if v == os.path.basename(fx)), None)
    if c and css:
        caja = re.search(rf'component "\*\*{c}\*\*([^"]*)"', ui)
        if caja and os.path.basename(css) not in caja.group(1):
            mal.append(f'{c} no muestra {os.path.basename(css)}')
ok(not mal, f'cada pantalla muestra la hoja que Main le carga ({len(mal)})')

print('\n=== resultado ===')
print(f'{len(fallos)} comprobaciones fallidas' if fallos else 'todo correcto')
