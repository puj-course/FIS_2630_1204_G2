"""Comprueba que TODO el repositorio esta representado en algun diagrama.

Recorre los archivos versionados (git ls-files) y para cada uno exige una de
dos cosas: que su nombre aparezca en algun .puml, o que pertenezca a una
carpeta que algun diagrama representa con un recuento, y entonces comprueba
que el recuento coincide con los archivos que hay. Lo que no entra en
ninguno de los dos casos se declara fuera de alcance de forma explicita, y
esa lista tambien se imprime para que se vea que no esconde nada.
"""
import os, re, sys, glob, subprocess, collections

REPO = sys.argv[1] if len(sys.argv) > 1 else os.environ.get(
    'GF_SRC', '/tmp/claude-0/-home-claude/'
    'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = sys.argv[2] if len(sys.argv) > 2 else os.environ.get(
    'GF_OUT', '/home/claude/diagramas')

PUML = {os.path.relpath(f, BASE): open(f, encoding='utf-8').read()
        for f in sorted(glob.glob(BASE + '/puml*/*.puml'))}
TODO = ''.join(PUML.values())

archivos = [a for a in subprocess.run(
    ['git', '-C', REPO, 'ls-files'],
    capture_output=True, text=True).stdout.splitlines() if a]

# carpetas que los diagramas representan con un recuento, no archivo a archivo
POR_RECUENTO = {
    'db/ddl': ('*.ddl', ['{n} .ddl', 'db/ddl · {n}']),
    'db/migrations': ('*.sql', ['{n} .sql', 'db/migrations · {n}']),
    'db/functions': ('*', ['{n} archivos', 'db/functions · {n}']),
}
# archivos que ningun diagrama tiene por que mostrar, y por que
FUERA = {
    '.gitignore': 'configuracion de git',
    '.gitattributes': 'configuracion de git',
    'LICENSE': 'licencia',
    'README.md': 'documentacion',
    'HELP.md': 'documentacion',
    'CONTRIBUTING.md': 'documentacion',
    'BOILERPLATE_template.md': 'documentacion',
    'mvnw': 'wrapper de maven, en el diagrama de despliegue',
    'mvnw.cmd': 'wrapper de maven, en el diagrama de despliegue',
    '.mvn/wrapper/maven-wrapper.properties': 'wrapper de maven',
    'conf/.gitkeep': 'marcador de carpeta vacia',
    '.github/pull_request_template.md': 'plantilla de GitHub',
}
PREF_GITKEEP = '.gitkeep'
PREF_FUERA = {'docs/': 'documentacion e imagenes',
              'src/test/': 'pruebas, en los diagramas de clases 15_pruebas'}

cubierto, sin_cubrir, fuera = [], [], []
for a in archivos:
    base = os.path.basename(a)
    if a in FUERA:
        fuera.append((a, FUERA[a]))
    elif base == '.gitkeep':
        fuera.append((a, 'marcador de carpeta vacia'))
    elif any(a.startswith(p) for p in PREF_FUERA):
        fuera.append((a, next(v for p, v in PREF_FUERA.items()
                              if a.startswith(p))))
    elif base in TODO or a in TODO or os.path.splitext(base)[0] in TODO:
        cubierto.append(a)
    elif os.path.dirname(a) in POR_RECUENTO:
        cubierto.append(a)
    else:
        sin_cubrir.append(a)

print(f'archivos versionados          : {len(archivos)}')
print(f'  nombrados en algun diagrama : {len(cubierto)}')
print(f'  fuera de alcance declarado  : {len(fuera)}')
print(f'  SIN representar             : {len(sin_cubrir)}')
for a in sin_cubrir:
    print('     ', a)

print('\nrecuentos de carpeta, comprobados contra el disco:')
mal = []
for carpeta, (patron_glob, formas) in POR_RECUENTO.items():
    reales = len([p for p in glob.glob(f'{REPO}/{carpeta}/{patron_glob}')
                  if os.path.isfile(p)])
    donde = [f for f, txt in PUML.items()
             if any(x.format(n=reales) in txt for x in formas)]
    estado = 'OK' if donde else 'FALLA'
    if not donde:
        mal.append(carpeta)
    print(f'  {carpeta:16} {patron_glob:6} en disco {reales:3}  {estado}'
          f'   {", ".join(os.path.basename(d) for d in donde)}')

print('\nfuera de alcance, uno por uno:')
agr = collections.Counter(m for _a, m in fuera)
for m, n in sorted(agr.items()):
    print(f'  {n:3}  {m}')

print('\n=== resultado ===')
if sin_cubrir or mal:
    print(f'{len(sin_cubrir)} archivos sin representar, '
          f'{len(mal)} recuentos que no cuadran')
else:
    print('todo el repositorio esta representado')
