"""Las dos vistas ligeras de la arquitectura, en una sola imagen cada una.

El diagrama de arquitectura detallado sigue existiendo y sigue llevando
todo: los archivos de cada paquete, las pruebas, el pom.xml y las flechas
de punta a punta. Estas dos no. Son la misma estructura en capas leida a
dos distancias: una con los nombres de las clases y otra sin ellos.

Las clases anidadas van con su clase contenedora entre parentesis, porque
no son archivos propios y ponerlas sueltas en la banda haria creer que si.
Nada se escribe a mano: los nombres y las capas salen de clases.json.
"""
import json
import os

BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
SAL = BASE + '/puml'

T = json.load(open(BASE + '/clases.json', encoding='utf-8'))
R = json.load(open(BASE + '/relaciones.json', encoding='utf-8'))
LL = json.load(open(BASE + '/llamadas.json', encoding='utf-8'))

# las mismas capas que el diagrama de arquitectura y el de componentes, mas
# el modelo, que en aquellos no es un componente pero aqui es la mitad de
# las clases
BANDAS = [
    ('presentacion', 'Presentación', '#DBEAFE',
     'Controladores JavaFX y arranque', ('controller', '(raiz)')),
    ('negocio', 'Negocio', '#DCFCE7',
     'Servicios y fachada', ('service',)),
    ('datos', 'Acceso a datos', '#FFEDD5',
     'Repositorios y DAO, con el SQL a mano', ('repository', 'dao')),
    ('conexion', 'Conexión', '#F1F5F9',
     'Única puerta a PostgreSQL', ('ConexionBD',)),
]
LADO = [
    ('modelo', 'Modelo del dominio', '#FEF9C3',
     'Entidades, enumeraciones, DTO y excepciones',
     ('entity', 'enums', 'dto', 'exceptions')),
    ('apoyo', 'Apoyo transversal', '#F3E8FF',
     'Estado de sesión y utilidades', ('session', 'util')),
]

POR_PAQ = {}
for t in T:
    POR_PAQ.setdefault(t['paquete'], []).append(t)


# hay dos tipos llamados EstadoMesa, uno en entity y otro en enums. En una
# lista de nombres sueltos saldrian dos veces iguales y pareceria un error
# del dibujo, asi que a los nombres repetidos se les pone el paquete delante
AMBIGUOS = {t['nombre'] for t in T
            if sum(1 for x in T if x['nombre'] == t['nombre']) > 1}


def como_se_llama(t):
    return f"{t['paquete']}.{t['nombre']}" if t['nombre'] in AMBIGUOS \
        else t['nombre']


def tipos_de(paquetes):
    return [t for p in paquetes for t in POR_PAQ.get(p, [])]


def nombres(paquetes):
    """Las entradas de esa banda, con la anidada detras de su contenedora."""
    sueltas, dentro = [], {}
    for t in tipos_de(paquetes):
        if t['anidada_en']:
            dentro.setdefault(t['anidada_en'], []).append(como_se_llama(t))
        else:
            sueltas.append(como_se_llama(t))
    # se ordena por el nombre de la clase, no por el texto: si no, las dos
    # que llevan el paquete delante se irian al final de la lista
    def clave(x):
        return x.rsplit('.', 1)[-1].lower()

    out = []
    for n in sorted(sueltas, key=clave):
        hijas = sorted(dentro.pop(n.rsplit('.', 1)[-1], []), key=clave)
        out.append(f'{n} ({", ".join(hijas)})' if hijas else n)
    # una anidada cuya contenedora no esta en la banda no se pierde
    for madre, hijas in sorted(dentro.items()):
        out += [f'{h} (en {madre})' for h in sorted(hijas, key=clave)]
    return out


BANDA_DE_PAQ = {p: alias for alias, _t, _c, _pie, paqs in BANDAS + LADO
                for p in paqs}
PAQ_DE_NOMBRE = {}
for t in T:
    PAQ_DE_NOMBRE.setdefault(t['nombre'], set()).add(t['paquete'])


def banda_de(nombre):
    ps = PAQ_DE_NOMBRE.get(nombre)
    # un nombre que existe en dos paquetes no se puede asignar a ciegas
    return BANDA_DE_PAQ.get(next(iter(ps))) if ps and len(ps) == 1 else None


def pares_de(R):
    """Las relaciones como (origen, destino), vengan de la lista que vengan.

    Cada lista de relaciones.json tiene sus propias claves: asociacion y
    dependencia usan origen/destino, generalizacion usa hijo/padre y
    realizacion usa clase/interfaz. Tratarlas todas como origen/destino
    reventaba en cuanto aparecio la primera realizacion del proyecto.
    """
    out = []
    for r in R.get('asociacion', []) + R.get('dependencia', []):
        out.append((r['origen'], r['destino']))
    for r in R.get('generalizacion', []):
        out.append((r['hijo'], r['padre']))
    for r in R.get('realizacion', []):
        out.append((r['clase'], r['interfaz']))
    return out


def dependencias():
    """Que banda depende de cual, contando relaciones y llamadas reales."""
    import collections
    pares = collections.Counter()
    for o, d in pares_de(R):
        a, b = banda_de(o), banda_de(d)
        if a and b and a != b:
            pares[(a, b)] += 1
    for l in LL['llamadas']:
        a, b = banda_de(l['origen']), banda_de(l['destino'])
        if a and b and a != b:
            pares[(a, b)] += 1
    return pares


COLUMNA = [x[0] for x in BANDAS]


def flechas():
    """Todas las dependencias que existen, no solo las del camino bonito.

    Las que se saltan una capa van en rojo y dicen que la saltan, igual que
    en el diagrama de arquitectura: si no, estas dos vistas pintarian una
    cascada limpia que el codigo no cumple.
    """
    out, pares = [], dependencias()
    # modelo arriba a la derecha: deja la columna de capas casi recta
    out.append('modelo -[hidden]down- apoyo')
    for (a, b), n in sorted(pares.items(), key=lambda x: (-x[1], x[0])):
        salta = (a in COLUMNA and b in COLUMNA
                 and COLUMNA.index(b) - COLUMNA.index(a) > 1)
        if a in COLUMNA and b in COLUMNA and not salta:
            out.append(f'{a} -down-> {b}')
        elif salta:
            out.append(f'{a} -[#B91C1C]right-> {b} : <color:#B91C1C>salta capa')
        else:
            out.append(f'{a} -right-> {b}')
    return out


def en_lineas(lista, ancho=62):
    """Reparte los nombres en lineas para que la caja no salga kilometrica."""
    lineas, actual = [], ''
    for n in lista:
        if actual and len(actual) + len(n) + 3 > ancho:
            lineas.append(actual)
            actual = n
        else:
            actual = f'{actual} · {n}' if actual else n
    if actual:
        lineas.append(actual)
    return lineas


def caja(alias, titulo, color, pie, paquetes):
    ns = nombres(paquetes)
    cuerpo = '\\n'.join(f'<size:11>{l}</size>' for l in en_lineas(ns))
    return (f'rectangle "<size:16><b>{titulo}</b></size>  '
            f'<size:11>· {len(tipos_de(paquetes))}</size>\\n'
            f'<size:11><i>{pie}</i></size>\\n\\n{cuerpo}" as {alias} {color}')


L = ['@startuml',
     'skinparam backgroundColor #FFFFFF',
     'skinparam shadowing false',
     'skinparam defaultFontName Helvetica',
     'skinparam roundCorner 10',
     'skinparam nodesep 18',
     'skinparam ranksep 26',
     'skinparam ArrowColor #F59E0B',
     'skinparam ArrowThickness 2.2',
     'skinparam RectangleBorderColor #94A3B8',
     'skinparam RectangleFontSize 13',
     'skinparam RectangleBorderThickness 1.2',
     'skinparam padding 4']
for alias, titulo, color, pie, paquetes in BANDAS:
    L.append(caja(alias, titulo, color, pie, paquetes))
for alias, titulo, color, pie, paquetes in LADO:
    L.append(caja(alias, titulo, color, pie, paquetes))
# La pila baja en linea recta y las dos cajas transversales van las dos al
# mismo lado: si una tira hacia la izquierda y otra hacia la derecha,
# graphviz dobla la columna y el dibujo deja de leerse de arriba abajo.
L += flechas()
L.append('@enduml')
open(f'{SAL}/01_Arquitectura_Mapa.puml', 'w', encoding='utf-8').write(
    '\n'.join(L) + '\n')

# ======================================================= version corta ===
# La misma estructura pero sin un solo nombre de clase: cada banda dice de
# que paquetes se compone y cuantos tipos trae cada uno. Es para mirarla de
# lejos y saber el tamano de cada parte; para saber que clase esta donde
# esta el mapa de al lado.
def resumen(paquetes):
    trozos = []
    for p in paquetes:
        n = len(POR_PAQ.get(p, []))
        if n:
            trozos.append(f'{p} · {n}')
    return '   '.join(trozos)


C = ['@startuml',
     'skinparam backgroundColor #FFFFFF',
     'skinparam shadowing false',
     'skinparam defaultFontName Helvetica',
     'skinparam roundCorner 10',
     'skinparam nodesep 16',
     'skinparam ranksep 24',
     'skinparam ArrowColor #F59E0B',
     'skinparam ArrowThickness 2.2',
     'skinparam RectangleBorderColor #94A3B8',
     'skinparam RectangleBorderThickness 1.2',
     'skinparam padding 4']
for alias, titulo, color, pie, paquetes in BANDAS + LADO:
    n_t = len(tipos_de(paquetes))
    C.append(f'rectangle "<size:17><b>{titulo}</b></size>  '
             f'<size:12>· {n_t} tipo{"s" if n_t != 1 else ""}</size>\\n'
             f'<size:12><i>{pie}</i></size>\\n'
             f'<size:12>{resumen(paquetes)}</size>" as {alias} {color}')
C += flechas()
C.append('@enduml')
open(f'{SAL}/01_Arquitectura_Resumen.puml', 'w', encoding='utf-8').write(
    '\n'.join(C) + '\n')

total = sum(len(tipos_de(p)) for *_x, p in BANDAS + LADO)
print(f'arquitectura ligera: {total} tipos de los {len(T)} de clases.json')
for alias, titulo, _c, _p, paquetes in BANDAS + LADO:
    print(f'  {titulo:22} {len(tipos_de(paquetes)):3}  '
          f'({len(nombres(paquetes))} entradas)')
