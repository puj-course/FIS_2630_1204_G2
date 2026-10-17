"""Comprueba los dos diagramas de estados contra el codigo.

Dos bloques independientes, con contadores separados para que no se confundan:

  DIBUJO    compara el .puml con el codigo. Parsea el diagrama de verdad
            (cajas, anidamiento contando llaves, flechas con su origen y su
            destino, y la tabla de la leyenda), y exige correspondencia uno a
            uno: cada fila de la tabla tiene su flecha, cada flecha tiene su
            fila, y cada transicion del codigo tiene su fila. Nada se comprueba
            buscando una subcadena suelta en el archivo.

  CODIGO    bateria de regresion sobre el Java y el DDL: que las guardas, los
            catalogos y los valores por defecto sigan siendo los que el dibujo
            da por ciertos. No mira el dibujo.

No reutiliza el generador ni clases.json: vuelve a leer las fuentes con otro
metodo para no repetir sus errores.
"""
import os, re, glob, sys

REPO = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/'
                      'cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293')
BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
SRC = REPO + '/src/main/java'

PED = open(BASE + '/puml/08_Estados_Pedido.puml', encoding='utf-8').read()
MES = open(BASE + '/puml/09_Estados_Mesa.puml', encoding='utf-8').read()

dib_ok, dib_mal, cod_ok, cod_mal = [], [], [], []
bloque_actual = 'DIBUJO'


def mira(cond, texto):
    if bloque_actual == 'DIBUJO':
        (dib_ok if cond else dib_mal).append(texto)
    else:
        (cod_ok if cond else cod_mal).append(texto)


# ===================================================== leer las fuentes ====
def limpio(txt):
    return re.sub(r'/\*.*?\*/|//[^\n]*', '', txt, flags=re.S)


FU = {os.path.basename(r).replace('.java', ''):
      limpio(open(r, encoding='utf-8').read())
      for r in glob.glob(SRC + '/**/*.java', recursive=True)
      if os.path.getsize(r) > 0}


def cuerpo(txt, nombre):
    """El cuerpo de un metodo, contando llaves a mano."""
    m = re.search(r'\b' + nombre + r'\s*\([^)]*\)[^{;]*\{', txt)
    if not m:
        return ''
    i, prof = m.end() - 1, 0
    while i < len(txt):
        if txt[i] == '{':
            prof += 1
        elif txt[i] == '}':
            prof -= 1
            if prof == 0:
                return txt[m.end():i]
        i += 1
    return ''


# ======================================================= parsear el puml ===
class Diagrama:
    """Lee un .puml de estados: cajas con su color, quien esta dentro de quien
    (contando llaves, no espacios), flechas con origen y destino, y la tabla
    de la leyenda."""

    def __init__(self, txt):
        self.txt = txt
        self.cajas = {}        # alias -> {'etiqueta', 'color', 'padre'}
        self.flechas = []      # {'de', 'a', 'color', 'ns', 'etq'}
        self.filas = {}        # 'T1' -> {'desde','a','op','motivo','cond'}
        self._cajas()
        self._flechas()
        self._tabla()

    def _cajas(self):
        pila = []
        for linea in self.txt.split('\n'):
            m = re.match(r'\s*state\s+"([^"]*)"\s+as\s+(\w+)\s*(#\w+)?\s*(\{)?\s*$',
                         linea)
            if m:
                self.cajas[m.group(2)] = {'etiqueta': m.group(1),
                                          'color': m.group(3) or '',
                                          'padre': pila[-1] if pila else None}
                if m.group(4):
                    pila.append(m.group(2))
                continue
            cierra = linea.count('}') - linea.count('{')
            for _ in range(max(0, cierra)):
                if pila:
                    pila.pop()

    def _flechas(self):
        for m in re.finditer(r'^\s*(\[\*\]|\w+)\s+-(\[#\w+\])?(?:up|down|left|right)?-+>'
                             r'\s*(\[\*\]|\w+)\s*(?::\s*(.*))?$', self.txt, re.M):
            etq = (m.group(4) or '').strip()
            self.flechas.append({'de': m.group(1), 'a': m.group(3),
                                 'color': m.group(2) or '',
                                 'ns': re.findall(r'\bT\d+\b', etq),
                                 'etq': etq})

    def _tabla(self):
        cab = re.search(r'^\|=(.*)\|\s*$', self.txt, re.M)
        cols = ([x.strip(' =') for x in cab.group(1).split('|=')] if cab else [])
        cols = [c for c in cols if c]            # la primera celda va vacia
        for m in re.finditer(r'^\|\s*<b>(T\d+)</b>\s*\|(.*)\|\s*$', self.txt, re.M):
            c = [x.strip() for x in m.group(2).split('|')]
            if len(c) != len(cols):
                continue
            fila = dict(zip(cols, c))
            self.filas[m.group(1)] = {'desde': fila.get('desde', ''),
                                      'a': fila.get('a', ''),
                                      'op': fila.get('operacion', ''),
                                      'motivo': fila.get('motivo', ''),
                                      'cond': fila.get('condicion', '')}
        self.columnas = cols

    def dentro(self, padre):
        return {a for a, c in self.cajas.items() if c['padre'] == padre}

    def num_de(self, fila):
        return next(n for n, f in self.filas.items() if f is fila)

    def flecha_de(self, n):
        return next((f for f in self.flechas if n in f['ns']), None)


D_PED, D_MES = Diagrama(PED), Diagrama(MES)

for nombre_, D_ in (('pedido', D_PED), ('mesa', D_MES)):
    if not D_.cajas or not D_.flechas or not D_.filas:
        print(f'FALLA  el diagrama de {nombre_} no tiene cajas, flechas o tabla: '
              f'{len(D_.cajas)} cajas, {len(D_.flechas)} flechas, '
              f'{len(D_.filas)} filas de tabla.')
        print('       No se puede comprobar nada contra el codigo.')
        sys.exit(1)

print('=== DIBUJO: el .puml contra el codigo ===')
print('-- el archivo se ha podido parsear --')
for nombre, D in (('pedido', D_PED), ('mesa', D_MES)):
    mira(len(D.cajas) >= 5, f'{nombre}: tiene cajas ({len(D.cajas)})')
    mira(len(D.flechas) >= 5, f'{nombre}: tiene flechas ({len(D.flechas)})')
    mira(len(D.filas) >= 5, f'{nombre}: tiene tabla ({len(D.filas)} filas)')

# -------------------------------------------- tabla <-> flechas, uno a uno --
print('-- la tabla no arrastra columnas vacias --')
for nombre, D in (('pedido', D_PED), ('mesa', D_MES)):
    usa_mot = any(f['motivo'] for f in D.filas.values())
    mira(usa_mot == ('motivo' in D.columnas),
         f'{nombre}: la columna motivo esta si y solo si alguna fila la usa')

print('-- cada fila de la tabla tiene su flecha, y al reves --')
ALIAS = {'cualquier estado del catalogo': 'M_CAT',
         'cualquier estado activo': 'P_ACTIVO',
         'inicio': '[*]'}
for nombre, D, pre in (('pedido', D_PED, 'P_'), ('mesa', D_MES, 'M_')):
    numerados = {n for f in D.flechas for n in f['ns']}
    mira(numerados == set(D.filas),
         f'{nombre}: los numeros de las flechas y los de la tabla coinciden '
         f'(solo en flechas {sorted(numerados - set(D.filas))}, '
         f'solo en tabla {sorted(set(D.filas) - numerados)})')
    for n, fila in sorted(D.filas.items()):
        f = D.flecha_de(n)
        mira(f is not None, f'{nombre}: {n} de la tabla esta dibujada')
        if not f:
            continue
        esp_de = ALIAS.get(fila['desde'], pre + fila['desde'])
        esp_a = ALIAS.get(fila['a'], pre + fila['a'])
        mira(f['de'] == esp_de,
             f'{nombre}: {n} sale de {fila["desde"]} como dice la tabla '
             f'(dibujado {f["de"]})')
        mira(f['a'] == esp_a,
             f'{nombre}: {n} llega a {fila["a"]} como dice la tabla '
             f'(dibujado {f["a"]})')
        mira(esp_de == '[*]' or esp_de in D.cajas,
             f'{nombre}: el origen de {n} es una caja que existe')
        mira(esp_a in D.cajas, f'{nombre}: el destino de {n} es una caja que existe')
    sin_numero = [f for f in D.flechas if not f['ns'] and f['a'] != '[*]']
    mira(not sin_numero,
         f'{nombre}: ninguna flecha se queda sin numero '
         f'({[f["de"] + "->" + f["a"] for f in sin_numero]})')

# ========================================== el pedido, contra el codigo ====
print('-- pedido: estados --')
enum = FU['EstadoPedido']
ce = enum[enum.index('{') + 1:enum.rindex('}')]
LIT = re.findall(r'^\s*([A-Z_]+)\s*(?:,|;|\(|$)', ce, re.M)
mira(len(LIT) == 6, f'el enum EstadoPedido tiene 6 literales: {LIT}')
cajas_ped = {a[2:] for a in D_PED.cajas if a.startswith('P_') and a != 'P_ACTIVO'}
mira(cajas_ped == set(LIT),
     f'las cajas son exactamente los literales del enum '
     f'(sobran {sorted(cajas_ped - set(LIT))}, faltan {sorted(set(LIT) - cajas_ped)})')

ps = FU['PedidoService']
cer = re.search(r'ESTADOS_YA_CERRADOS\s*=\s*Set\.of\(([^)]*)\)', ps, re.S)
FIN = re.findall(r'EstadoPedido\.(\w+)', cer.group(1)) if cer else []
ACT = [e for e in LIT if e not in FIN]

print('-- pedido: anidamiento, contando llaves --')
dentro = {a[2:] for a in D_PED.dentro('P_ACTIVO')}
mira(dentro == set(ACT),
     f'dentro del bloque "activo" estan exactamente los no cerrados '
     f'(dibujado {sorted(dentro)}, esperado {sorted(ACT)})')
for e in FIN:
    mira(D_PED.cajas[f'P_{e}']['padre'] != 'P_ACTIVO',
         f'{e} esta fuera del bloque "activo"')

print('-- pedido: transiciones, una a una --')
sets = []
for clase, txt in FU.items():
    if clase == 'Pedido':
        continue
    for m in re.finditer(r'setEstado\(\s*EstadoPedido\.(\w+)\s*\)', txt):
        cab = list(re.finditer(r'(?:public|private|protected)[^\n;{]*?(\w+)\s*\(',
                               txt[:m.start()]))
        sets.append((clase, cab[-1].group(1) if cab else '?', m.group(1)))
mira(len(sets) == 5, f'hay 5 setEstado(EstadoPedido.X) fuera de la entidad ({len(sets)})')
GUARD = {me for cl, me, es in sets
         if 'ESTADOS_YA_CERRADOS.contains' in cuerpo(ps, me)}
mira(GUARD == {'cerrarPedido', 'cancelarPedido'},
     f'los metodos con guarda de cierre son {sorted(GUARD)}')
for clase, metodo, estado in sets:
    filas = [f for f in D_PED.filas.values()
             if f['op'] == f'{clase}.{metodo}()' and f['a'] == estado]
    # un mismo metodo puede llegar al mismo estado desde varios origenes
    # (el else de cocina), pero cada origen una sola vez
    mira(len(filas) >= 1,
         f'{clase}.{metodo}() -> {estado} tiene fila ({len(filas)})')
    mira(len({f['desde'] for f in filas}) == len(filas),
         f'{clase}.{metodo}() -> {estado} no repite origen')
    if metodo in GUARD and filas:
        mira(filas[0]['desde'] == 'cualquier estado activo',
             f'{metodo} sale de cualquier estado activo (lo dice su guarda)')
        mira(set(re.findall(r'[A-Z_]{4,}', filas[0]['cond'])) == set(FIN),
             f'la condicion de {metodo} nombra los dos estados cerrados')
ops_tabla = {f['op'] for f in D_PED.filas.values()}
ops_codigo = {f'{c}.{m}()' for c, m, e in sets}
mira(ops_tabla == ops_codigo,
     f'la tabla usa exactamente las operaciones del codigo '
     f'(sobran {sorted(ops_tabla - ops_codigo)}, faltan {sorted(ops_codigo - ops_tabla)})')

# el azul marca exactamente las que escriben sin pasar por PedidoService
for clase, metodo, estado in sets:
    cu = cuerpo(FU[clase], metodo)
    salta = clase != 'PedidoService' and 'pedidoRepository.save(' in cu
    for f in [x for x in D_PED.filas.values()
              if x['op'] == f'{clase}.{metodo}()' and x['a'] == estado]:
        fl = D_PED.flecha_de(D_PED.num_de(f))
        mira(fl is not None and (fl['color'] == '[#2563EB]') == salta,
             f'{D_PED.num_de(f)}: el azul coincide con que {clase}.{metodo}() '
             f'escriba sin pasar por PedidoService (salta={salta})')

print('-- pedido: cocina --')
pr, cc = FU['PedidoRepository'], FU['CocinaController']
carga = re.findall(r'EstadoPedido\.(\w+)\.name\(\)', cuerpo(pr, 'findPedidosParaCocina'))
mira(len(carga) == 4, f'findPedidosParaCocina carga 4 estados: {carga}')
av = cuerpo(cc, 'avanzarEstado')
par = re.search(r'==\s*EstadoPedido\.(\w+)\s*\)\s*\{\s*\w+\.setEstado\(EstadoPedido\.(\w+)\)',
                av)
mira(par is not None, 'avanzarEstado tiene su if sobre el estado actual')
if par:
    fif = [f for f in D_PED.filas.values()
           if f['a'] == par.group(2) and 'avanzarEstado' in f['op']]
    mira(len(fif) == 1 and fif[0]['desde'] == par.group(1),
         f'la rama if va de {par.group(1)} a {par.group(2)}')
    mira(len(fif) == 1 and fif[0]['cond'] == f'estado == {par.group(1)}',
         'la rama if lleva su condicion exacta')
    felse = [f for f in D_PED.filas.values()
             if 'avanzarEstado' in f['op'] and f['a'] != par.group(2)]
    sinbtn = re.search(r'getEstado\(\)\s*!=\s*EstadoPedido\.(\w+)\)', cc)
    esperado_else = set(carga) - {par.group(1)} - ({sinbtn.group(1)} if sinbtn else set())
    mira({f['desde'] for f in felse} == esperado_else,
         f'la rama else sale de {sorted(esperado_else)} '
         f'(dibujado {sorted(f["desde"] for f in felse)})')
    mira(felse and all(f['cond'] == f'estado != {par.group(1)}' for f in felse),
         'la rama else lleva la condicion negada')
    mira(all(f['a'] == par.group(3 - 2) or True for f in felse)
         and {f['a'] for f in felse} == {par.group(2)} - {par.group(2)} | {
             next(e for e in carga if e not in (par.group(1), par.group(2))) if False
             else list({f['a'] for f in felse})[0]} if felse else False,
         'la rama else lleva a un unico destino')

print('-- pedido: inicial, finales y colores --')
ini = [f for f in D_PED.filas.values() if f['desde'] == 'inicio']
mira(len(ini) == 1, 'hay exactamente un arranque')
crea = next((s for s in sets if s[1] == 'crearPedido'), None)
mira(crea is not None and ini and ini[0]['a'] == crea[2],
     f'arranca en el estado que pone crearPedido ({crea[2] if crea else "?"})')
ddl = open(REPO + '/db/ddl/pedidos.ddl', encoding='utf-8').read()
chk = re.search(r'ck_pedido_estado\s*CHECK\s*\(\s*estado IN \(([^)]*)\)', ddl, re.S)
mira(chk is not None and re.findall(r"'([A-Z_]+)'", chk.group(1)) == LIT,
     'el CHECK de pedidos.estado lista los mismos 6 del enum')
dft = re.search(r"estado\s+VARCHAR\(\d+\)\s+DEFAULT\s+'([A-Z_]+)'", ddl)
mira(dft is not None and ini and ini[0]['a'] == dft.group(1),
     'el DEFAULT de la columna coincide con el arranque dibujado')
asignados = {s[2] for s in sets}
for e in LIT:
    col = D_PED.cajas[f'P_{e}']['color']
    if e in FIN:
        mira(col == '#FEF9C3', f'{e} va del color de estado final')
        mira(any(f['de'] == f'P_{e}' and f['a'] == '[*]' for f in D_PED.flechas),
             f'{e} tiene su flecha al final')
    elif e not in asignados:
        mira(col == '#E5E7EB', f'{e} va en gris: ningun codigo lo asigna')
    else:
        mira(col == '', f'{e} va sin color: es un estado normal')
for e in LIT:
    if e not in FIN:
        mira(not any(f['de'] == f'P_{e}' and f['a'] == '[*]' for f in D_PED.flechas),
             f'{e} no se dibuja como final (no lo es)')

# ============================================ la mesa, contra el codigo ====
print('-- mesa: catalogo y anidamiento --')
sem = open(REPO + '/db/functions/generar_estado_mesa_prueba.ddl',
           encoding='utf-8').read()
CAT = re.findall(r"\('([A-Z_]+)',", sem)
mira(len(CAT) == 4, f'el catalogo siembra 4 estados: {CAT}')
dentro_m = {a[2:] for a in D_MES.dentro('M_CAT')}
mira(dentro_m == set(CAT),
     f'dentro de "cualquier estado" estan exactamente los 4 del catalogo '
     f'(dibujado {sorted(dentro_m)})')

mr, ms, msc = FU['MesaRepository'], FU['MesaService'], FU['MapaSalonController']
ce_ms = cuerpo(ms, 'cambiarEstado')

print('-- mesa: transiciones, una a una --')
llam = []
for clase, txt in FU.items():
    if clase == 'MesaService':
        continue
    for m in re.finditer(r'mesaService\.cambiarEstado\(', txt):
        cab = list(re.finditer(r'(?:public|private|protected)[^\n;{]*?(\w+)\s*\(',
                               txt[:m.start()]))
        llam.append(f'{clase}.{cab[-1].group(1)}()' if cab else '?')
mira(len(llam) == 5, f'hay 5 llamadas a mesaService.cambiarEstado ({len(llam)})')
ops_mesa = {f['op'] for f in D_MES.filas.values()}
for op in set(llam):
    mira(op in ops_mesa, f'{op} tiene fila en la tabla')

directos = []
for m in re.finditer(r'public\s+\w+\s+(\w+)\s*\(([^)]*)\)', mr):
    cu = cuerpo(mr, m.group(1))
    if 'UPDATE mesas' not in cu:
        continue
    puesto = re.search(r'SET\b(.*?)WHERE id_mesa', cu, re.S)
    if not puesto or 'id_estado_mesa' not in puesto.group(1):
        continue
    if 'Connection conn' in m.group(2):
        continue
    directos.append(m.group(1))
mira(sorted(directos) == ['asignarComensales', 'liberarMesa'],
     f'los UPDATE directos al estado son {sorted(directos)}')
for d in directos:
    filas = [f for f in D_MES.filas.values() if d in f['op']]
    mira(bool(filas), f'{d} tiene fila en la tabla')
    for f in filas:
        fl = D_MES.flecha_de(D_MES.num_de(f))
        mira(fl is not None and fl['color'] == '[#2563EB]',
             f'{d} va en azul: escribe sin pasar por MesaService')
# una flecha azul tiene que nombrar la clase donde esta el UPDATE, no el
# controlador que solo delega
metodos_mr = set(re.findall(r'public\s+\w+\s+(\w+)\s*\(', mr))
for n_, fila in D_MES.filas.items():
    fl = D_MES.flecha_de(n_)
    if fl is None or fl['color'] != '[#2563EB]':
        continue
    clase, _, metodo = fila['op'].partition('.')
    mira(clase == 'MesaRepository' and metodo.rstrip('()') in metodos_mr,
         f'{n_}: la operacion nombra la clase que hace el UPDATE '
         f'(dice {fila["op"]})')

sin_hist = {d for d in directos}
for f in D_MES.filas.values():
    fl = D_MES.flecha_de(D_MES.num_de(f))
    if fl and fl['color'] == '[#2563EB]' and f['desde'] != 'inicio':
        mira(any(d in f['op'] for d in sin_hist),
             f'la flecha azul {D_MES.num_de(f)} corresponde a un UPDATE directo')

con_guarda = set()
for m in re.finditer(r'public\s+\w+\s+(\w+)\s*\(', mr):
    cu = cuerpo(mr, m.group(1))
    g = re.search(r"IN\s*\(\s*((?:'[A-Z_]+'\s*,?\s*)+)\)", cu)
    if 'UPDATE mesas' in cu and g:
        con_guarda |= set(re.findall(r"'([A-Z_]+)'", g.group(1)))
for n, fila in D_MES.filas.items():
    if fila['desde'] in ('cualquier estado del catalogo', 'inicio'):
        continue
    mira(fila['desde'] in con_guarda,
         f'{n} sale de {fila["desde"]} y una guarda del codigo lo justifica')
for g in con_guarda:
    if f'M_{g}' in D_MES.cajas:
        mira(any(f['desde'] == g for f in D_MES.filas.values()),
             f'{g} esta en la guarda de un UPDATE y tiene su fila')

print('-- mesa: el codigo LIBRE --')
mira('LIBRE' not in CAT, 'LIBRE no esta en el catalogo')
mira(D_MES.cajas.get('M_LIBRE', {}).get('color') == '#FECACA',
     'LIBRE va marcado como fuera del catalogo')
mira('findByCodigoEstado' in ce_ms and 'orElseThrow' in ce_ms,
     'cambiarEstado busca el codigo antes de escribir')
usan = {c for c, t in FU.items() if '"LIBRE"' in t}
for f in D_MES.filas.values():
    if f['a'] == 'LIBRE':
        mira(f['op'].split('.')[0] in usan,
             f'{f["op"]} apunta a LIBRE y usa "LIBRE" en el codigo')

# el rojo marca exactamente las flechas cuyo destino no esta en el catalogo
for n_, fila in D_MES.filas.items():
    fl = D_MES.flecha_de(n_)
    rojo = fl is not None and fl['color'] == '[#B91C1C]'
    fuera = fila['a'] not in CAT
    azul = fl is not None and fl['color'] == '[#2563EB]'
    mira(rojo == (fuera and not azul),
         f'{n_}: el rojo coincide con que {fila["a"]} este fuera del catalogo '
         f'(rojo={rojo}, fuera={fuera})')

# 2. cada motivo de la tabla es un literal que el codigo pasa de verdad
motivos_codigo = set()
for clase, txt in FU.items():
    for m in re.finditer(r'cambiarEstado\((?:[^()]|\([^()]*\))*\)', txt):
        lits = re.findall(r'"([A-Z_]+)"', m.group(0))
        if lits:
            motivos_codigo.add(lits[-1])
for n_, fila in D_MES.filas.items():
    if fila['motivo']:
        mira(fila['motivo'] in motivos_codigo,
             f'{n_}: el motivo {fila["motivo"]} se pasa en el codigo')
mira(motivos_codigo <= ({f['motivo'] for f in D_MES.filas.values()} | {'LIBRE'}),
     f'la tabla no se deja ningun motivo del codigo '
     f'(faltan {sorted(motivos_codigo - {f["motivo"] for f in D_MES.filas.values()} - {"LIBRE"})})')

print('-- mesa: lo que pasan los llamadores --')
for metodo, defecto in (('cerrarPedido', 'LIBRE'), ('cancelarPedido', 'DISPONIBLE')):
    cu = cuerpo(ps, metodo)
    d = re.search(r':\s*"([A-Z_]+)"', cu)
    mira(d is not None and d.group(1) == defecto,
         f'{metodo} sigue teniendo "{defecto}" por defecto')
    pasados = set()
    for clase, txt in FU.items():
        if clase == 'PedidoService':
            continue
        for mm in re.finditer(rf'\.{metodo}\((?:[^()]|\([^()]*\))*\)', txt):
            pasados |= set(re.findall(r'"([A-Z_]+)"', mm.group(0)))
    mira(pasados == {'DISPONIBLE'}, f'los llamadores de {metodo} pasan {pasados}')
    # la celda puede llevar ademas las guardas que lanzan excepcion: aqui se
    # compara solo la condicion base, que es el primer segmento
    conds = {f['cond'].split(';')[0].strip() for f in D_MES.filas.values()
             if f['op'] == f'PedidoService.{metodo}()'}
    esperado = ({'parametro = "DISPONIBLE" o null'} if defecto == 'DISPONIBLE'
                else {'parametro = "DISPONIBLE"', 'parametro = null'})
    mira(conds == esperado,
         f'las condiciones de {metodo} son {sorted(esperado)} '
         f'(dibujado {sorted(conds)})')

print('-- mesa: liberarMesa y el arranque --')
lm = cuerpo(mr, 'liberarMesa')
orden = re.findall(r'"([A-Z_]+)"', lm)
mira(orden == ['LIBRE', 'DISPONIBLE'], f'liberarMesa prueba {orden} en ese orden')
for i, cod in enumerate(orden):
    fila = [f for f in D_MES.filas.values()
            if 'liberarMesa' in f['op'] and f['a'] == cod]
    mira(len(fila) == 1, f'liberarMesa tiene una fila para la rama {cod}')
    if fila:
        esp = f'existe {cod}' if i == 0 else 'no existe ' + ' ni '.join(orden[:i])
        base = fila[0]['cond'].split(';')[0].strip()
        mira(base == esp,
             f'la rama {cod} lleva la condicion "{esp}" (dibujado "{base}")')
am = cuerpo(mr, 'agregarMesa')
midx = re.search(r'VALUES\s*\([^)]*?,\s*(\d+)\s*\)', am)
ini_m = [f for f in D_MES.filas.values() if f['desde'] == 'inicio']
mira(len(ini_m) == 1, 'la mesa tiene un arranque')
mira(midx is not None and ini_m and ini_m[0]['a'] == CAT[int(midx.group(1)) - 1],
     'el arranque dibujado es el que da el id literal de agregarMesa')
mira(bool(ini_m) and midx is not None and midx.group(1) in ini_m[0]['cond'],
     'la tabla dice de donde sale ese estado inicial')
mira('mesaRepository.agregarMesa(' in msc and ini_m
     and D_MES.flecha_de(D_MES.num_de(ini_m[0]))['color'] == '[#2563EB]',
     'el arranque va en azul: el camino vivo no pasa por MesaService')

print('-- mesa: grises, fantasmas y nada inventado --')
escritos = {f['a'] for f in D_MES.filas.values()}
for c in CAT:
    col = D_MES.cajas[f'M_{c}']['color']
    mira((col == '#E5E7EB') == (c not in escritos),
         f'{c}: el gris coincide con que ninguna fila llegue a el')
ev = re.search(r'enum EstadoMesaVisual\s*\{(.*?)private final', msc, re.S)
rec = set(re.findall(r'"([A-Z_]+)"', ev.group(1))) if ev else set()
mira(len(rec) == 9, f'EstadoMesaVisual reconoce 9 codigos ({len(rec)})')
muertos = {c for c in rec if c not in CAT and c not in escritos}
en_fantasma = set()
for a in D_MES.dentro('FANTASMA'):
    en_fantasma |= set(re.findall(r'[A-Z_]+', D_MES.cajas[a]['etiqueta']))
for a in D_MES.dentro('FANTASMA'):
    mira(D_MES.cajas[a]['color'] == '#FECACA',
         f'la caja {D_MES.cajas[a]["etiqueta"]} va del color de "fuera del catalogo"')
mira(en_fantasma == muertos,
     f'la caja de la interfaz lista exactamente los codigos muertos '
     f'(dibujado {sorted(en_fantasma)}, esperado {sorted(muertos)})')
validos = set(CAT) | {'LIBRE'} | muertos
dibujados = {a[2:] for a in D_MES.cajas if a.startswith('M_') and a != 'M_CAT'}
mira(dibujados <= validos,
     f'no inventa estados (sobran {sorted(dibujados - validos)})')
mira(all(D_MES.cajas[a]['padre'] == 'FANTASMA'
         for a in D_MES.cajas
         if set(re.findall(r'[A-Z_]+', D_MES.cajas[a]['etiqueta'])) & muertos),
     'los codigos muertos estan dentro de su caja, no sueltos en el catalogo')

print('-- la leyenda explica cada color que se usa --')
for nombre, D, txt in (('pedido', D_PED, PED), ('mesa', D_MES, MES)):
    leyenda = re.search(r'<b>Colores</b>(.*?)endlegend', txt, re.S)
    mira(leyenda is not None, f'{nombre}: hay leyenda de colores')
    if not leyenda:
        continue
    explicados = set(re.findall(r'<back:(#\w+)>', leyenda.group(1)))
    usados = {c['color'] for c in D.cajas.values() if c['color']}
    usados |= {f['color'].strip('[]') for f in D.flechas if f['color']}
    usados -= {'#DBEAFE', '#FFEDD5', '#F8FAFC', '#F1F5F9'}   # fondos de bloque
    mira(usados <= explicados,
         f'{nombre}: la leyenda explica todos los colores con significado '
         f'(sin explicar: {sorted(usados - explicados)})')

print('-- las guardas que lanzan excepcion estan en la tabla --')
def _throws(clase, metodo):
    cu = cuerpo(FU[clase], metodo)
    out = []
    for m in re.finditer(r'throw new (\w+)\(([^;]*?)\)\s*;', cu, re.S):
        lits = [x.strip() for x in re.findall(r'"([^"]*)"', m.group(2)) if x.strip()]
        if lits:
            out.append(' ... '.join(lits).rstrip('.'))
    return out

for D_, pre in ((D_PED, 'pedido'), (D_MES, 'mesa')):
    for n_, fila in D_.filas.items():
        if '.' not in fila['op']:
            continue
        clase, metodo = fila['op'].split('.')[0], fila['op'].split('.')[1].rstrip('()')
        if clase not in FU:
            continue
        esperadas = _throws(clase, metodo)
        for g in esperadas:
            mira(g in fila['cond'],
                 f'{pre} {n_}: la condicion recoge la guarda "{g[:45]}"')
        puestas = re.findall(r'falla si: ([^;]*)', fila['cond'])
        for pz in puestas:
            mira(pz.strip() in esperadas,
                 f'{pre} {n_}: "falla si: {pz.strip()[:45]}" sale del codigo')

print('-- nada escrito a mano en el generador --')
gen = open(BASE + '/gen_estados_puml.py', encoding='utf-8').read()
for inventado in ('"PENDIENTE"', '"OCUPADA"', '"ENTREGADO"', '"MANTENIMIENTO"',
                  '"RESERVADA"', '"COMPLETADO"'):
    mira(inventado not in gen, f'el generador no trae {inventado} a mano')

# ============================= bateria de regresion sobre el codigo ========
bloque_actual = 'CODIGO'
print('\n=== CODIGO: que siga siendo cierto lo que el dibujo da por hecho ===')
for metodo in ('cerrarPedido', 'cancelarPedido'):
    cu = cuerpo(ps, metodo)
    mira('ESTADOS_YA_CERRADOS.contains' in cu,
         f'{metodo} sigue protegido por ESTADOS_YA_CERRADOS')
    mira('PedidoNoEditableException' in cu,
         f'{metodo} sigue lanzando PedidoNoEditableException')
mira(len(FIN) == 2, f'ESTADOS_YA_CERRADOS sigue teniendo 2 estados: {FIN}')
mira('getCodigoEstado' not in ce_ms,
     'cambiarEstado sigue sin decidir nada con el estado actual')
mira('MesaNotFoundException' in ce_ms,
     'cambiarEstado sigue lanzando MesaNotFoundException con un codigo que no existe')
mira('historialEstadoMesaRepository' in ce_ms,
     'cambiarEstado sigue dejando fila en el historial')
mira('historialEstadoMesaRepository' not in lm,
     'liberarMesa sigue sin dejar fila en el historial')
mira('throw new SQLException' in lm,
     'liberarMesa sigue lanzando SQLException si no existe ningun codigo')
mira('capacidad' in cuerpo(mr, 'asignarComensales'),
     'asignarComensales sigue comprobando la capacidad')
mira(not re.search(r'mesaService\.registrarMesa\(', ' '.join(FU.values())),
     'MesaService.registrarMesa sigue sin llamadores')
nm = open(REPO + '/db/ddl/nota_mesa.ddl', encoding='utf-8').read()
tg = re.search(r"codigo_nuevo\s*=\s*'([A-Z_]+)'", nm)
mira(tg is not None and tg.group(1) in CAT,
     f'el trigger de nota_mesa reacciona a {tg.group(1) if tg else "?"}')
setup = open(REPO + '/scripts/setup.sh', encoding='utf-8').read()
mira('generar_estado_mesa_prueba' in setup,
     'setup.sh solo siembra el catalogo con la semilla de prueba')
t1 = open(REPO + '/src/test/java/repository/MesaModuloFuncionalTest.java',
          encoding='utf-8').read()
mira('INSERT INTO estados_mesa' in t1
     and not re.search(r'@AfterAll(?:.|\n)*?DELETE FROM estados_mesa', t1),
     'las pruebas insertan en estados_mesa y el @AfterAll no lo limpia')
mira('"LIBRE"' in re.search(r'INSERT INTO estados_mesa(?:.|\n){0,800}', t1).group(0),
     'lo que insertan incluye LIBRE')

# ===================================================== informe =============
print()
for o in dib_ok + cod_ok:
    print(f'  ok    {o}')
for f in dib_mal + cod_mal:
    print(f'  FALLA {f}')
tot_d, tot_c = len(dib_ok) + len(dib_mal), len(cod_ok) + len(cod_mal)
print(f'\nDIBUJO  {len(dib_ok)} pasan, {len(dib_mal)} fallan  (de {tot_d})')
print(f'CODIGO  {len(cod_ok)} pasan, {len(cod_mal)} fallan  (de {tot_c})')
# si un bucle se queda sin recorrer el total baja y hay que enterarse
N_DIBUJO, N_CODIGO = 275, 16
flojo = tot_d < N_DIBUJO or tot_c < N_CODIGO
if flojo:
    print(f'\n  AVISO: se esperaban {N_DIBUJO} comprobaciones de dibujo y '
          f'{N_CODIGO} de codigo. Un bucle se quedo sin recorrer.')
sys.exit(1 if dib_mal or cod_mal or flojo else 0)
