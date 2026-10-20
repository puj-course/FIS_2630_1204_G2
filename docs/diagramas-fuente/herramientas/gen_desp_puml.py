"""Diagrama de despliegue de GastroFlow en PlantUML.

Un solo equipo ejecuta la aplicacion y la base de datos, porque
ConexionBD apunta a localhost. GitHub no es un nodo de despliegue: de ahi
se descarga el codigo y el runner solo comprueba que compile, asi que se
dibuja como una caja plana aparte y la instalacion ocurre entera en el equipo.
Todos los datos salen del repositorio.
"""
import os, re, glob, json

REPO = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                      'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
E = json.load(open(BASE + '/esquema.json', encoding='utf-8'))

pom = open(REPO + '/pom.xml', encoding='utf-8').read()
art = re.search(r'<artifactId>(.*?)</artifactId>', pom).group(1)
ver = re.search(r'<version>(.*?)</version>', pom).group(1)
props = dict(re.findall(r'<(\w[\w.]*)>([^<>]+)</\1>', pom))
ci = open(REPO + '/.github/workflows/ci.yml', encoding='utf-8').read()

# dependencias de compilacion, con las propiedades ya resueltas
libs = []
for m in re.finditer(r'<dependency>(.*?)</dependency>', pom, re.S):
    b = m.group(1)
    if '<scope>test</scope>' in b:
        continue
    a_ = re.search(r'<artifactId>(.*?)</', b).group(1)
    v_ = re.search(r'<version>(.*?)</', b)
    v_ = v_.group(1) if v_ else ''
    mm = re.fullmatch(r'\$\{(.+?)\}', v_)
    if mm:
        v_ = props.get(mm.group(1), v_)
    libs.append(f'{a_} {v_}')

# todo lo que src/main/resources mete en target/classes al compilar
RES = REPO + '/src/main/resources'
recursos = sorted(os.path.relpath(p, RES) for p in
                  glob.glob(RES + '/**/*', recursive=True)
                  if os.path.isfile(p) and not p.endswith('.gitkeep'))
por_carpeta = {}
for r in recursos:
    por_carpeta.setdefault(os.path.dirname(r) or '(raiz)', []).append(r)


def lista(patron, texto):
    return sorted(set(re.findall(patron, texto)))


usos = lista(r'uses:\s*(\S+)', ci)
pasos_ci = [n.strip() for n in re.findall(r'-\s*name:\s*(.+)', ci)]
imagen = re.search(r'image:\s*(\S+)', ci)
puertos = lista(r'-\s*(\d+:\d+)', ci)
disparo_push = re.search(r'push:\s*\n\s*branches:\s*((?:\s*-\s*\S+\n)+)', ci)
disparo_pr = re.search(r'pull_request:\s*\n\s*branches:\s*((?:\s*-\s*\S+\n)+)', ci)


def ramas(m):
    return ', '.join(re.findall(r'-\s*(\S+)', m.group(1))) if m else '?'


runner = re.search(r'runs-on:\s*(\S+)', ci).group(1)
java_v = re.search(r"java-version:\s*'?([\d.]+)'?", ci).group(1)
distro = re.search(r'distribution:\s*(\S+)', ci).group(1)

views = len(glob.glob(REPO + '/src/main/resources/views/*'))
csss = len(glob.glob(REPO + '/src/main/resources/css/*'))
ddl = len(glob.glob(REPO + '/db/ddl/*.ddl'))
mig = len(glob.glob(REPO + '/db/migrations/*.sql'))
fun = len(glob.glob(REPO + '/db/functions/*'))
url = re.search(r'URL\s*=\s*"([^"]+)"',
                open(REPO + '/src/main/java/ConexionBD/ConexionBD.java',
                     encoding='utf-8').read()).group(1)
mainclass = re.search(r'<mainClass>(.*?)</mainClass>', pom).group(1)


# como se arranca: lo dice el README, y el pom no trae ningun plugin de
# empaquetado, asi que no hay artefacto que instalar en otra maquina
readme = open(REPO + '/README.md', encoding='utf-8').read()
m = re.search(r'## Ejecuci[^\n]*\n+```bash\n(.*?)```', readme, re.S)
comandos = [c.strip() for c in m.group(1).strip().split('\n')] if m else []
plugins = re.findall(r'<artifactId>(maven-[\w-]+|javafx-maven-plugin)</artifactId>', pom)
empaqueta = [x for x in ('maven-shade-plugin', 'maven-assembly-plugin',
                         'maven-jlink-plugin', 'jpackage-maven-plugin',
                         'spring-boot-maven-plugin') if x in pom]
cbd = open(REPO + '/src/main/java/ConexionBD/ConexionBD.java', encoding='utf-8').read()
consts = re.findall(r'static final String (\w+)\s*=\s*"([^"]*)"', cbd)
usa_env = bool(re.search(r'System\.getenv', cbd))

L = ['@startuml',

     'skinparam backgroundColor #FFFFFF', 'skinparam shadowing false',
     'skinparam defaultFontName Helvetica', 'skinparam roundCorner 4',
     'skinparam nodesep 40', 'skinparam ranksep 70', 'skinparam padding 5',
     'skinparam ArrowColor #1E293B', 'skinparam ArrowThickness 1.6',
     'skinparam ArrowFontSize 11', 'skinparam ArrowFontStyle bold',
     'skinparam NodeFontSize 14', 'skinparam NodeFontStyle bold',
     'skinparam ArtifactFontSize 12',
     'skinparam NodeBackgroundColor #FFFFFF',
     'skinparam ArtifactBackgroundColor #FFFFFF']

# requisitos, tal como los lista el README
req = re.findall(r'## Requisitos\n+((?:- [^\n]+\n)+)', readme)
requisitos = [r.strip('- ').strip() for r in req[0].strip().split('\n')] if req else []
jdk = next((r for r in requisitos if 'JDK' in r), f'JDK {props.get("maven.compiler.release", "?")}')
mvn_req = next((r for r in requisitos if 'Maven' in r), 'Maven')
pg_req = next((r for r in requisitos if 'PostgreSQL' in r), 'PostgreSQL')
# las opciones del script, de su propio case. Cada una va en su linea: en
# PlantUML, --texto-- dentro de una misma linea se dibuja tachado.
setup_src = open(REPO + '/scripts/setup.sh', encoding='utf-8').read()
opciones = sorted(set(re.findall(r'^\s*(--[a-z-]+)(?:\||\))', setup_src, re.M)))

ci_art = ('.github/workflows/ci.yml\\n'
          + '\\n'.join(usos)
          + f'\\njava-version {java_v} · {distro}'
          + f'\\non: push → {ramas(disparo_push)}'
          + f'\\non: pull_request → {ramas(disparo_pr)}')
pasos = 'steps\\n' + '\\n'.join(f'{i+1}. {n}' for i, n in enumerate(pasos_ci))
L += [
    'rectangle "GitHub" as github <<servicio externo>> #FFEDD5 {',
    '  node "Repositorio remoto" as repo <<execution environment>> #FFFFFF {',
    '    artifact "FIS_2630_1204_G2\\nmain · develop · features-*" as a_repo',
    '  }',
    f'  node "Runner {runner}" as runner <<execution environment>> #FFFFFF {{',
    f'    artifact "{ci_art}" as a_ci',
    f'    artifact "{pasos}" as a_pasos',
    f'    artifact "services: postgres\\n{imagen.group(1) if imagen else "?"}'
    f'\\nports {", ".join(puertos)}" as a_pg_ci',
    '    a_ci -[hidden]right- a_pasos',
    '    a_pasos -[hidden]right- a_pg_ci',
    '  }',
    '  repo -down-> runner : on: push · pull_request',
    '}',
]


L += [
    'node "Equipo" as equipo <<device>> #DBEAFE {',
    # el clon y sus archivos viven en el disco, no dentro de la JVM
    '  artifact "FIS_2630_1204_G2\\nmain · develop · features-*" as a_src',
    f'  artifact "pom.xml\\n' + '\\n'.join(sorted(set(plugins)))
    + ('\\n' + ' · '.join(empaqueta) if empaqueta
       else '\\nningun plugin de empaquetado') + '" as a_pom',
    '  artifact "mvnw · mvnw.cmd\\n.mvn/wrapper" as a_mvn',
    f'  artifact "db/ddl · {ddl}\\ndb/migrations · {mig}'
    f'\\ndb/functions · {fun}" as a_ddl',
    f'  artifact "scripts/setup.sh\\n' + '\\n'.join(opciones) + '" as a_setup',
    f'  node "{jdk} · {mvn_req}" as jvm <<execution environment>> #FFFFFF {{',
    f'    artifact "target/classes\\n{art} {ver}\\n'
    + '\\n'.join(f'{c} · {len(v)}' if c != '(raiz)' else ' · '.join(v)
                 for c, v in sorted(por_carpeta.items())) + '" as a_jar',
    f'    artifact "' + '\\n'.join(libs[:2]) + '" as a_fx',
    f'    artifact "' + '\\n'.join(libs[2:]) + '" as a_libs',
    f'    artifact "ConexionBD.ConexionBD\\n'
    + '\\n'.join(f'{k} = {v}' for k, v in consts)
    + ('\\nSystem.getenv' if usa_env else '') + '" as a_conf',
    '    artifact "' + '\\n'.join(comandos) + '" as a_cmd',
    '    a_jar -[hidden]right- a_fx',
    '    a_fx -[hidden]right- a_libs',
    '    a_libs -[hidden]right- a_conf',
    '    a_conf -[hidden]right- a_cmd',
    '  }',
    f'  node "{pg_req}" as pg <<execution environment>> #FFFFFF {{',
    f'    artifact "gastroflow · {len(E)} tablas" as a_db',
    '  }',
    '  a_src -[hidden]right- a_pom',
    '  a_pom -[hidden]right- a_mvn',
    '  a_mvn -[hidden]right- a_ddl',
    '  a_ddl -[hidden]right- a_setup',
    f'  a_pom ..> a_jar : <<build>> {comandos[0] if comandos else "mvn"}',
    f'  a_ddl ..> a_setup : <<entrada>>',
    '  a_setup ..> a_db : <<crea>> psql',
    f'  jvm -down-> pg : {url}',
    '}',
    # Una sola flecha cruza entre GitHub y el equipo, y apunta al equipo:
    # el codigo se descarga y se instala alli. De GitHub no sale ningun
    # binario; el runner solo verifica que compile y que pasen las pruebas.
    # La flecha de subida se quito porque competia con esta y confundia la
    # direccion; que el equipo empuja a GitHub ya lo dice ci.yml con sus
    # disparadores on: push y on: pull_request.
    'a_repo ..> a_src : <<descarga>> git clone',
    '@enduml',
]

open(BASE + '/puml/06_Despliegue.puml', 'w', encoding='utf-8').write('\n'.join(L) + '\n')
print('ci uses:', usos)
print('ci steps:', pasos_ci)
print(f'tablas {len(E)} · ddl {ddl} · migrations {mig} · functions {fun} · '
      f'views {views} · css {csss}')
