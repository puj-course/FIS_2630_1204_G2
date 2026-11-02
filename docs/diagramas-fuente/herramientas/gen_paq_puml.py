"""Diagrama de paquetes de GastroFlow en PlantUML.

Los paquetes son los directorios de src/main/java y cada flecha se cuenta
sobre los import reales: el numero es cuantos archivos del paquete de origen
importan al menos una clase del paquete de destino.
"""
import os, re, glob, json, collections

REPO = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                      'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
SRC = REPO + '/src/main/java'

archivos = sorted(glob.glob(SRC + '/**/*.java', recursive=True))
paquete_de = {}
ficheros, vacios = collections.Counter(), collections.Counter()
for a in archivos:
    rel = os.path.relpath(a, SRC)
    p = rel.split(os.sep)[0] if os.sep in rel else '(raiz)'
    paquete_de[a] = p
    ficheros[p] += 1
    if os.path.getsize(a) == 0:
        vacios[p] += 1

T = json.load(open(BASE + '/clases.json', encoding='utf-8'))
clases = collections.Counter(t['paquete'] for t in T if not t['anidada_en'])
anidadas = collections.Counter(t['paquete'] for t in T if t['anidada_en'])
for p in ficheros:
    clases.setdefault(p, 0)

PAQ = sorted(ficheros)
usa = collections.Counter()
for a in archivos:
    txt = open(a, encoding='utf-8').read()
    imp = set(re.findall(r'^import\s+([\w.]+)\s*;', txt, re.M))
    destinos = {i.split('.')[0] for i in imp} & set(PAQ)
    # las clases de la raiz se usan sin import; se detectan por el nombre
    for d in destinos - {paquete_de[a]}:
        usa[(paquete_de[a], d)] += 1

CADENA = [('(raiz)', 'controller'), ('controller', 'service'),
          ('service', 'repository'), ('service', 'dao'),
          ('controller', 'dao'), ('repository', 'ConexionBD'),
          ('dao', 'ConexionBD')]
SALTO = [('controller', 'repository'), ('service', 'ConexionBD'),
         ('controller', 'ConexionBD')]
FLUJO = ['(raiz)', 'controller', 'service', 'repository', 'dao', 'ConexionBD']
APOYO = [p for p in PAQ if p not in FLUJO]

L = ['@startuml',
     'skinparam backgroundColor #FFFFFF', 'skinparam shadowing false',
     'skinparam defaultFontName Helvetica', 'skinparam roundCorner 4',
     'skinparam nodesep 30', 'skinparam ranksep 50',
     'skinparam ArrowFontSize 11', 'skinparam ArrowThickness 1.5',
     'skinparam PackageBorderColor #94A3B8', 'skinparam PackageFontStyle bold',
     'skinparam RectangleBorderColor #334155',
     'skinparam RectangleBackgroundColor #FFFFFF',
     'skinparam RectangleFontSize 13']


def ali(p):
    return re.sub(r'\W', '', p) or 'raiz'


def rect(p):
    partes = [f'{ficheros[p]} .java']
    if clases[p] != ficheros[p]:
        partes.append(f'{clases[p]} de nivel superior')
    if anidadas[p]:
        partes.append(f'{anidadas[p]} anidada{"s" if anidadas[p] > 1 else ""}')
    if vacios[p]:
        partes.append(f'{vacios[p]} vacio{"s" if vacios[p] > 1 else ""}')
    return (f'  rectangle "**{p}**\\n<size:10>{" · ".join(partes)}</size>"'
            f' as {ali(p)}')


L.append('package "src/main/java" as flujo #DBEAFE {')
for p in FLUJO:
    L.append(rect(p))
L.append(f'  {ali("repository")} -[hidden]right- {ali("dao")}')
L.append('}')

L.append('package "paquetes transversales" as apoyo #F3E8FF {')
for p in APOYO:
    L.append(rect(p))
for i in range(1, len(APOYO)):
    L.append(f'  {ali(APOYO[i-1])} -[hidden]right- {ali(APOYO[i])}')
L.append('}')

dibujadas = set()
for o, d in CADENA:
    if usa[(o, d)]:
        L.append(f'{ali(o)} -[#1D4ED8]down-> {ali(d)} : <<use>> {usa[(o, d)]}')
        dibujadas.add((o, d))
for o, d in SALTO:
    if usa[(o, d)]:
        L.append(f'{ali(o)} -[#C2410C]..> {ali(d)} : <<use>> {usa[(o, d)]} · salta capa')
        dibujadas.add((o, d))
# uso de los transversales: una flecha por paquete de flujo que los importa
for o in FLUJO:
    n = sum(usa[(o, d)] for d in APOYO)
    if n:
        L.append(f'{ali(o)} -[#7E22CE]..> apoyo : <<use>> {n}')
        dibujadas |= {(o, d) for d in APOYO if usa[(o, d)]}
# dependencias entre transversales
for o in APOYO:
    for d in APOYO:
        if usa[(o, d)]:
            L.append(f'{ali(o)} -[#7E22CE]..> {ali(d)} : <<use>> {usa[(o, d)]}')
            dibujadas.add((o, d))
L.append('@enduml')
open(BASE + '/puml/07_Paquetes.puml', 'w', encoding='utf-8').write('\n'.join(L) + '\n')

print('archivos por paquete:', dict(clases))
print('pares de paquetes con import:', len(usa))
sin = sorted(set(usa) - dibujadas)
print('pares medidos no dibujados:', sin or 'ninguno')
for (o, d), n in sorted(usa.items(), key=lambda x: -x[1]):
    print(f'  {o:12} -> {d:12} {n}')
