"""Diagrama 09: las clases de src/test/java y lo que prueban de src/main/java.

Todo sale de clases_test.json, clases.json y del propio codigo fuente.
"""
import json, os, re

REPO = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                      'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
DIR = BASE + '/puml_clases'
os.makedirs(DIR, exist_ok=True)

T = json.load(open(BASE + '/clases.json', encoding='utf-8'))
TT = json.load(open(BASE + '/clases_test.json', encoding='utf-8'))
POR = {t['nombre']: t for t in T}
NOMBRES = set(POR)

import importlib.util as _iu
_spec = _iu.spec_from_file_location('gcc', BASE + '/gen_clases_completo.py')

COLOR = {'entity': '#F3E8FF', 'controller': '#DBEAFE', 'repository': '#FFEDD5',
         'service': '#DCFCE7', 'dao': '#DCFCE7', 'dto': '#FEF9C3',
         'enums': '#FEF9C3', 'exceptions': '#FEF9C3', 'session': '#FEF9C3',
         'util': '#FEF9C3', 'ConexionBD': '#F1F5F9', '(raiz)': '#F1F5F9'}

CAB = '''@startuml
skinparam backgroundColor #FFFFFF
skinparam shadowing false
skinparam defaultFontName Helvetica
skinparam roundCorner 4
skinparam nodesep 35
skinparam ranksep 60
skinparam padding 3
skinparam ClassFontSize 13
skinparam ClassAttributeFontSize 11
skinparam ClassStereotypeFontSize 10
skinparam ClassBorderColor #334155
skinparam ArrowColor #1E293B
skinparam ArrowFontSize 10
skinparam PackageBorderColor #94A3B8
skinparam PackageFontStyle bold
skinparam ClassAttributeIconSize 0
hide empty members
'''


def esc(s):
    return (s or '').replace('{', '(').replace('}', ')')


def firma(m):
    ps = ', '.join(f"{p['nombre']} : {esc(p['tipo'])}" for p in m['params'])
    ret = '' if m.get('constructor') or not m['retorno'] else f" : {esc(m['retorno'])}"
    mod = ''
    if m['static']:
        mod += '{static} '
    if m['abstract']:
        mod += '{abstract} '
    return f"{m['vis']} {mod}{m['nombre']}({ps}){ret}"


def caja(t, sangria='  '):
    kind = {'class': 'class', 'interface': 'interface', 'enum': 'enum',
            'record': 'class', 'annotation': 'annotation'}[t['kind']]
    est = [f"anidada en {t['anidada_en']}"] if t['anidada_en'] else [t['paquete']]
    L = [f"{sangria}{kind} {t['nombre']} <<{' · '.join(est)}>> #FEF9C3 {{"]
    for c in t['campos']:
        mod = '{static} ' if c['static'] else ''
        L.append(f"{sangria}  {c['vis']} {mod}{c['nombre']} : {esc(c['tipo'])}")
    if t['campos'] and t['metodos']:
        L.append(f'{sangria}  --')
    for m in t['metodos']:
        L.append(f'{sangria}  {firma(m)}')
    L.append(f'{sangria}}}')
    return '\n'.join(L)


def limpiar(txt):
    txt = re.sub(r'/\*.*?\*/', ' ', txt, flags=re.S)
    txt = re.sub(r'//[^\n]*', ' ', txt)
    txt = re.sub(r'"(?:\\.|[^"\\])*"', ' ', txt)
    txt = re.sub(r"'(?:\\.|[^'\\])*'", ' ', txt)
    return txt


# ---- que clase de produccion usa cada archivo de prueba -------------------
SRCT = REPO + '/src/test/java'
usos = {}
for t in TT:
    if t['anidada_en']:
        continue
    txt = limpiar(open(f"{SRCT}/{t['archivo']}", encoding='utf-8').read())
    usos[t['nombre']] = sorted({p for p in re.findall(r'\b[A-Z]\w*\b', txt)
                                if p in NOMBRES})

referidas = sorted({p for v in usos.values() for p in v})

L = [CAB]
L.append('package "src/test/java" {')
orden = sorted(TT, key=lambda x: (x['archivo'], x['nombre']))
for t in orden:
    L.append(caja(t))
nm = [t['nombre'] for t in orden]
COLS = 3
for i in range(len(nm)):
    if i % COLS and i:
        L.append(f'  {nm[i-1]} -[hidden]right- {nm[i]}')
    if i >= COLS:
        L.append(f'  {nm[i-COLS]} -[hidden]down- {nm[i]}')
L.append('}')

L.append('package "src/main/java" #FAFAFA {')
for n in referidas:
    t2 = POR[n]
    kind = {'class': 'class', 'interface': 'interface', 'enum': 'enum',
            'record': 'class', 'annotation': 'annotation'}[t2['kind']]
    L.append(f"  {kind} {n} <<{t2['paquete']}>> {COLOR[t2['paquete']]} {{\n  }}")
COLS = 5
for i in range(len(referidas)):
    if i % COLS and i:
        L.append(f'  {referidas[i-1]} -[hidden]right- {referidas[i]}')
    if i >= COLS:
        L.append(f'  {referidas[i-COLS]} -[hidden]down- {referidas[i]}')
L.append('}')

n_rel = 0
for origen in sorted(usos):
    for destino in usos[origen]:
        L.append(f'{origen} ..> {destino}')
        n_rel += 1
L.append('@enduml')
open(f'{DIR}/15_pruebas.puml', 'w', encoding='utf-8').write('\n'.join(L) + '\n')

print(f'15_pruebas: {len(TT)} tipos de prueba, {len(referidas)} clases probadas, '
      f'{n_rel} dependencias')
for o in sorted(usos):
    print(f'   {o:28} -> {len(usos[o])}')
