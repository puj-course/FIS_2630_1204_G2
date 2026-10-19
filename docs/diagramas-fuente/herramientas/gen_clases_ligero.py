"""Un mapa ligero de todo el modelo de clases, en una sola imagen.

Los quince diagramas de clases siguen existiendo y siguen llevando los
atributos y los metodos. Este no: es una banda por capa con los nombres de
sus clases y nada mas, para saber de un vistazo que hay y donde vive cada
cosa antes de entrar al detalle.

Las clases anidadas van con su clase contenedora entre parentesis, porque
no son archivos propios y ponerlas sueltas en la banda haria creer que si.
Nada se escribe a mano: los nombres y las capas salen de clases.json.
"""
import json
import os

BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
SAL = BASE + '/puml_clases'

T = json.load(open(BASE + '/clases.json', encoding='utf-8'))

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
for a, b in zip([x[0] for x in BANDAS], [x[0] for x in BANDAS][1:]):
    L.append(f'{a} -down-> {b}')
# modelo primero: es la caja grande y, puesta arriba a la derecha, deja
# la columna de capas casi recta
L.append('modelo -[hidden]down- apoyo')
L += ['presentacion -right-> apoyo',
      'negocio -right-> apoyo',
      'presentacion -right-> modelo',
      'negocio -right-> modelo',
      'datos -right-> modelo',
      '@enduml']
open(f'{SAL}/00_Mapa_Clases.puml', 'w', encoding='utf-8').write(
    '\n'.join(L) + '\n')

total = sum(len(tipos_de(p)) for *_x, p in BANDAS + LADO)
print(f'mapa ligero: {total} tipos de los {len(T)} de clases.json')
for alias, titulo, _c, _p, paquetes in BANDAS + LADO:
    print(f'  {titulo:22} {len(tipos_de(paquetes)):3}  '
          f'({len(nombres(paquetes))} entradas)')
