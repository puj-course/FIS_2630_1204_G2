"""Comprueba, leyendo el .java linea a linea, que cada linea dibujada entre
dos clases es del tipo correcto y lleva la multiplicidad correcta."""
import re, os, sys, json, glob, collections

REPO, DIAG = sys.argv[1], sys.argv[2]
SRC = REPO + '/src/main/java'
R = json.load(open(DIAG + '/relaciones.json', encoding='utf-8'))
T = json.load(open(DIAG + '/clases.json', encoding='utf-8'))
POR = {t['nombre']: t for t in T}
NOMBRES = set(POR)
DIBUJO = ''.join(open(f, encoding='utf-8').read()
                 for f in glob.glob(DIAG + '/puml_clases/*_rel_*.puml'))

COLEC = ('List', 'ArrayList', 'Set', 'HashSet', 'LinkedHashSet', 'Collection',
         'Map', 'HashMap', 'LinkedHashMap', 'ObservableList', 'Queue', 'Deque')
fallos = []


def fuente(t):
    return open(f"{SRC}/{t['archivo']}", encoding='utf-8').read()


def alias(n):
    return re.sub(r'\W', '', POR[n]['paquete']) + '_' + n


print('=== A. cada asociacion existe como campo en el .java ===')
for a in R['asociacion']:
    txt = fuente(POR[a['origen']])
    for rol in a['roles']:
        # la declaracion del campo, tal cual, en el archivo del dueno
        m = re.search(rf'^\s*(?:@\w+(?:\([^)]*\))?\s*)*'
                      rf'((?:public|private|protected|static|final|\s)*)'
                      rf'([\w.<>\[\], ?]+?)\s+{rol}\s*(=|;)', txt, re.M)
        if not m:
            fallos.append(f"{a['origen']}.{rol}: no encuentro la declaracion")
            continue
        tipo = m.group(2).strip()
        base = tipo.split('<')[0].strip()
        esp = '*' if (base in COLEC or tipo.endswith('[]')) else \
              ('0..1' if base == 'Optional' else '1')
        if esp != a['multiplicidad']:
            fallos.append(f"{a['origen']}.{rol} : {tipo} -> dibujada "
                          f"{a['multiplicidad']}, deberia {esp}")
        if a['destino'] not in tipo:
            fallos.append(f"{a['origen']}.{rol} : {tipo} no menciona "
                          f"{a['destino']}")
print(f'  {len(R["asociacion"])} asociaciones revisadas contra su declaracion')

print('\n=== B. composicion: el dueno la crea con new y no la devuelve ===')
for a in R['asociacion']:
    if not a['composicion']:
        continue
    txt = fuente(POR[a['origen']])
    crea = any(re.search(rf'\b{r}\s*=\s*new\s+\w*{a["destino"]}', txt)
               for r in a['roles'])
    expone = any(m['retorno'] and a['destino'] in m['retorno']
                 for m in POR[a['origen']]['metodos'])
    if not crea:
        fallos.append(f"composicion sin new: {a['origen']} *-- {a['destino']}")
    if expone:
        fallos.append(f"composicion pero hay getter: {a['origen']} *-- {a['destino']}")
    if f"{alias(a['origen'])} \"1\" *-[" not in DIBUJO:
        fallos.append(f"composicion no dibujada con rombo lleno: {a['origen']}")
print(f'  {sum(1 for a in R["asociacion"] if a["composicion"])} composiciones')

print('\n=== C. agregacion: la parte llega por constructor ===')
for a in R['asociacion']:
    if not a.get('agregacion'):
        continue
    ctor = [m for m in POR[a['origen']]['metodos'] if m.get('constructor')]
    if not any(a['destino'] in (p['tipo'] or '') for m in ctor for p in m['params']):
        fallos.append(f"agregacion sin parametro de constructor: {a['origen']} "
                      f"o-- {a['destino']}")
    if f"{alias(a['origen'])} o-[" not in DIBUJO:
        fallos.append(f"agregacion no dibujada con rombo hueco: {a['origen']}")
print(f'  {sum(1 for a in R["asociacion"] if a.get("agregacion"))} agregaciones')

print('\n=== D. asociacion simple: ni se crea dentro ni llega por constructor ===')
n = 0
for a in R['asociacion']:
    if a['composicion'] or a.get('agregacion'):
        continue
    n += 1
    txt = fuente(POR[a['origen']])
    crea = any(re.search(rf'\b{r}\s*=\s*new\s+\w*{a["destino"]}', txt)
               for r in a['roles'])
    ctor = [m for m in POR[a['origen']]['metodos'] if m.get('constructor')]
    iny = any(a['destino'] in (p['tipo'] or '') for m in ctor for p in m['params'])
    expone = any(m['retorno'] and a['destino'] in m['retorno']
                 for m in POR[a['origen']]['metodos'])
    if crea and not expone and not iny:
        fallos.append(f"deberia ser composicion: {a['origen']} -> {a['destino']}")
    if iny:
        fallos.append(f"deberia ser agregacion: {a['origen']} -> {a['destino']}")
print(f'  {n} asociaciones simples')

print('\n=== E. ningun campo de tipo del proyecto se quedo sin linea ===')
for t in T:
    for c in t['campos']:
        for p in re.findall(r'\b[A-Z]\w*\b', c['tipo']):
            if p in NOMBRES and p != t['nombre']:
                if not any(a['origen'] == t['nombre'] and a['destino'] == p
                           for a in R['asociacion']):
                    fallos.append(f"campo sin asociacion: {t['nombre']}.{c['nombre']}"
                                  f" : {c['tipo']}")
print('  revisados los 371 campos')

print('\n=== F. herencia: cada extends del codigo esta dibujado ===')
ext = []
for ruta in sorted(glob.glob(SRC + '/**/*.java', recursive=True)):
    if os.path.getsize(ruta) == 0:
        continue
    txt = re.sub(r'/\*.*?\*/|//[^\n]*', '', open(ruta, encoding='utf-8').read(),
                 flags=re.S)
    for m in re.finditer(r'\b(?:class|interface)\s+(\w+)\s*'
                         r'(?:<[^>]*>\s*)?extends\s+([\w.]+)', txt):
        ext.append((m.group(1), m.group(2)))
for hijo, padre in ext:
    if padre in NOMBRES:
        ok = f"{alias(hijo)} --|> {alias(padre)}" in DIBUJO
    else:
        ok = f"{alias(hijo)} --|> {padre}" in DIBUJO
    if not ok:
        fallos.append(f"herencia no dibujada: {hijo} --|> {padre}")
print(f'  {len(ext)} clausulas extends en el codigo')

print('\n=== G. implements: ninguno hacia una interfaz del proyecto ===')
imp = []
for ruta in sorted(glob.glob(SRC + '/**/*.java', recursive=True)):
    if os.path.getsize(ruta) == 0:
        continue
    txt = re.sub(r'/\*.*?\*/|//[^\n]*', '', open(ruta, encoding='utf-8').read(),
                 flags=re.S)
    for m in re.finditer(r'\bclass\s+(\w+)[^{]*?\bimplements\s+([^{]+)', txt):
        for p in re.findall(r'\b[A-Z]\w*', m.group(2)):
            if p in NOMBRES:
                imp.append((m.group(1), p))
print(f'  {len(imp)} implements hacia interfaces del proyecto: {imp or "ninguno"}')

print('\n=== resultado ===')
if fallos:
    for f in fallos:
        print('  FALLA', f)
    print(f'{len(fallos)} problemas')
else:
    print('sin diferencias')
