"""Comprueba las dos vistas resumidas nuevas (13 y 14) y como quedan en el .md.

La regla que ya nos costo una vez: un verificador que repita la regla del
generador no comprueba nada, defiende el error. Asi que aqui nada se compara
contra los .json que usa el generador. Cada numero se vuelve a sacar del
repositorio con glob y leyendo los archivos, y cada flecha se contrasta
ademas contra el diagrama detallado del que la vista es resumen, que es un
artefacto independiente: si el resumen y su version larga no dicen lo mismo,
uno de los dos esta mal y hay que mirar.

    python3 verificar_resumenes.py <clon> <carpeta de trabajo>
"""
import collections
import glob
import json
import os
import re
import sys

REPO = sys.argv[1] if len(sys.argv) > 1 else os.environ.get('GF_SRC', '')
BASE = sys.argv[2] if len(sys.argv) > 2 else os.environ.get('GF_OUT', '.')
PC = BASE + '/puml_componentes'

fallos, pasan = [], 0


def ok(cond, titulo, detalle=''):
    global pasan
    if cond:
        pasan += 1
        print(f'  OK   {titulo}')
    else:
        fallos.append(titulo)
        print(f'  FALLA {titulo}')
        if detalle:
            for linea in str(detalle).split('\n'):
                print(f'         {linea}')


def leer(nombre):
    ruta = f'{PC}/{nombre}'
    if not os.path.exists(ruta):
        fallos.append(f'no existe {nombre}')
        print(f'  FALLA no existe {nombre}')
        return ''
    s = open(ruta, encoding='utf-8').read()
    if '@startuml' not in s or '@enduml' not in s:
        fallos.append(f'{nombre} no es un diagrama')
        print(f'  FALLA {nombre} no es un diagrama')
    return s


# las flechas a tablas: alias del origen, estilo, nombre de la tabla
FLECHA_T = re.compile(r'^(\w+)\s+(-->|\.\.>)\s+T_(\w+)', re.M)
CAJA_C = re.compile(r'^\s*component\s+"(?:\*\*)?([\w]+)(?:\*\*)?[^"]*"\s+as\s+(\w+)', re.M)
CAJA_T = re.compile(r'^\s*database\s+"(\w+)"\s+as\s+T_(\w+)', re.M)


def pares(texto):
    """{(componente, tabla): escribe}"""
    return {(o, t): (estilo == '-->') for o, estilo, t in FLECHA_T.findall(texto)}


print('=== 13_Simple_Datos ===')
d13 = leer('13_Simple_Datos.puml')
d04 = leer('04_Componentes_datos.puml')
d05 = leer('05_Componentes_datos_compartidos.puml')

p13 = pares(d13)
plargo = pares(d04) | pares(d05)

faltan = {k: v for k, v in plargo.items() if k not in p13}
sobran = {k: v for k, v in p13.items() if k not in plargo}
giradas = {k for k in p13.keys() & plargo.keys() if p13[k] != plargo[k]}
ok(not faltan, f'el resumen dibuja las {len(plargo)} parejas componente-tabla '
   f'de los diagramas 04 y 05',
   '\n'.join(f'falta {o} -> {t}' for o, t in sorted(faltan)))
ok(not sobran, 'el resumen no inventa ninguna pareja',
   '\n'.join(f'sobra {o} -> {t}' for o, t in sorted(sobran)))
ok(not giradas, 'escribe o solo lee coincide con la version larga',
   '\n'.join(f'{o} -> {t}: resumen {"escribe" if p13[(o, t)] else "lee"}, '
             f'largo {"escribe" if plargo[(o, t)] else "lee"}'
             for o, t in sorted(giradas)))

# los componentes: que existan como .java y que ese .java tenga SQL.
# Se busca en el repositorio, no en llamadas.json.
SQL = re.compile(r'\b(SELECT|INSERT\s+INTO|UPDATE|DELETE\s+FROM)\b', re.I)
cajas13 = {n for n, _a in CAJA_C.findall(d13)}
sin_java, sin_sql = [], []
for n in sorted(cajas13):
    f = glob.glob(f'{REPO}/src/main/java/**/{n}.java', recursive=True)
    if not f:
        sin_java.append(n)
    elif not SQL.search(open(f[0], encoding='utf-8', errors='ignore').read()):
        sin_sql.append(n)
ok(not sin_java, f'los {len(cajas13)} componentes existen como .java',
   ', '.join(sin_java))
ok(not sin_sql, 'todos ejecutan SQL', ', '.join(sin_sql))

# y al reves: ningun .java que hable con la base se queda fuera. Se busca
# por la llamada JDBC y no por el texto del SQL, porque el SQL puede venir
# partido en trozos concatenados y entonces no se reconoce leyendo cadenas
JDBC = re.compile(r'\b(prepareStatement|createStatement|prepareCall)\s*\(')
con_sql = {os.path.basename(f)[:-5]
           for f in glob.glob(f'{REPO}/src/main/java/**/*.java', recursive=True)
           if JDBC.search(open(f, encoding='utf-8', errors='ignore').read())}
ok(not (con_sql - cajas13),
   f'ningun .java con SQL propio se queda fuera ({len(con_sql)} encontrados)',
   ', '.join(sorted(con_sql - cajas13)))

# las tablas: contra el esquema sacado de db/, no contra esquema.json
ddl = ' '.join(open(f, encoding='utf-8', errors='ignore').read()
               for f in glob.glob(f'{REPO}/db/**/*', recursive=True)
               if os.path.isfile(f))
reales = {m.lower() for m in re.findall(
    r'CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?(?:public\.)?(\w+)', ddl, re.I)}
tablas13 = {t for t, _a in CAJA_T.findall(d13)}
ok(not (tablas13 - reales), f'las {len(tablas13)} tablas dibujadas existen en db/',
   ', '.join(sorted(tablas13 - reales)))

print('\n=== 14_Simple_Artefactos ===')
d14 = leer('14_Simple_Artefactos.puml')
d06 = leer('06_Componentes_artefactos.puml')

ART = re.compile(r'^\s*artifact\s+"([^"\\]+)(?:\\n<size:\d+>([^<]*)</size>)?"\s+as\s+(\w+)', re.M)
cuentas14 = {m[0].strip(): m[1].strip() for m in ART.findall(d14)}
cuentas06 = {m[0].strip(): m[1].strip() for m in ART.findall(d06)}

# a) los .java de cada carpeta, contados en el disco
mal = []
for etiqueta, texto in cuentas14.items():
    m = re.match(r'(\d+) \.java$', texto or '')
    if not m:
        continue
    carpeta = (f'{REPO}/src/main/java' if etiqueta == '(raiz)'
               else f'{REPO}/src/main/java/{etiqueta.rstrip("/")}')
    real = len(glob.glob(f'{carpeta}/*.java'))
    if real != int(m.group(1)):
        mal.append(f'{etiqueta}: dice {m.group(1)}, hay {real}')
ok(not mal, 'los .java de cada carpeta cuadran con el repositorio', '\n'.join(mal))

n_fxml = len(glob.glob(f'{REPO}/src/main/resources/views/*.fxml'))
n_css = len(glob.glob(f'{REPO}/src/main/resources/css/*.css'))
n_db = len([f for f in glob.glob(f'{REPO}/db/ddl/*.ddl')
            + glob.glob(f'{REPO}/db/migrations/*.sql')
            + glob.glob(f'{REPO}/db/functions/*') if os.path.isfile(f)]) \
    + len(glob.glob(f'{REPO}/scripts/setup.sh'))
ok(cuentas14.get('views/') == f'{n_fxml} .fxml',
   f'views/ dice los {n_fxml} .fxml que hay', cuentas14.get('views/'))
ok(cuentas14.get('css/') == f'{n_css} .css',
   f'css/ dice los {n_css} .css que hay', cuentas14.get('css/'))
ok(cuentas14.get('scripts') == f'{n_db} archivos',
   f'db dice los {n_db} archivos que hay', cuentas14.get('scripts'))

m = re.search(r'database "\*\*gastroflow\*\*\\n<size:\d+>(\d+) tablas', d14)
ok(m and int(m.group(1)) == len(reales),
   f'la base dice sus {len(reales)} tablas',
   m.group(1) if m else 'no aparece')

# b) las dos flechas con numero
fx_con_controller = sum(
    1 for f in glob.glob(f'{REPO}/src/main/resources/views/*.fxml')
    if 'fx:controller' in open(f, encoding='utf-8', errors='ignore').read())
m = re.search(r'A_fxml \.\.> J_controller : (\d+)', d14)
ok(m and int(m.group(1)) == fx_con_controller,
   f'la flecha de views/ a controller/ dice los {fx_con_controller} .fxml '
   f'que declaran fx:controller', m.group(1) if m else 'no aparece')

main = glob.glob(f'{REPO}/src/main/java/Main.java')
hojas = set()
if main:
    hojas = {os.path.basename(h) for h in re.findall(
        r'"(/css/[\w\-.]+\.css)"', open(main[0], encoding='utf-8').read())}
m = re.search(r'J__raiz_ \.\.> A_css : (\d+)', d14)
ok(m and int(m.group(1)) == len(hojas),
   f'la flecha de (raiz) a css/ dice las {len(hojas)} hojas distintas que '
   f'carga Main', f'dice {m.group(1) if m else "nada"}, son {sorted(hojas)}')
ok(len(hojas) == n_css,
   'las hojas que carga Main son todas las que hay en css/',
   f'{sorted(hojas)} vs {sorted(os.path.basename(c) for c in glob.glob(f"{REPO}/src/main/resources/css/*.css"))}')

# c) el resumen no puede contradecir a su version larga
dif = [f'{k}: resumen "{v}", largo "{cuentas06.get(k)}"'
       for k, v in cuentas14.items()
       if k in cuentas06 and v != cuentas06[k]]
ok(not dif, 'las cuentas coinciden con las del diagrama 06', '\n'.join(dif))

print('\n=== 00_Mapa_Clases ===')
mapa = ''
rm = f'{BASE}/puml_clases/00_Mapa_Clases.puml'
if not os.path.exists(rm):
    ok(False, 'existe el mapa ligero de clases')
else:
    mapa = open(rm, encoding='utf-8').read()
    ok('@startuml' in mapa and '@enduml' in mapa, 'el mapa es un diagrama')

# que banda le toca a cada paquete, escrito aqui aparte del generador: si
# los dos lo dicen igual es que no se ha movido nada por accidente
BANDA = {'controller': 'presentacion', '(raiz)': 'presentacion',
         'service': 'negocio', 'repository': 'datos', 'dao': 'datos',
         'ConexionBD': 'conexion', 'entity': 'modelo', 'enums': 'modelo',
         'dto': 'modelo', 'exceptions': 'modelo', 'session': 'apoyo',
         'util': 'apoyo'}
TIPOS = json.load(open(f'{BASE}/clases.json', encoding='utf-8'))
# por (paquete, nombre) y no por nombre: hay dos EstadoMesa, uno en entity
# y otro en enums, y un conjunto de nombres los colapsaria en uno
esperado = {}
for t in TIPOS:
    b = BANDA.get(t['paquete'])
    if b is None:
        fallos.append(f'paquete sin banda: {t["paquete"]}')
        continue
    esperado.setdefault(b, set()).add((t['paquete'], t['nombre']))
PAQ_DE = {}
for t in TIPOS:
    PAQ_DE.setdefault(t['nombre'], set()).add(t['paquete'])


def resolver(txt):
    """'entity.EstadoMesa' o 'Mesa' -> (paquete, nombre)"""
    if '.' in txt:
        p_, n_ = txt.rsplit('.', 1)
        return (p_, n_)
    ps = PAQ_DE.get(txt)
    return (next(iter(ps)), txt) if ps and len(ps) == 1 else ('?', txt)

CAJA = re.compile(r'^rectangle "(.*?)" as (\w+) #', re.M | re.S)
dibujado, cabecera = {}, {}
for etiqueta, alias in CAJA.findall(mapa):
    lineas = etiqueta.split('\\n')
    m = re.search(r'<size:11>· (\d+)</size>', lineas[0])
    cabecera[alias] = int(m.group(1)) if m else -1
    nombres_caja = set()
    for l in lineas[2:]:
        cuerpo = re.fullmatch(r'<size:11>(.*)</size>', l)
        if not cuerpo:
            continue
        for trozo in cuerpo.group(1).split(' · '):
            m2 = re.fullmatch(r'([\w.]+)(?: \((.*)\))?', trozo.strip())
            if not m2:
                fallos.append(f'entrada rara en el mapa: {trozo!r}')
                continue
            nombres_caja.add(resolver(m2.group(1)))
            if m2.group(2):
                for h in m2.group(2).replace('en ', '').split(','):
                    nombres_caja.add(resolver(h.strip()))
    dibujado[alias] = nombres_caja

mal = []
for banda, nombres_ok in sorted(esperado.items()):
    hay = dibujado.get(banda, set())
    if hay - nombres_ok:
        mal.append(f'{banda}: sobran {sorted(hay - nombres_ok)}')
    if nombres_ok - hay:
        mal.append(f'{banda}: faltan {sorted(nombres_ok - hay)}')
ok(not mal, f'las {len(TIPOS)} clases estan en el mapa y cada una en su banda',
   '\n'.join(mal))
ok(set(dibujado) == set(esperado),
   f'el mapa tiene las {len(esperado)} bandas y ninguna de mas',
   f'{sorted(dibujado)} vs {sorted(esperado)}')
mal = [f'{b}: la cabecera dice {cabecera.get(b)} y son {len(n)}'
       for b, n in sorted(esperado.items())
       if cabecera.get(b) != len(n)]
ok(not mal, 'el numero de cada banda es el de sus clases', '\n'.join(mal))
# ninguna clase puede salir en dos bandas
repes = [n for n in {x for v in dibujado.values() for x in v}
         if sum(n in v for v in dibujado.values()) > 1]
ok(not repes, 'ninguna clase aparece en dos bandas', ', '.join(sorted(repes)))

print('\n=== 00_Resumen_Clases ===')
rr = f'{BASE}/puml_clases/00_Resumen_Clases.puml'
if not os.path.exists(rr):
    ok(False, 'existe la vista corta de clases')
else:
    corto = open(rr, encoding='utf-8').read()
    ok('@startuml' in corto and '@enduml' in corto, 'la vista corta es un diagrama')
    porpaq = collections.Counter(t['paquete'] for t in TIPOS)
    mal, bandas_vistas = [], set()
    for etiqueta, alias in CAJA.findall(corto):
        bandas_vistas.add(alias)
        m = re.search(r'<size:12>· (\d+) tipos?</size>', etiqueta)
        total_dicho = int(m.group(1)) if m else -1
        if total_dicho != len(esperado.get(alias, ())):
            mal.append(f'{alias}: la cabecera dice {total_dicho} y son '
                       f'{len(esperado.get(alias, ()))}')
        pares = re.findall(r'([\w()]+) · (\d+)', etiqueta.split('\\n')[-1])
        dichos = {p_: int(n_) for p_, n_ in pares}
        reales = {p_: porpaq[p_] for p_ in porpaq
                  if BANDA.get(p_) == alias and porpaq[p_]}
        if dichos != reales:
            mal.append(f'{alias}: dice {dichos} y son {reales}')
    ok(not mal, 'cada banda de la vista corta dice sus paquetes y su tamano',
       '\n'.join(mal))
    ok(bandas_vistas == set(esperado),
       f'la vista corta tiene las {len(esperado)} bandas',
       f'{sorted(bandas_vistas)} vs {sorted(esperado)}')

print('\n=== las flechas de las dos vistas ligeras ===')
# Se recalculan aqui desde las relaciones y las llamadas medidas sobre el
# codigo, y se exige que las dos vistas dibujen exactamente esas y ninguna
# mas. Sin esto, las dos pintaban una cascada limpia y se callaban los dos
# saltos de capa que el resto de los diagramas si marcan.
Rj = json.load(open(f'{BASE}/relaciones.json', encoding='utf-8'))
LLj = json.load(open(f'{BASE}/llamadas.json', encoding='utf-8'))
PAQS = {}
for t in TIPOS:
    PAQS.setdefault(t['nombre'], set()).add(t['paquete'])


def _banda(n):
    ps = PAQS.get(n)
    return BANDA.get(next(iter(ps))) if ps and len(ps) == 1 else None


reales = set()
for _t in ('asociacion', 'dependencia', 'generalizacion', 'realizacion'):
    for r in Rj.get(_t, []):
        a, b = _banda(r['origen']), _banda(r['destino'])
        if a and b and a != b:
            reales.add((a, b))
for l in LLj['llamadas']:
    a, b = _banda(l['origen']), _banda(l['destino'])
    if a and b and a != b:
        reales.add((a, b))
COLUMNA = ['presentacion', 'negocio', 'datos', 'conexion']
saltos = {(a, b) for a, b in reales
          if a in COLUMNA and b in COLUMNA
          and COLUMNA.index(b) - COLUMNA.index(a) > 1}
FL = re.compile(r'^(\w+) -(?:\[#(\w+)\])?(?:up|down|left|right)?'
                r'(?:\[#(\w+)\])?-+> (\w+)', re.M)
for arch, texto in (('00_Resumen_Clases.puml', corto if 'corto' in dir() else ''),
                    ('00_Mapa_Clases.puml', mapa)):
    if not texto:
        ok(False, f'{arch}: no se pudo leer')
        continue
    dib, rojas = set(), set()
    for o, c1, c2, d in FL.findall(texto):
        if o in BANDA.values() or o in esperado:
            dib.add((o, d))
            if (c1 or c2) == 'B91C1C':
                rojas.add((o, d))
    ok(dib == reales, f'{arch}: dibuja las {len(reales)} dependencias reales '
       f'entre bandas',
       f'sobran {sorted(dib - reales)} · faltan {sorted(reales - dib)}')
    ok(rojas == saltos, f'{arch}: marca en rojo los {len(saltos)} saltos de capa',
       f'rojas {sorted(rojas)} vs saltos {sorted(saltos)}')

print('\n=== como queda en el .md ===')
MD = BASE + '/docs_entrega/diagramaComponentes.md'
texto = open(MD, encoding='utf-8').read() if os.path.exists(MD) else ''
ok(bool(texto), 'existe diagramaComponentes.md')
imgs = re.findall(r'!\[[^\]]*\]\(Imagenes/([\w.]+)\)', texto)
faltan_img = [i for i in imgs
              if not os.path.exists(f'{BASE}/docs_entrega/Imagenes/{i}')]
ok(not faltan_img, f'las {len(imgs)} imagenes referenciadas existen',
   ', '.join(faltan_img))

# cada resumen justo debajo del largo que le toca, y sin titulo
PAREJAS = [('DiagramaComponentes_General', 'DiagramaComponentes_S1_Capas'),
           ('DiagramaComponentes_UI', 'DiagramaComponentes_S2_UI'),
           ('DiagramaComponentes_Negocio', 'DiagramaComponentes_S3_Negocio'),
           ('DiagramaComponentes_Infraestructura', 'DiagramaComponentes_S4_Apoyo'),
           ('DiagramaComponentes_Datos', 'DiagramaComponentes_S5_Datos'),
           ('DiagramaComponentes_Artefactos', 'DiagramaComponentes_S6_Artefactos')]
orden = imgs
mal = []
for largo, resumen in PAREJAS:
    if f'{largo}.png' not in orden or f'{resumen}.png' not in orden:
        mal.append(f'falta alguna de {largo} / {resumen}')
        continue
    i, j = orden.index(f'{largo}.png'), orden.index(f'{resumen}.png')
    if j != i + 1:
        mal.append(f'{resumen} no va justo despues de {largo}')
    # entre los dos no puede haber un titulo
    trozo = texto.split(f'{largo}.png)')[1].split(f'{resumen}.png)')[0]
    if re.search(r'^#+ ', trozo, re.M):
        mal.append(f'hay un titulo entre {largo} y {resumen}')
ok(not mal, 'los 6 resumenes van pegados a su version larga y sin titulo',
   '\n'.join(mal))

# ninguna imagen generada se queda sin usar
en_disco = {os.path.basename(p) for p in
            glob.glob(f'{BASE}/docs_entrega/Imagenes/*.png')}
todas_md = set()
for f in glob.glob(f'{BASE}/docs_entrega/*.md'):
    todas_md |= set(re.findall(r'!\[[^\]]*\]\(Imagenes/([\w.]+)\)',
                               open(f, encoding='utf-8').read()))
ok(not (en_disco - todas_md), 'ninguna imagen generada queda sin usar',
   ', '.join(sorted(en_disco - todas_md)))
ok(not (todas_md - en_disco), 'ningun .md apunta a una imagen que no existe',
   ', '.join(sorted(todas_md - en_disco)))

print(f'\n=== resultado ===\n{pasan} pasan, {len(fallos)} fallan')
if fallos:
    sys.exit(1)
print('todo correcto')
