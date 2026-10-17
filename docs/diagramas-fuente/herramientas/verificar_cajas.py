"""Compara, linea a linea, cada caja dibujada contra el texto del .java.

No cuenta miembros: reconstruye la caja que DEBERIA salir del archivo fuente
y la compara con la dibujada, en orden, con visibilidad, modificadores,
tipos de campo, tipos de retorno y tipos de cada parametro.
"""
import collections
import re, os, sys, glob, json, collections

REPO, DIAG = sys.argv[1], sys.argv[2]
SRC = REPO + '/src/main/java'
T = json.load(open(DIAG + '/clases.json', encoding='utf-8'))
fallos = []


def ok(cond, msg):
    print(('  OK   ' if cond else '  FALLA ') + msg)
    if not cond:
        fallos.append(msg)


# ---------------------------------------------------------------------------
# 1. cada tipo con miembros aparece una sola vez en 01-07
# ---------------------------------------------------------------------------
print('=== 1. una caja con miembros por tipo, sin repetir ===')
# clave (paquete, nombre): hay dos tipos llamados EstadoMesa, uno en entity
# y otro en enums. El paquete sale del titulo del package que la contiene.
PAQ = ['entity', 'controller', 'repository', 'service', 'dao', 'dto', 'enums',
       'exceptions', 'session', 'util', 'ConexionBD', '(raiz)']
con_cuerpo = collections.Counter()
ESTER, CUERPO = {}, {}
for f in sorted(glob.glob(DIAG + '/puml_clases/0[1-7]_*.puml')):
    txt = open(f, encoding='utf-8').read()
    # (posicion, titulo) de cada package del archivo
    packs = [(m.start(), m.group(1)) for m in
             re.finditer(r'^package "([^"]+)"', txt, re.M)]
    for b in re.finditer(r'^(\s*)(class|interface|enum|annotation)\s+(\w+)'
                         r'([^\n{]*)\{\n(.*?)^\1\}', txt, re.M | re.S):
        titulo = [t for p, t in packs if p < b.start()][-1]
        if titulo == 'referencias':
            continue
        nom, cola, cuerpo = b.group(3), b.group(4), b.group(5)
        cands = [t for t in T if t['nombre'] == nom]
        if len(cands) > 1:
            cands = [t for t in cands if t['paquete'] in titulo] or cands
        clave = (cands[0]['paquete'], nom) if cands else ('?', nom)
        con_cuerpo[clave] += 1
        ESTER[clave] = (b.group(2), cola)
        CUERPO[clave] = [l.strip() for l in cuerpo.split('\n') if l.strip()]
rep = {n: c for n, c in con_cuerpo.items() if c > 1}
ok(not rep, f'ningun tipo dibuja sus miembros dos veces ({rep or "ninguno"})')
claves_codigo = {(t['paquete'], t['nombre']) for t in T}
ok(set(con_cuerpo) == claves_codigo,
   f'hay caja propia para cada tipo '
   f'(faltan {sorted(claves_codigo - set(con_cuerpo)) or "ninguno"}, '
   f'sobran {sorted(set(con_cuerpo) - claves_codigo) or "ninguno"})')

# ---------------------------------------------------------------------------
# 2. forma: class / interface / enum / record / anidada / excluida
# ---------------------------------------------------------------------------
print('\n=== 2. forma de cada caja ===')
pom = open(REPO + '/pom.xml', encoding='utf-8').read()
EXCL = {l.strip() for l in re.findall(r'<exclude>(.+?)</exclude>', pom)}
mal = []
for t in T:
    clave = (t['paquete'], t['nombre'])
    if clave not in ESTER:
        continue
    kind, cola = ESTER[clave]
    esperado = {'class': 'class', 'interface': 'interface', 'enum': 'enum',
                'record': 'class', 'annotation': 'annotation'}[t['kind']]
    if kind != esperado:
        mal.append(f"{t['nombre']}: dibujado como {kind}, es {t['kind']}")
    if t['kind'] == 'record' and 'record' not in cola:
        mal.append(f"{t['nombre']}: es record y no lo dice")
    if t['anidada_en'] and f"anidada en {t['anidada_en']}" not in cola:
        mal.append(f"{t['nombre']}: anidada en {t['anidada_en']} y no lo dice")
    if not t['anidada_en'] and 'anidada' in cola:
        mal.append(f"{t['nombre']}: dice anidada y no lo es")
    if (t['archivo'] in EXCL) != ('excluida en pom.xml' in cola):
        mal.append(f"{t['nombre']}: marca de pom.xml incorrecta")
ok(not mal, f'kind, record, anidada y excluida correctos ({len(mal)} fallos)')
for x in mal[:10]:
    print('      ', x)

# ---------------------------------------------------------------------------
# 3. cada linea de miembro, exacta y en el mismo orden que el codigo
# ---------------------------------------------------------------------------
print('\n=== 3. cada linea de miembro, exacta y en orden ===')


def esc(s):
    return (s or '').replace('{', '(').replace('}', ')')


def linea_campo(c):
    mod = '{static} ' if c['static'] else ''
    return f"{c['vis']} {mod}{c['nombre']} : {esc(c['tipo'])}"


def linea_metodo(m):
    ps = ', '.join(f"{p['nombre']} : {esc(p['tipo'])}" for p in m['params'])
    ret = '' if m.get('constructor') or not m['retorno'] else f" : {esc(m['retorno'])}"
    mod = ('{static} ' if m['static'] else '') + ('{abstract} ' if m['abstract'] else '')
    return f"{m['vis']} {mod}{m['nombre']}({ps}){ret}"


dibujo = CUERPO
difs = 0
for t in T:
    esperada = list(t['literales'])
    if t['literales'] and (t['campos'] or t['metodos']):
        esperada.append('--')
    esperada += [linea_campo(c) for c in t['campos']]
    if t['campos'] and t['metodos']:
        esperada.append('--')
    esperada += [linea_metodo(m) for m in t['metodos']]
    real = dibujo.get((t['paquete'], t['nombre']), [])
    if real != esperada:
        difs += 1
        print(f"\n  {t['nombre']}")
        for i in range(max(len(real), len(esperada))):
            a = esperada[i] if i < len(esperada) else '(nada)'
            b = real[i] if i < len(real) else '(nada)'
            if a != b:
                print(f'     codigo : {a}')
                print(f'     dibujo : {b}')
ok(difs == 0, f'las {sum(len(t["campos"]) + len(t["metodos"]) + len(t["literales"]) for t in T)} '
              f'lineas de miembro coinciden en texto y orden ({difs} tipos con diferencias)')

# ---------------------------------------------------------------------------
# 4. visibilidad y modificadores contra el texto del .java
# ---------------------------------------------------------------------------
print('\n=== 4. visibilidad y static/abstract contra el .java ===')
mal = []
for t in T:
    txt = open(f"{SRC}/{t['archivo']}", encoding='utf-8').read()
    txt = re.sub(r'/\*.*?\*/|//[^\n]*', '', txt, flags=re.S)
    for c in t['campos']:
        if t['kind'] == 'record':
            continue                     # componentes implicitos
        # la declaracion lleva un tipo delante; 'this.color = color;' no
        cs = [x for x in re.finditer(
                  rf'^([^\n;=]*?)\b{c["nombre"]}\b\s*(=|;)', txt, re.M)
              if re.search(r'\w', x.group(1)) and 'this.' not in x.group(1)
              and 'return' not in x.group(1)]
        if not cs:
            continue
        # si hay varias coincidencias, la declaracion es la que lleva
        # modificador de acceso; las demas son usos de una variable local
        con_vis = [x for x in cs if re.search(r'\b(public|private|protected)\b',
                                              x.group(1))]
        cab = (con_vis or cs)[0].group(1)
        esp = ('+' if 'public' in cab else '-' if 'private' in cab
               else '#' if 'protected' in cab
               else '+' if t['kind'] in ('interface', 'annotation') else '~')
        if esp != c['vis']:
            mal.append(f"{t['nombre']}.{c['nombre']}: dibujado {c['vis']}, "
                       f"en el codigo {esp}")
        if ('static' in cab.split()) != c['static']:
            mal.append(f"{t['nombre']}.{c['nombre']}: marca static incorrecta")
    for me in t['metodos']:
        # la declaracion lleva tipo de retorno (o es el constructor) y
        # termina en '{' o ';'; una llamada no cumple ninguna de las dos
        pat = (rf'^[ \t]*((?:(?:public|private|protected|static|final|abstract|'
               rf'synchronized|default|native)\s+)*)'
               rf'([\w.<>\[\],? ]+\s+)?{re.escape(me["nombre"])}\s*\([^;{{]*\)'
               rf'\s*(?:throws [\w., ]+)?\s*[{{;]')
        cands = [x for x in re.finditer(pat, txt, re.M | re.S)
                 if ((x.group(2) and re.search(r'\w', x.group(2))
                      and not re.search(r'\b(return|new|this)\b|\.$',
                                        x.group(2).strip()))
                     or me['nombre'] == t['nombre'])]
        if not cands:
            continue
        con_vis = [x for x in cands
                   if re.search(r'\b(public|private|protected)\b', x.group(1))]
        cab = (con_vis or cands)[0].group(1)
        esp = ('+' if 'public' in cab else '-' if 'private' in cab
               else '#' if 'protected' in cab
               else '+' if t['kind'] in ('interface', 'annotation')
               # el constructor de un enum sin modificador es privado,
               # no package-private (JLS 8.9.2)
               else '-' if (t['kind'] == 'enum'
                            and me['nombre'] == t['nombre']) else '~')
        if esp != me['vis']:
            mal.append(f"{t['nombre']}.{me['nombre']}(): dibujado {me['vis']}, "
                       f"en el codigo {esp}")
ok(not mal, f'visibilidad y static de campos y metodos ({len(mal)} fallos)')
for x in mal[:12]:
    print('      ', x)

# ---------------------------------------------------------------------------
# 5. las cajas de referencia van vacias y con su paquete
# ---------------------------------------------------------------------------
print('\n=== 5. cajas de referencia ===')
# dos tipos pueden compartir nombre simple (entity.EstadoMesa y
# enums.EstadoMesa): indexar por nombre a secas hace que uno pise al otro y
# que este chequeo reclame el estereotipo del que no es
_porn = collections.defaultdict(list)
for t in T:
    _porn[t['nombre']].append(t)
POR, DUPES = {}, {n for n, v in _porn.items() if len(v) > 1}
for n_, v_ in _porn.items():
    POR[n_] = v_[0]
mal = []
for f in sorted(glob.glob(DIAG + '/puml_clases/0[1-7]_*.puml')):
    s = open(f, encoding='utf-8').read()
    bloque = re.search(r'package "referencias".*?\n(.*?)\n\}\n(?=\w|$)', s, re.S)
    if not bloque:
        continue
    for b in re.finditer(r'^\s*(?:class|interface|enum|annotation)\s+(\w+)\s+'
                         r'<<(\w+|\(raiz\))>>', bloque.group(1), re.M):
        n, paq = b.group(1), b.group(2)
        if n in DUPES:
            # con el nombre duplicado, vale cualquiera de los paquetes reales
            if paq in {x['paquete'] for x in _porn[n]}:
                continue
        if n in POR and POR[n]['paquete'] != paq:
            mal.append(f'{os.path.basename(f)}: {n} marcada <<{paq}>>, '
                       f'es de {POR[n]["paquete"]}')
ok(not mal, f'el estereotipo de cada caja de referencia es su paquete ({len(mal)})')

# ---------------------------------------------------------------------------
# 6. ninguna relacion repetida ni perdida entre los siete mapas
# ---------------------------------------------------------------------------
print('\n=== 6. las relaciones no se repiten entre mapas ===')
vistas = collections.Counter()
for f in sorted(glob.glob(DIAG + '/puml_clases/*_rel_*.puml')):
    s = open(f, encoding='utf-8').read()
    for l in s.split('\n'):
        if re.search(r'-\[#\w+(,dashed)?\]->|--\|>', l) and '-[hidden]' not in l:
            vistas[(os.path.basename(f), l.strip())] += 1
dentro = [k for k, v in vistas.items() if v > 1]
ok(not dentro, f'ninguna linea repetida dentro de un mapa ({len(dentro)})')
porlinea = collections.Counter(l for _f, l in vistas)
entre = {l: c for l, c in porlinea.items() if c > 1}
ok(not entre, f'ninguna linea repetida en dos mapas ({len(entre)})')
print(f'  lineas totales dibujadas en los siete mapas: {sum(porlinea.values())}')

print('\n=== resultado ===')
print(f'{len(fallos)} comprobaciones fallidas' if fallos else 'todo correcto')
