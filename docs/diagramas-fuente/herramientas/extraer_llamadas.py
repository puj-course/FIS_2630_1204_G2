"""Resuelve, con tree-sitter, que metodo de que clase del proyecto llama cada
clase, y que tablas de la base toca cada repository o dao.

Salida: llamadas.json
  llamadas : [{origen, destino, metodo, via}]   via = campo | local | estatico
  sql      : {clase: {tablas: [...], funciones: [...]}}
"""
import glob, json, os, re, collections
import tree_sitter_java
from tree_sitter import Language, Parser

REPO = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                      'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
SRC = REPO + '/src/main/java'

T = json.load(open(BASE + '/clases.json', encoding='utf-8'))
POR = {t['nombre']: t for t in T}
NOMBRES = set(POR)
METODOS = {n: {m['nombre'] for m in t['metodos']} for n, t in POR.items()}

LANG = Language(tree_sitter_java.language())
PARSER = Parser(LANG)


def txt(n, src):
    return src[n.start_byte:n.end_byte].decode('utf-8')


def base_tipo(expr):
    """'List<Pedido>' -> 'Pedido' si Pedido es del proyecto; si no, la raiz."""
    partes = [p for p in re.findall(r'\b[A-Z]\w*\b', expr or '') if p in NOMBRES]
    return partes[0] if partes else (expr or '').split('<')[0].strip()


def recoger_vars(nodo, src, mapa):
    """variable -> tipo declarado, para campos, parametros y locales."""
    for n in nodo.named_children:
        if n.type in ('field_declaration', 'local_variable_declaration'):
            t = n.child_by_field_name('type')
            tipo = txt(t, src) if t else ''
            for d in n.named_children:
                if d.type == 'variable_declarator':
                    nm = d.child_by_field_name('name')
                    if nm:
                        mapa[txt(nm, src)] = tipo
        elif n.type in ('formal_parameter', 'spread_parameter'):
            t = n.child_by_field_name('type')
            nm = n.child_by_field_name('name')
            if t and nm:
                mapa[txt(nm, src)] = txt(t, src)
        recoger_vars(n, src, mapa)


llamadas = []
vistas = set()
for ruta in sorted(glob.glob(SRC + '/**/*.java', recursive=True)):
    if '/database/' in ruta or os.path.getsize(ruta) == 0:
        continue
    datos = open(ruta, 'rb').read()
    arbol = PARSER.parse(datos)
    rel = os.path.relpath(ruta, SRC)
    # tipo de nivel superior del archivo: el dueno de las llamadas
    propios = [t['nombre'] for t in T if t['archivo'] == rel]
    duenos = [t['nombre'] for t in T if t['archivo'] == rel and not t['anidada_en']]
    if not duenos:
        continue
    dueno = duenos[0]

    mapa = {}
    recoger_vars(arbol.root_node, datos, mapa)

    pila = [arbol.root_node]
    while pila:
        n = pila.pop()
        pila.extend(n.children)
        if n.type == 'method_reference':
            # Clase::metodo o campo::metodo, que tambien es una llamada
            partes = [c for c in n.children if c.type not in ('::',)]
            if len(partes) == 2:
                r = txt(partes[0], datos)
                m = txt(partes[1], datos)
                cl = (base_tipo(mapa[r]) if r in mapa
                      else r if r in NOMBRES else None)
                if cl and cl not in propios and m in METODOS.get(cl, set()):
                    clave = (dueno, cl, m)
                    if clave not in vistas:
                        vistas.add(clave)
                        llamadas.append({'origen': dueno, 'destino': cl,
                                         'metodo': m, 'via': 'referencia'})
            continue
        if n.type != 'method_invocation':
            continue
        obj = n.child_by_field_name('object')
        nom = n.child_by_field_name('name')
        if nom is None:
            continue
        metodo = txt(nom, datos)
        if obj is None:
            continue
        receptor = txt(obj, datos)
        via, destino = None, None
        if receptor in mapa:                       # campo, parametro o local
            destino = base_tipo(mapa[receptor])
            via = 'campo'
        elif receptor in NOMBRES:                  # llamada estatica
            destino = receptor
            via = 'estatico'
        elif re.fullmatch(r'new\s+(\w+)\s*\(.*\)', receptor, re.S):
            destino = re.match(r'new\s+(\w+)', receptor).group(1)
            via = 'nuevo'
        elif obj.type == 'method_invocation':
            # llamada encadenada: el receptor es lo que devuelve otra llamada.
            # Caso tipico, el singleton: SesionUsuario.getInstance().getRol()
            o2 = obj.child_by_field_name('object')
            n2 = obj.child_by_field_name('name')
            if o2 is not None and n2 is not None:
                r2 = txt(o2, datos)
                clase2 = (base_tipo(mapa[r2]) if r2 in mapa
                          else r2 if r2 in NOMBRES else None)
                if clase2 in POR:
                    ret = next((m['retorno'] for m in POR[clase2]['metodos']
                                if m['nombre'] == txt(n2, datos)), None)
                    destino = base_tipo(ret) if ret else None
                    via = 'encadenada'
        if not destino or destino not in NOMBRES or destino in propios:
            continue
        if metodo not in METODOS.get(destino, set()):
            continue                               # metodo heredado o externo
        clave = (dueno, destino, metodo)
        if clave in vistas:
            continue
        vistas.add(clave)
        llamadas.append({'origen': dueno, 'destino': destino,
                         'metodo': metodo, 'via': via})

# ------------------------------------------------- SQL de cada repositorio --
TABLAS = set(json.load(open(BASE + '/esquema.json', encoding='utf-8')))
sql = {}
for t in T:
    if t['paquete'] not in ('repository', 'dao') or t['anidada_en']:
        continue
    texto = open(f"{SRC}/{t['archivo']}", encoding='utf-8').read()
    cadenas = ' '.join(re.findall(r'"((?:\\.|[^"\\])*)"', texto)) + ' ' + \
              ' '.join(re.findall(r'"""(.*?)"""', texto, re.S))
    usadas, escritas = set(), set()
    for m in re.finditer(r'\b(FROM|JOIN|INTO|UPDATE|DELETE\s+FROM)\s+([a-z_][\w]*)',
                         cadenas, re.I):
        tabla = m.group(2).lower()
        if tabla in TABLAS:
            usadas.add(tabla)
            if m.group(1).upper() in ('INTO', 'UPDATE') or \
               m.group(1).upper().startswith('DELETE'):
                escritas.add(tabla)
    funcs = {f.lower() for f in re.findall(r'\b(fn_\w+)\s*\(', cadenas, re.I)}

    # Tablas que el codigo elige en tiempo de ejecucion: metodos con un
    # parametro llamado tabla o tablas (existeTabla, primeraTablaExistente),
    # a los que se les pasa el nombre como literal.
    dinamicas = set()
    con_param_tabla = {m['nombre'] for m in t['metodos']
                       if any(p['nombre'] in ('tabla', 'tablas')
                              for p in m['params'])}
    sin_com = re.sub(r'/\*.*?\*/|//[^\n]*', '', texto, flags=re.S)
    for nom in con_param_tabla:
        for m in re.finditer(rf'\b{nom}\s*\(', sin_com):
            hondo, j = 1, m.end()
            while j < len(sin_com) and hondo:
                hondo += (sin_com[j] == '(') - (sin_com[j] == ')')
                j += 1
            for lit in re.findall(r'"([a-z_][a-z0-9_]*)"', sin_com[m.end():j]):
                if lit in TABLAS:
                    dinamicas.add(lit)
    nuevas = dinamicas - usadas
    usadas |= dinamicas

    if usadas or funcs:
        sql[t['nombre']] = {'tablas': sorted(usadas),
                            'escribe': sorted(escritas),
                            'dinamicas': sorted(nuevas),
                            'funciones': sorted(funcs)}

json.dump({'llamadas': llamadas, 'sql': sql},
          open(BASE + '/llamadas.json', 'w', encoding='utf-8'),
          indent=1, ensure_ascii=False)

print(f'llamadas resueltas entre clases del proyecto: {len(llamadas)}')
c = collections.Counter(l['destino'] for l in llamadas)
print('clases mas llamadas:', c.most_common(8))
v = collections.Counter(l['via'] for l in llamadas)
print('por via:', dict(v))
print(f'\nrepositorios y dao con SQL reconocido: {len(sql)}')
tot = sorted({t for d in sql.values() for t in d['tablas']})
print(f'tablas distintas alcanzadas desde el codigo: {len(tot)} de {len(TABLAS)}')
print('tablas de la base que ningun repositorio toca:',
      sorted(TABLAS - set(tot)) or 'ninguna')
fn = sorted({f for d in sql.values() for f in d['funciones']})
print('funciones de la base llamadas desde el codigo:', fn or 'ninguna')
