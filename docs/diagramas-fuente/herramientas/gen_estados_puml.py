"""HU-129: diagrama de estados del pedido y de la mesa.

Todo lo que se dibuja sale del codigo, no se escribe a mano:

  pedido
    - los estados, del enum EstadoPedido
    - los finales, del conjunto ESTADOS_YA_CERRADOS de PedidoService
    - cada transicion, de un setEstado(EstadoPedido.X) con el metodo que lo
      ejecuta; el origen sale de la guarda que protege ese metodo, no de una
      suposicion
    - que estados llegan al tablero de cocina, de findPedidosParaCocina()
    - desde que estados hay boton, de la guarda de crearTarjeta()

  mesa
    - los estados, del catalogo que siembra
      db/functions/generar_estado_mesa_prueba.ddl
    - cada transicion, de una llamada a MesaService.cambiarEstado(...) con su
      motivo; ninguna mira el estado actual de la mesa, asi que el origen se
      dibuja como "cualquier estado del catalogo", que es lo que dice el codigo
    - los codigos que la interfaz reconoce, del enum EstadoMesaVisual
    - el camino que se salta MesaService, de MapaSalonController.liberarMesa()

Marca en rojo los codigos que el codigo usa y el catalogo no tiene, y en gris
los que la interfaz pinta pero nadie escribe.
"""
import json, os, re, glob

REPO = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                      'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
SRC = REPO + '/src/main/java'
T = json.load(open(BASE + '/clases.json', encoding='utf-8'))
POR = {t['nombre']: t for t in T}


def limpio(ruta):
    """El archivo sin comentarios, para que un ejemplo comentado no cuente."""
    t = open(ruta, encoding='utf-8').read()
    return re.sub(r'/\*.*?\*/|//[^\n]*', '', t, flags=re.S)


def metodo_en(txt, pos):
    """Nombre del metodo que contiene la posicion pos."""
    cab = list(re.finditer(r'^\s*(?:public|private|protected)[^\n;{]*?(\w+)\s*\(',
                           txt[:pos], re.M))
    return cab[-1].group(1) if cab else '?'


def cuerpo_metodo(txt, nombre):
    """El texto del metodo nombre, contando llaves."""
    patron = (r'^\s*(?:public|private|protected)[^\n;{]*?\b' + nombre
              + r'\s*\([^)]*\)[^{;]*\{')
    m = re.search(patron, txt, re.M | re.S)
    if not m:
        return ''
    i, prof = m.end() - 1, 0
    while i < len(txt):
        if txt[i] == '{':
            prof += 1
        elif txt[i] == '}':
            prof -= 1
            if prof == 0:
                return txt[m.start():i + 1]
        i += 1
    return txt[m.start():]


FUENTES = {os.path.basename(r).replace('.java', ''): limpio(r)
           for r in sorted(glob.glob(SRC + '/**/*.java', recursive=True))
           if os.path.getsize(r) > 0}

def guardas(clase, metodo):
    """Las condiciones que impiden la transicion, sacadas del codigo: cada
    throw con el texto de su propio mensaje, y las guardas que viajan en el
    SQL. No se escribe ninguna a mano."""
    cu = cuerpo_metodo(FUENTES[clase], metodo)
    out = []
    for m in re.finditer(r'throw new (\w+)\(([^;]*?)\)\s*;', cu, re.S):
        lits = re.findall(r'"([^"]*)"', m.group(2))
        if not lits:
            continue
        # "La mesa " + id + " ya tiene..." -> "La mesa ... ya tiene..."
        txt = ' ... '.join(x.strip() for x in lits if x.strip())
        out.append(txt.rstrip('.'))
    exige = [f'la cantidad cabe en {m.group(1)}'
             for m in re.finditer(r'AND\s+\?\s*<=\s*(\w+)', cu)]
    return out, exige


def con_guardas(clase, metodo, base=''):
    falla, exige = guardas(clase, metodo)
    return '; '.join(([base] if base else [])
                     + ['falla si: ' + x for x in falla]
                     + ['exige: ' + x for x in exige])


# =========================================================== el pedido =====
EST_PEDIDO = POR['EstadoPedido']['literales']
ps = FUENTES['PedidoService']

cerrados = re.search(r'ESTADOS_YA_CERRADOS\s*=\s*Set\.of\(([^)]*)\)', ps, re.S)
FINALES = re.findall(r'EstadoPedido\.(\w+)', cerrados.group(1)) if cerrados else []
ACTIVOS = [e for e in EST_PEDIDO if e not in FINALES]

# cada setEstado(EstadoPedido.X) con la clase y el metodo que lo hace
asigna = []
for clase, txt in FUENTES.items():
    for m in re.finditer(r'setEstado\(\s*EstadoPedido\.(\w+)\s*\)', txt):
        asigna.append({'estado': m.group(1), 'clase': clase,
                       'metodo': metodo_en(txt, m.start())})
ASIGNADOS = {a['estado'] for a in asigna}

# el metodo que protege su entrada con ESTADOS_YA_CERRADOS puede venir de
# cualquier estado activo: esa es la guarda, no una suposicion mia
GUARDADOS = {a['metodo'] for a in asigna
             if 'ESTADOS_YA_CERRADOS.contains' in cuerpo_metodo(ps, a['metodo'])}

# el tablero de cocina solo carga estos estados
pr = FUENTES['PedidoRepository']
coc = cuerpo_metodo(pr, 'findPedidosParaCocina')
EN_COCINA = re.findall(r'EstadoPedido\.(\w+)\.name\(\)', coc)

# El tablero decide dos cosas: si la tarjeta lleva boton y a que estado pasa
# al pulsarlo. Hasta el patron Adapter las dos reglas estaban en
# CocinaController; ahora viven en PedidoComandaAdapter. Se buscan donde
# esten, y si no aparecen se para en vez de seguir con None: un diagrama de
# estados al que le faltan las transiciones de cocina es peor que ninguno.
def buscar_regla(patron, candidatos, que):
    for clase, metodo in candidatos:
        if clase not in FUENTES:
            continue
        txt = cuerpo_metodo(FUENTES[clase], metodo) if metodo \
            else FUENTES[clase]
        m_ = re.search(patron, txt, re.S)
        if m_:
            return m_, f'{clase}.{metodo}()' if metodo else clase
    raise SystemExit(
        f'No se encontro {que} en ' + ', '.join(c for c, _ in candidatos)
        + '. La regla se movio de sitio en el codigo: hay que actualizar '
          'gen_estados_puml.py en lugar de dibujar un diagrama incompleto.')


sin_boton, _ = buscar_regla(
    r'getEstado\(\)\s*!=\s*EstadoPedido\.(\w+)',
    [('PedidoComandaAdapter', 'puedeAvanzar'), ('CocinaController', None)],
    'la regla de que tarjeta lleva boton')
SIN_BOTON = sin_boton.group(1)
CON_BOTON = [e for e in EN_COCINA if e != SIN_BOTON]

# el if que reparte el destino al pulsar el boton
m, ORIGEN_AVANCE = buscar_regla(
    r'if\s*\(\s*\w+\.getEstado\(\)\s*==\s*EstadoPedido\.(\w+)\s*\)\s*\{'
    r'\s*\w+\.setEstado\(EstadoPedido\.(\w+)\);\s*\}\s*else\s*\{'
    r'\s*\w+\.setEstado\(EstadoPedido\.(\w+)\);',
    [('PedidoComandaAdapter', 'avanzar'),
     ('CocinaController', 'avanzarEstado')],
    'el reparto de estados del tablero de cocina')
COCINA_SI = (m.group(1), m.group(2))    # EN_PREPARACION -> COMPLETADO
COCINA_NO = m.group(3)                  # el resto -> EN_PREPARACION

# Quien escribe el estado sin pasar por PedidoService se salta la guarda
# ESTADOS_YA_CERRADOS: eso merece la misma marca que en el diagrama de mesa.
# Desde el patron Adapter, cambiar el estado y guardarlo ya no ocurren en la
# misma clase: PedidoComandaAdapter.avanzar() lo cambia y CocinaController
# hace el save con lo que devuelve. Por eso se mira tambien un salto mas
# alla, en el metodo de quien llama. Sin esto el salto de capa dejaria de
# marcarse en azul y el diagrama escondería un incumplimiento que sigue ahi.
def guarda_el_llamador(clase, metodo):
    for otra, txt in FUENTES.items():
        if otra == clase:
            continue
        for m_ in re.finditer(rf'\.{re.escape(metodo)}\s*\(', txt):
            if 'pedidoRepository.save(' in cuerpo_metodo(
                    txt, metodo_en(txt, m_.start())):
                return True
    return False


for a in asigna:
    cu = cuerpo_metodo(FUENTES[a['clase']], a['metodo'])
    a['sin_servicio'] = (a['clase'] != 'PedidoService'
                         and ('pedidoRepository.save(' in cu
                              or guarda_el_llamador(a['clase'], a['metodo'])))

INICIAL = next((a for a in asigna if a['metodo'] == 'crearPedido'), None)

# ============================================================= la mesa =====
semilla = open(REPO + '/db/functions/generar_estado_mesa_prueba.ddl',
               encoding='utf-8').read()
CAT_MESA = re.findall(r"\('([A-Z_]+)',", semilla)

# MesaService.cambiarEstado busca el codigo antes de tocar la base
ms = FUENTES['MesaService']
ce = cuerpo_metodo(ms, 'cambiarEstado')
BUSCA_ANTES = 'findByCodigoEstado' in ce and 'orElseThrow' in ce
EXC_BUSCA = re.search(r'orElseThrow\(\(\)\s*->\s*new (\w+)', ce)
EXC_BUSCA = EXC_BUSCA.group(1) if EXC_BUSCA else 'excepcion'

# cada llamada a cambiarEstado(..., destino, "MOTIVO")
trans = []
for clase, txt in FUENTES.items():
    if clase == 'MesaService':
        continue
    for m in re.finditer(r'cambiarEstado\(\s*([^;]*?)\)\s*;', txt, re.S):
        args, metodo = m.group(1), metodo_en(txt, m.start())
        lits = re.findall(r'"([A-Z_]+)"', args)
        if len(lits) == 2:                       # destino literal
            trans.append({'destino': lits[0], 'motivo': lits[1], 'clase': clase,
                          'metodo': metodo, 'origen': 'literal'})
            continue
        var = re.search(r',\s*(\w+)\s*,\s*"([A-Z_]+)"', args)
        if not var:
            continue
        nombre_var = var.group(1)
        # si la variable sale del dialogo, el destino lo elige el usuario y la
        # lista de opciones ya se dibuja aparte
        if re.search(rf'{nombre_var}\s*=\s*resultado\.get\(\)', txt[:m.start()]):
            trans.append({'destino': None, 'motivo': var.group(2), 'clase': clase,
                          'metodo': metodo, 'origen': 'pantalla'})
            continue
        # el destino es un parametro: vale lo que pase el llamador, y solo si
        # llega null vale el literal del operador ternario mas cercano
        previas = list(re.finditer(rf'{nombre_var}\s*=\s*[^;]*?:\s*"([A-Z_]+)"',
                                   txt[:m.start()], re.S))
        trans.append({'destino': None, 'motivo': var.group(2), 'clase': clase,
                      'metodo': metodo, 'origen': 'parametro',
                      'defecto': previas[-1].group(1) if previas else None})

def argumentos(txt, pos):
    """Los argumentos de la llamada que abre parentesis en pos, contando
    parentesis para no cortarse en pedido.getId()."""
    i, prof = pos, 0
    while i < len(txt):
        if txt[i] == '(':
            prof += 1
        elif txt[i] == ')':
            prof -= 1
            if prof == 0:
                return txt[pos + 1:i]
        i += 1
    return ''


# que pasan de verdad los llamadores de cada metodo con destino parametrizado
for t in trans:
    if t['origen'] != 'parametro':
        continue
    pasados = []
    for clase, txt in FUENTES.items():
        if clase == t['clase']:
            continue                       # la declaracion, no una llamada
        for mm in re.finditer(rf'\.{t["metodo"]}\s*\(', txt):
            args = argumentos(txt, mm.end() - 1)
            lit = re.findall(r'"([A-Z_]+)"', args)
            pasados.append((clase, lit[-1] if lit else 'null'))
    t['pasados'] = sorted({p[1] for p in pasados if p[1] != 'null'})
    t['n_llamadores'] = len(pasados)
    t['pasa_null'] = any(p[1] == 'null' for p in pasados)

mr = FUENTES['MesaRepository']

# los estados que ofrece el mapa de salon al cambiar a mano
msc = FUENTES['MapaSalonController']
cm = cuerpo_metodo(msc, 'cambiarEstadoMesa')
MANUAL = re.findall(r'"([A-Z_]+)"', cm.split('ChoiceDialog')[0])
MISMO_NO = 'equalsIgnoreCase' in cm          # el dialogo descarta el estado actual

# liberarMesa se salta MesaService y escribe directo
lm = cuerpo_metodo(mr, 'liberarMesa')
LIBERA = re.findall(r'obtenerIdEstado\(conn,([^)]*)\)', lm)
LIBERA = re.findall(r'"([A-Z_]+)"', LIBERA[0]) if LIBERA else []
LIBERA_EXC = 'SQLException' if 'throw new SQLException' in lm else None
LIBERA_DESDE = sorted({f'MapaSalonController.{metodo_en(msc, m.start())}()'
                       for m in re.finditer(r'mesaRepository\.liberarMesa\(', msc)})

# los codigos que la interfaz sabe pintar
ev = re.search(r'private enum EstadoMesaVisual\s*\{(.*?)\n\s*private final',
               msc, re.S)
VISUAL = {}
if ev:
    for linea in ev.group(1).split('\n'):
        mm = re.match(r'\s*([A-Z_]+)\("([^"]*)",\s*"(#[0-9A-Fa-f]+)"\s*,?(.*?)\)\s*[,;]',
                      linea)
        if mm:
            VISUAL[mm.group(1)] = {'etiqueta': mm.group(2), 'color': mm.group(3),
                                   'codigos': re.findall(r'"([A-Z_]+)"', mm.group(4))}

# UPDATE directos a mesas.id_estado_mesa que no pasan por MesaService:
# no dejan fila en historial_estado_mesa
DIRECTOS = []
for m in re.finditer(r'public\s+\w+\s+(\w+)\s*\([^)]*\)[^{;]*\{', mr):
    cuerpo = cuerpo_metodo(mr, m.group(1))
    if 'UPDATE mesas' not in cuerpo or 'id_estado_mesa' not in cuerpo:
        continue
    if 'Connection conn' in m.group(0):          # ese lo invoca MesaService
        continue
    # el destino solo cuenta si esta en el SET; un literal en el WHERE es
    # una guarda sobre el estado actual, no un cambio de estado
    puesto = re.search(r"SET\b(.*?)WHERE id_mesa", cuerpo, re.S)
    destino = (re.search(r"codigo_estado\)\s*=\s*'([A-Z_]+)'", puesto.group(1))
               if puesto and 'id_estado_mesa' in puesto.group(1).split('=')[0] + puesto.group(1)
               else None)
    if puesto and 'id_estado_mesa' not in re.sub(r'WHERE.*', '', puesto.group(1), flags=re.S):
        destino = None
    guarda = re.search(r"IN\s*\(\s*((?:'[A-Z_]+'\s*,?\s*)+)\)", cuerpo)
    DIRECTOS.append({'metodo': m.group(1),
                     'destino': destino.group(1) if destino else None,
                     'desde': re.findall(r"'([A-Z_]+)'", guarda.group(1)) if guarda else [],
                     'cuerpo': cuerpo})

# el estado inicial: agregarMesa fija el id_estado_mesa a mano, y como
# estados_mesa es IDENTITY, ese id es el del primer codigo que siembra el
# catalogo
am = cuerpo_metodo(mr, 'agregarMesa')
mi = re.search(r'VALUES\s*\([^)]*?,\s*(\d+)\s*\)', am)
INICIAL_MESA = CAT_MESA[int(mi.group(1)) - 1] if mi and int(mi.group(1)) <= len(CAT_MESA) else None

# el trigger de nota_mesa reacciona a un codigo concreto
nm = open(REPO + '/db/ddl/nota_mesa.ddl', encoding='utf-8').read()
tg = re.search(r"codigo_nuevo\s*=\s*'([A-Z_]+)'", nm)
TRIGGER = tg.group(1) if tg else None

# que codigos usa el codigo y el catalogo no tiene
usados = set(MANUAL) | set(LIBERA)
for t in trans:
    if t['destino']:
        usados.add(t['destino'])
    if t.get('defecto'):
        usados.add(t['defecto'])
    usados |= set(t.get('pasados') or [])
HUERFANOS = sorted(u for u in usados if u not in CAT_MESA)

# codigos que la interfaz reconoce y que nadie escribe ni siembra, agrupados
# por el color que pintan: INHABILITADA e INHABILITADO son el mismo recuadro
reconocidos = {c for v in VISUAL.values() for c in v['codigos']}
FANTASMA = sorted(c for c in reconocidos if c not in CAT_MESA and c not in usados)
FANTASMA_VIS = {}
for v, info in VISUAL.items():
    muertos = [c for c in info['codigos'] if c in FANTASMA]
    if muertos:
        FANTASMA_VIS[v] = {'etiqueta': info['etiqueta'], 'muertos': muertos}
NUNCA_ASIGNADO = sorted(c for c in CAT_MESA if c not in usados)

# ========================================================== el dibujo ======
# Las etiquetas no van encima de las flechas: cada transicion lleva solo su
# numero y el detalle baja a una tabla. Asi ninguna etiqueta queda flotando
# entre dos lineas, que era lo que hacia ilegible el de mesa.
def cabecera():
    return ['@startuml',
            'skinparam backgroundColor #FFFFFF', 'skinparam shadowing false',
            'skinparam defaultFontName Helvetica',
            'skinparam nodesep 32', 'skinparam ranksep 48',
            'skinparam ArrowFontSize 12', 'skinparam ArrowFontStyle bold',
            'skinparam ArrowColor #1E293B',
            'skinparam StateFontSize 13', 'skinparam StateFontStyle bold',
            'skinparam StateBackgroundColor #FFFFFF',
            'skinparam StateBorderColor #334155',
            'skinparam legendBackgroundColor #FFFFFF',
            'skinparam legendBorderColor #94A3B8',
            'skinparam legendFontSize 11']


class Tabla:
    """Numera las transiciones y junta en una sola flecha las que comparten
    origen, destino y color."""

    def __init__(self):
        self.filas, self.flechas = [], {}

    def add(self, origen, destino, desde, op, motivo='', cond='', color=''):
        n = f'T{len(self.filas) + 1}'
        self.filas.append((n, desde, destino.replace('M_', '').replace('P_', ''),
                           op, motivo, cond))
        self.flechas.setdefault((origen, destino, color), []).append(n)
        return n

    def dibuja(self, sangria='  '):
        out = []
        for (o, d, c), ns in self.flechas.items():
            out.append(f'{sangria}{o} -{c}-> {d} : {", ".join(ns)}')
        return out

    def leyenda(self, colores):
        # la columna motivo solo existe si alguna transicion la usa: en el
        # pedido no hay motivos y una columna vacia solo estorba
        hay_mot = any(f[4] for f in self.filas)
        cab = '|=  |= desde |= a |= operacion |= condicion |'
        if hay_mot:
            cab = '|=  |= desde |= a |= operacion |= motivo |= condicion |'
        out = ['legend bottom', '<b>Transiciones</b>', cab]
        for n, desde, a, op, mot, cond in self.filas:
            celdas = [f'<b>{n}</b>', desde, a, op]
            if hay_mot:
                celdas.append(mot or ' ')
            celdas.append(cond or ' ')
            out.append('| ' + ' | '.join(celdas) + ' |')
        out += ['', '<b>Colores</b>  '
                + '   '.join(f'<back:{c}>      </back> {t}' for c, t in colores)]
        out.append('endlegend')
        return out


# ------------------------------------------------------------- pedido -----
L, T = cabecera(), Tabla()
L.append('state "Pedido  ·  enums/EstadoPedido" as PEDIDO #DBEAFE {')
L.append('  state "activo" as P_ACTIVO #F8FAFC {')
for e in ACTIVOS:
    gris = ' #E5E7EB' if e not in ASIGNADOS else ''
    L.append(f'    state "{e}" as P_{e}{gris}')
L.append('  }')
for e in FINALES:
    L.append(f'  state "{e}" as P_{e} #FEF9C3')
if INICIAL:
    T.add('[*]', f'P_{INICIAL["estado"]}', 'inicio',
          f'{INICIAL["clase"]}.{INICIAL["metodo"]}()', '',
          '; '.join('falla si: ' + g
                    for g in guardas(INICIAL['clase'], INICIAL['metodo'])[0]))
AZUL_P = '[#2563EB]'
# el color lo decide quien cambia el estado de verdad, que desde el Adapter
# ya no es el controlador
coc = next((a for a in asigna
            if f"{a['clase']}.{a['metodo']}()" == ORIGEN_AVANCE), None)
col_coc = AZUL_P if coc and coc['sin_servicio'] else ''
T.add(f'P_{COCINA_SI[0]}', f'P_{COCINA_SI[1]}', COCINA_SI[0],
      ORIGEN_AVANCE, '', f'estado == {COCINA_SI[0]}', col_coc)
for e in CON_BOTON:
    if e != COCINA_SI[0] and e != COCINA_NO:
        T.add(f'P_{e}', f'P_{COCINA_NO}', e, ORIGEN_AVANCE, '',
              f'estado != {COCINA_SI[0]}', col_coc)
for a in asigna:
    if a['metodo'] in GUARDADOS:
        base = f'estado no esta en {{{", ".join(FINALES)}}}'
        falla, _ = guardas(a['clase'], a['metodo'])
        T.add('P_ACTIVO', f'P_{a["estado"]}', 'cualquier estado activo',
              f'{a["clase"]}.{a["metodo"]}()', '',
              '; '.join([base] + ['falla si: ' + x for x in falla]),
              AZUL_P if a['sin_servicio'] else '')
L += T.dibuja()
for e in FINALES:
    L.append(f'  P_{e} --> [*]')
L.append('}')
col_ped = [('#E5E7EB', 'ningun codigo lo asigna'),
           ('#FEF9C3', 'estado final')]
if any(a['sin_servicio'] for a in asigna):
    col_ped.append(('#2563EB', 'flecha azul: escribe con pedidoRepository.save() '
                               'sin pasar por PedidoService, asi que se salta '
                               'la guarda ESTADOS_YA_CERRADOS'))
L += T.leyenda(col_ped)
L.append('@enduml')
open(BASE + '/puml/08_Estados_Pedido.puml', 'w',
     encoding='utf-8').write('\n'.join(L) + '\n')

# --------------------------------------------------------------- mesa -----
L, T = cabecera(), Tabla()
L.append('state "Mesa  ·  catalogo estados_mesa" as MESA #FFEDD5 {')
L.append('  state "cualquier estado del catalogo" as M_CAT #F8FAFC {')
for e in CAT_MESA:
    gris = ' #E5E7EB' if e in NUNCA_ASIGNADO else ''
    L.append(f'    state "{e}" as M_{e}{gris}')
L.append('  }')
for h in HUERFANOS:
    L.append(f'  state "{h}" as M_{h} #FECACA')

CUALQ = 'cualquier estado del catalogo'
AZUL = '[#2563EB]'
if INICIAL_MESA:
    T.add('[*]', f'M_{INICIAL_MESA}', 'inicio', 'MesaRepository.agregarMesa()',
          '', f'fija id_estado_mesa = {mi.group(1)}', AZUL)
for t in trans:
    op = f'{t["clase"]}.{t["metodo"]}()'
    if t['origen'] == 'literal':
        T.add('M_CAT', f'M_{t["destino"]}', CUALQ, op, t['motivo'],
              con_guardas(t['clase'], t['metodo']),
              '[#B91C1C]' if t['destino'] in HUERFANOS else '')
    elif t['origen'] == 'parametro':
        for d in (t.get('pasados') or []):
            cond = f'parametro = "{d}"' + (' o null' if t.get('defecto') == d else '')
            T.add('M_CAT', f'M_{d}', CUALQ, op, t['motivo'],
                  con_guardas(t['clase'], t['metodo'], cond))
        if t.get('defecto') and t['defecto'] not in (t.get('pasados') or []):
            T.add('M_CAT', f'M_{t["defecto"]}', CUALQ, op, t['motivo'],
                  con_guardas(t['clase'], t['metodo'], 'parametro = null'),
                  '[#B91C1C]' if t['defecto'] in HUERFANOS else '')
for e in MANUAL:
    T.add('M_CAT', f'M_{e}', CUALQ, 'MapaSalonController.cambiarEstadoMesa()',
          'MANUAL', con_guardas('MapaSalonController', 'cambiarEstadoMesa',
                                'elegido en el dialogo'))
for i, cod in enumerate(LIBERA):
    # obtenerIdEstado(conn, LIBRE, DISPONIBLE) devuelve el primero que exista
    previos = LIBERA[:i]
    cond = (f'existe {cod}' if not previos
            else 'no existe ' + ' ni '.join(previos))
    T.add('M_CAT', f'M_{cod}', CUALQ, 'MesaRepository.liberarMesa()', '',
          con_guardas('MesaRepository', 'liberarMesa', cond), AZUL)
for d in DIRECTOS:
    if d['metodo'] == 'liberarMesa' or not d['destino']:
        continue
    desde = [x for x in d['desde'] if x in CAT_MESA or x in HUERFANOS]
    cond = ('estado actual en ' + ', '.join(d['desde'])) if d['desde'] else ''
    for o in (desde or ['__CAT__']):
        T.add('M_CAT' if o == '__CAT__' else f'M_{o}', f'M_{d["destino"]}',
              CUALQ if o == '__CAT__' else o,
              f'MesaRepository.{d["metodo"]}()', '',
              con_guardas('MesaRepository', d['metodo'], cond), AZUL)
L += T.dibuja()
L.append('}')
if FANTASMA_VIS:
    L.append('state "solo en EstadoMesaVisual" as FANTASMA #F1F5F9 {')
    for v, info in FANTASMA_VIS.items():
        L.append(f'  state "{" / ".join(info["muertos"])}" as M_{v} #FECACA')
    L.append('}')
    L.append('MESA -[hidden]down-> FANTASMA')
L += T.leyenda([('#E5E7EB', 'ningun codigo lo asigna'),
                ('#FECACA', 'no esta en estados_mesa'),
                ('#B91C1C', 'flecha roja: pide un codigo que el catalogo no tiene'),
                ('#2563EB', 'flecha azul: escribe sin pasar por MesaService '
                            '(no deja fila en historial_estado_mesa)')])
L.append('@enduml')
open(BASE + '/puml/09_Estados_Mesa.puml', 'w',
     encoding='utf-8').write('\n'.join(L) + '\n')

# ============================================================ el informe ===
print(f'PEDIDO  estados del enum: {EST_PEDIDO}')
print(f'        finales (ESTADOS_YA_CERRADOS): {FINALES}')
print(f'        asignados por el codigo: {sorted(ASIGNADOS)}')
print(f'        nunca asignados: {sorted(set(EST_PEDIDO) - ASIGNADOS) or "ninguno"}')
print(f'        metodos con guarda de cierre: {sorted(GUARDADOS)}')
print(f'        cocina carga: {EN_COCINA}   sin boton desde: {SIN_BOTON}')
print(f'        cocina ({ORIGEN_AVANCE}): {COCINA_SI[0]} -> '
      f'{COCINA_SI[1]}, el resto -> {COCINA_NO}')
print(f'\nMESA    catalogo: {CAT_MESA}')
print(f'        nunca asignados: {NUNCA_ASIGNADO or "ninguno"}')
for t in trans:
    d = t['destino'] or (f'parametro, llamadores pasan {t.get("pasados")}, '
                         f'defecto {t.get("defecto")}' if t['origen'] == 'parametro'
                         else 'elegido en pantalla')
    print(f'        {t["clase"]}.{t["metodo"]}() -> {d}  motivo {t["motivo"]}')
print(f'        dialogo manual ofrece: {MANUAL} (descarta el actual: {MISMO_NO})')
print(f'        liberarMesa prueba {LIBERA}, llamado desde {LIBERA_DESDE}')
print(f'        la interfaz reconoce: {sorted(reconocidos)}')
print(f'        estado inicial (agregarMesa fija el id): {INICIAL_MESA}')
for d in DIRECTOS:
    print(f'        UPDATE directo: MesaRepository.{d["metodo"]}() -> '
          f'{d["destino"]}  desde {d["desde"] or "cualquiera"}')
print(f'\n  HUERFANOS (usados y no en el catalogo): {HUERFANOS or "ninguno"}')
print(f'  FANTASMA  (la UI los pinta y nadie los escribe): {FANTASMA or "ninguno"}')
print(f'  MesaService busca el codigo antes de escribir: {BUSCA_ANTES} -> {EXC_BUSCA}')
