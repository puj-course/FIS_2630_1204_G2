"""Deriva del codigo las relaciones UML entre los 99 tipos de GastroFlow.

  - generalizacion : extends entre tipos del proyecto
  - realizacion    : implements de interfaces del proyecto
  - asociacion     : un campo cuyo tipo es otro tipo del proyecto
                     multiplicidad 1 para un campo escalar, * para List/Set/Map/arreglo
  - dependencia    : un tipo del proyecto que aparece solo en parametros o retornos

No se marca composicion ni agregacion salvo que el codigo lo demuestre:
el campo se instancia dentro del dueno (new) y no se expone con un getter.
"""
import json, re, collections

import os
ENTRADA = os.environ.get('GF_OUT', '/home/claude/diagramas') + '/clases.json'
SRC = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293') + '/src/main/java'
SALIDA = os.environ.get('GF_OUT', '/home/claude/diagramas') + '/relaciones.json'

T = json.load(open(ENTRADA, encoding='utf-8'))
NOMBRES = {t['nombre'] for t in T}
POR_NOMBRE = {t['nombre']: t for t in T}

COLECCIONES = ('List', 'ArrayList', 'Set', 'HashSet', 'LinkedHashSet', 'Collection',
               'Map', 'HashMap', 'LinkedHashMap', 'Optional', 'ObservableList')


def tipos_en(expr):
    """Nombres de tipos del proyecto que aparecen en una expresion de tipo."""
    return [p for p in re.findall(r'\b[A-Z]\w*\b', expr or '') if p in NOMBRES]


def multiplicidad(expr):
    if not expr:
        return '1'
    if expr.rstrip().endswith('[]'):
        return '*'
    base = expr.split('<')[0].strip()
    if base in COLECCIONES:
        return '0..1' if base == 'Optional' else '*'
    return '1'


# ---------------------------------------------------------- generalizacion --
def base(expr):
    """Nombre del tipo sin sus argumentos genericos: la clase padre real.
    'JpaRepository<Pago, Long>' -> 'JpaRepository' (Pago es un argumento,
    no la superclase)."""
    return (expr or '').split('<')[0].strip()


generalizacion, realizacion = [], []
for t in T:
    if t['extends']:
        p = base(t['extends'])
        if p in NOMBRES and p != t['nombre']:
            generalizacion.append({'hijo': t['nombre'], 'padre': p})
    if t['implements']:
        for trozo in re.split(r',(?![^<]*>)', t['implements']):
            p = base(trozo)
            if p in NOMBRES and p != t['nombre']:
                realizacion.append({'clase': t['nombre'], 'interfaz': p})

# ------------------------------------------------------------- asociacion --
asociacion = []
for t in T:
    vistos = {}
    for c in t['campos']:
        for p in tipos_en(c['tipo']):
            if p == t['nombre']:
                continue
            m = multiplicidad(c['tipo'])
            clave = (p, m)
            if clave in vistos:
                vistos[clave]['roles'].append(c['nombre'])
                continue
            vistos[clave] = {'origen': t['nombre'], 'destino': p,
                             'multiplicidad': m, 'roles': [c['nombre']],
                             'static': c['static'], 'final': c['final']}
    asociacion += list(vistos.values())

# ----------------------------------- composicion demostrable por el codigo --
# el campo se crea con new dentro del dueno y no hay getter que lo devuelva
fuente = {}
for t in T:
    if t['archivo'] not in fuente:
        fuente[t['archivo']] = open(f"{SRC}/{t['archivo']}", encoding='utf-8').read()
for a in asociacion:
    t = POR_NOMBRE[a['origen']]
    txt = fuente[t['archivo']]
    crea = any(re.search(rf'\b{r}\s*=\s*new\s+\w*{a["destino"]}', txt) for r in a['roles'])
    expone = any(m['retorno'] and a['destino'] in m['retorno'] for m in t['metodos'])
    # el dueno recibe la parte por constructor: la parte existe fuera de el
    inyectada = any(a['destino'] in (p['tipo'] or '')
                    for m in t['metodos'] if m.get('constructor')
                    for p in m['params'])
    a['composicion'] = bool(crea and not expone and not inyectada)
    a['agregacion'] = bool(inyectada and not a['composicion'])

# ------------------------------------------------------------ dependencia --
# Una dependencia UML existe si el tipo se usa en cualquier parte: firma,
# cuerpo del metodo, clausula throws o captura. Se quitan antes los
# comentarios y las cadenas para no contar nombres que aparezcan ahi.
asociados = {(a['origen'], a['destino']) for a in asociacion}


def limpiar_fuente(txt):
    txt = re.sub(r'/\*.*?\*/', ' ', txt, flags=re.S)
    txt = re.sub(r'//[^\n]*', ' ', txt)
    txt = re.sub(r'"(?:\\.|[^"\\])*"', ' ', txt)
    txt = re.sub(r"'(?:\\.|[^'\\])*'", ' ', txt)
    return txt


# nombres declarados dentro del propio archivo (para no contarse a si mismo)
propios = {}
for t in T:
    propios.setdefault(t['archivo'], set()).add(t['nombre'])

dependencia = []
for t in T:
    if t['anidada_en']:
        continue                      # la anidada comparte archivo con su contenedora
    txt = limpiar_fuente(open(f"{SRC}/{t['archivo']}", encoding='utf-8').read())
    usados = {p for p in re.findall(r'\b[A-Z]\w*\b', txt) if p in NOMBRES}
    usados -= propios.get(t['archivo'], set())
    for p in sorted(usados):
        if (t['nombre'], p) not in asociados:
            dependencia.append({'origen': t['nombre'], 'destino': p})

R = {'generalizacion': generalizacion, 'realizacion': realizacion,
     'asociacion': asociacion, 'dependencia': dependencia}
json.dump(R, open(SALIDA, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)

print(f"generalizacion (extends): {len(generalizacion)}")
for g in generalizacion:
    print(f"   {g['hijo']} --|> {g['padre']}")
print(f"\nrealizacion (implements): {len(realizacion)}")
for r in realizacion:
    print(f"   {r['clase']} ..|> {r['interfaz']}")
comp = [a for a in asociacion if a['composicion']]
print(f"\nasociacion: {len(asociacion)}   (de ellas, composicion demostrable: {len(comp)})")
for a in comp:
    print(f"   {a['origen']} *-- \"{a['multiplicidad']}\" {a['destino']}  ({', '.join(a['roles'])})")
agr = [a for a in asociacion if a.get('agregacion')]
print(f"\nagregacion (la parte llega por constructor): {len(agr)}")
for a in agr:
    print(f"   {a['origen']} o-- \"{a['multiplicidad']}\" {a['destino']}  ({', '.join(a['roles'])})")
print(f"\ndependencia (uso en cualquier parte del cuerpo): {len(dependencia)}")
m = collections.Counter(a['multiplicidad'] for a in asociacion)
print(f"\nmultiplicidades: {dict(m)}")
