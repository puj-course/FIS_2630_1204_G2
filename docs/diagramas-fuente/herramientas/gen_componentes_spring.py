"""Diagrama 08: la parte Spring Boot / JPA que vive en el repositorio pero
no entra en la compilacion.

Seis clases importan org.springframework o jakarta.persistence. El pom.xml
no declara ninguna de esas dos librerias, asi que no compilarian, y por eso
el maven-compiler-plugin las excluye. Junto a ellas hay un
application.properties y un static/login.html que tampoco usa la aplicacion
JavaFX. Todo lo que dice este diagrama sale de esos archivos.
"""
import json, os, re, glob

REPO = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                      'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
DIR = BASE + '/puml_componentes'
T = json.load(open(BASE + '/clases.json', encoding='utf-8'))
POR = {t['nombre']: t for t in T}
SRC = REPO + '/src/main/java'

pom = open(REPO + '/pom.xml', encoding='utf-8').read()
EXCL = [l.strip() for l in re.findall(r'<exclude>(.+?)</exclude>', pom)]
dependencias = re.findall(r'<artifactId>(.*?)</artifactId>', pom)

# que clases importan Spring o Jakarta
con_spring = {}
for ruta in sorted(glob.glob(SRC + '/**/*.java', recursive=True)):
    if os.path.getsize(ruta) == 0:
        continue
    txt = open(ruta, encoding='utf-8').read()
    imps = re.findall(r'^import\s+((?:org\.springframework|jakarta)[\w.*]+)\s*;',
                      txt, re.M)
    if imps:
        rel = os.path.relpath(ruta, SRC)
        anot = sorted(set(re.findall(r'^\s*(@\w+)', txt, re.M)))
        con_spring[rel] = (sorted(set(imps)), anot)

vacios = sorted(os.path.relpath(p, REPO)
                for p in glob.glob(REPO + '/src/**/*.java', recursive=True)
                if os.path.getsize(p) == 0)

props = {}
ruta_props = REPO + '/src/main/resources/application.properties'
for linea in open(ruta_props, encoding='utf-8'):
    if '=' in linea and not linea.startswith('#'):
        k, v = linea.split('=', 1)
        props[k.strip()] = v.strip()

html = REPO + '/src/main/resources/static/login.html'
n_html = sum(1 for _ in open(html, encoding='utf-8'))

CAB = open(BASE + '/gen_componentes_puml.py', encoding='utf-8')
CAB.close()

L = ['@startuml', 'allowmixing',
     'skinparam backgroundColor #FFFFFF', 'skinparam shadowing false',
     'skinparam defaultFontName Helvetica', 'skinparam roundCorner 4',
     'skinparam nodesep 30', 'skinparam ranksep 55',
     'skinparam ArrowColor #475569', 'skinparam ArrowFontSize 10',
     'skinparam PackageBorderColor #94A3B8', 'skinparam PackageFontStyle bold',
     'skinparam ComponentBackgroundColor #FFFFFF',
     'skinparam ArtifactBackgroundColor #FFFFFF',
     'skinparam DatabaseBackgroundColor #FFFFFF']


def ali(s):
    return re.sub(r'\W', '_', s)


def lin(xs):
    return ''.join(f'\\n<size:9>{x}</size>' for x in xs)


L.append(f'package "clases que importan Spring o Jakarta · {len(con_spring)}"'
         f' #FEF2F2 {{')
for rel, (imps, anot) in sorted(con_spring.items()):
    nombre = os.path.basename(rel).replace('.java', '')
    detalle = [i.replace('org.springframework.', '').replace('jakarta.', '')
               for i in imps]
    if POR.get(nombre, {}).get('extends'):
        detalle.append('extends ' + POR[nombre]['extends'])
    detalle += [a for a in anot if a in ('@Entity', '@Table', '@Service',
                                         '@Transactional')]
    L.append(f'  component "**{nombre}**{lin(detalle)}" as {ali(nombre)}')
nm = [os.path.basename(r).replace('.java', '') for r in sorted(con_spring)]
for i in range(1, len(nm)):
    if i % 3:
        L.append(f'  {ali(nm[i-1])} -[hidden]right- {ali(nm[i])}')
    else:
        L.append(f'  {ali(nm[i-3])} -[hidden]down- {ali(nm[i])}')
L.append('}')

L.append('package "configuracion y recursos que la aplicacion JavaFX no usa"'
         ' #FEF9C3 {')
L.append(f'  artifact "src/main/resources/application.properties'
         f'{lin([k + "=" + v for k, v in props.items()])}" as A_props')
L.append(f'  artifact "src/main/resources/static/login.html'
         f'{lin([str(n_html) + " lineas"])}" as A_html')
L.append('  A_props -[hidden]right- A_html')
L.append('}')

L.append(f'package "archivos .java de 0 bytes · {len(vacios)}" #F1F5F9 {{')
for v in vacios:
    L.append(f'  artifact "{v}" as {ali(v)}')
for i in range(1, len(vacios)):
    L.append(f'  {ali(vacios[i-1])} -[hidden]right- {ali(vacios[i])}')
L.append('}')

# conf/ y scripts/ traen archivos de 1 byte: solo un salto de linea
otros = sorted(os.path.relpath(p, REPO) for p in
               glob.glob(REPO + '/conf/*') + glob.glob(REPO + '/scripts/*')
               if os.path.getsize(p) == 1 and not p.endswith('.gitkeep'))
L.append(f'package "archivos de 1 byte, solo un salto de linea · {len(otros)}"'
         f' #F1F5F9 {{')
for o in otros:
    L.append(f'  artifact "{o}" as {ali(o)}')
for i in range(1, len(otros)):
    L.append(f'  {ali(otros[i-1])} -[hidden]right- {ali(otros[i])}')
L.append('}')

L.append(f'artifact "pom.xml{lin(["maven-compiler-plugin excluye " + str(len(EXCL)) + " archivos", "ninguna dependencia de Spring ni de Jakarta"])}" as A_pom')
for rel in sorted(con_spring):
    nombre = os.path.basename(rel).replace('.java', '')
    if rel in EXCL:
        L.append(f'A_pom ..> {ali(nombre)} : <<exclude>>')
vacio_excluido = [e for e in EXCL if e not in con_spring]
for e in vacio_excluido:
    n = os.path.basename(e).replace('.java', '')
    L.append(f'A_pom ..> {ali("src/main/java/" + e)} : <<exclude>>')
L.append('@enduml')
open(f'{DIR}/08_Componentes_spring_sin_compilar.puml', 'w',
     encoding='utf-8').write('\n'.join(L) + '\n')

print(f'clases con Spring o Jakarta: {len(con_spring)}')
for r, (i, _a) in sorted(con_spring.items()):
    print(f'   {r:42} {", ".join(i)}')
print(f'excluidas en pom.xml: {len(EXCL)}')
print(f'dependencias de Spring en pom.xml: '
      f'{[d for d in dependencias if "spring" in d.lower()] or "ninguna"}')
print(f'archivos .java vacios: {len(vacios)} -> {vacios}')
print(f'archivos de 1 byte: {len(otros)} -> {otros}')
print(f'application.properties: {len(props)} propiedades')
print(f'static/login.html: {n_html} lineas')
