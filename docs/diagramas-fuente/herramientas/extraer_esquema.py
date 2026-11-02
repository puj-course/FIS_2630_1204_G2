"""Extrae tablas, columnas clave y llaves foraneas de los DDL de GastroFlow.

Lee db/ddl/*.ddl y tambien las tablas que crean las migraciones
(db/migrations/*.sql), porque scripts/setup.sh las cuenta como parte del
esquema final.
"""
import re, os, json, glob

REPO = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                      'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
DDL = REPO + '/db/ddl'
MIG = REPO + '/db/migrations'

tablas = {}


def parsear(txt, inicio, archivo):
    """Columnas, PK y FK de un CREATE TABLE que empieza en 'inicio'."""
    cols, pk, fks = [], [], []
    fin = inicio
    for linea in txt[inicio:].splitlines():
        s = linea.strip()
        fin += len(linea) + 1
        if not s or s.startswith('--') or s.upper().startswith(
                ('CONSTRAINT', 'FOREIGN KEY', 'PRIMARY KEY', 'UNIQUE', 'CHECK',
                 'REFERENCES', 'ON DELETE', 'ON UPDATE')):
            continue
        if s.startswith(')'):
            break
        cm = re.match(r'(\w+)\s+([A-Z][A-Z0-9 ]*(?:\([\d,\s]+\))?)', s)
        if cm:
            col = cm.group(1)
            cols.append((col, cm.group(2).strip()))
            # clave primaria declarada en la propia columna
            if re.search(r'\bPRIMARY KEY\b', s, re.I):
                pk.append(col)
            # llave foranea declarada en la propia columna
            fm = re.search(r'\bREFERENCES\s+(\w+)\s*\(([^)]+)\)', s, re.I)
            if fm:
                fks.append({'col': col, 'tabla': fm.group(1),
                            'ref': fm.group(2).strip()})
    cuerpo = txt[inicio:fin]
    # clave primaria y llaves foraneas declaradas como restriccion de tabla
    mpk = re.search(r'PRIMARY KEY\s*\(([^)]+)\)', cuerpo, re.I)
    if mpk:
        pk = [c.strip() for c in mpk.group(1).split(',')]
    for fm in re.finditer(
            r'FOREIGN KEY\s*\(([^)]+)\)\s*(?:\n\s*)?REFERENCES\s+(\w+)\s*\(([^)]+)\)',
            cuerpo, re.I):
        fks.append({'col': fm.group(1).strip(), 'tabla': fm.group(2).strip(),
                    'ref': fm.group(3).strip()})
    unicas, vistas = [], set()
    for f in fks:
        clave = (f['col'], f['tabla'], f['ref'])
        if clave not in vistas:
            vistas.add(clave)
            unicas.append(f)
    return {'cols': cols, 'pk': pk, 'fks': unicas, 'archivo': archivo}


for ruta in sorted(glob.glob(f'{DDL}/*.ddl')):
    txt = open(ruta, encoding='utf-8').read()
    m = re.search(r'CREATE TABLE\s+(?:IF NOT EXISTS\s+)?(\w+)\s*\(', txt, re.I)
    if m:
        tablas[m.group(1)] = parsear(txt, m.end(), os.path.basename(ruta))

# tablas creadas por una migracion (no tienen archivo propio en db/ddl)
for ruta in sorted(glob.glob(f'{MIG}/*.sql')):
    txt = open(ruta, encoding='utf-8').read()
    for m in re.finditer(r'CREATE TABLE\s+(?:IF NOT EXISTS\s+)?(\w+)\s*\(', txt, re.I):
        if m.group(1).lower().startswith('hu'):     # tablas TEMP de trabajo
            continue
        if 'TEMP TABLE' in txt[max(0, m.start() - 20):m.start() + 20].upper():
            continue
        tablas.setdefault(m.group(1), parsear(txt, m.end(), os.path.basename(ruta)))

# columnas agregadas por migraciones
migs = []
for ruta in sorted(glob.glob(f'{MIG}/*.sql')):
    txt = open(ruta, encoding='utf-8').read()
    for am in re.finditer(r'ALTER TABLE\s+(\w+)[\s\S]{0,80}?ADD COLUMN'
                          r'(?:\s+IF NOT EXISTS)?\s+(\w+)\s+([A-Z][A-Z0-9()\,\s]*)',
                          txt, re.I):
        migs.append((os.path.basename(ruta), am.group(1), am.group(2),
                     am.group(3).strip().split('\n')[0]))

print(f'TABLAS: {len(tablas)}')
for t in sorted(tablas):
    d = tablas[t]
    fks = ', '.join(f"{f['col']}->{f['tabla']}" for f in d['fks']) or '-'
    print(f"  {t:28} pk={','.join(d['pk']):22} cols={len(d['cols']):2}  fk: {fks}")
print(f"\nFK totales: {sum(len(d['fks']) for d in tablas.values())}")

print(f'\nCOLUMNAS AGREGADAS POR MIGRACIONES: {len(migs)}')
for m in migs:
    print(f'  {m[0]:45} {m[1]}.{m[2]}  {m[3][:40]}')

json.dump(tablas, open(os.environ.get('GF_OUT', '/home/claude/diagramas')
                       + '/esquema.json', 'w'), indent=1, ensure_ascii=False)
