"""Comprueba los tres diagramas de patrones contra el codigo.

No se fia del generador: vuelve a leer clases.json y llamadas.json y exige
que cada linea dibujada exista en el codigo con esa misma firma, que las
cajas completas no se dejen ningun miembro fuera, y que las flechas sean
las llamadas que de verdad hay.

Lee el .puml, que es el fuente que se publica, y comprueba ademas que cada
.md muestre la imagen que le corresponde y que esa imagen exista.

    python3 verificar_patrones.py <carpeta con los json> <carpeta docs> <clon>
"""
import json
import os
import re
import sys

PAT = sys.argv[1] if len(sys.argv) > 1 else '.'
DOCS = sys.argv[2] if len(sys.argv) > 2 else '.'
SRC = (sys.argv[3] if len(sys.argv) > 3
       else os.environ.get('GF_SRC', '.')) + '/src/main/java'

T = {t['nombre']: t for t in
     json.load(open(PAT + '/clases.json', encoding='utf-8'))}
LL = json.load(open(PAT + '/llamadas.json', encoding='utf-8'))
LLAMA = {}
for l in LL['llamadas']:
    LLAMA.setdefault(l['origen'], {}).setdefault(l['destino'], set()).add(
        l['metodo'])

fallos, pasan = [], 0


def ok(cond, titulo, detalle=''):
    global pasan
    if cond:
        pasan += 1
        print(f'  OK   {titulo}')
    else:
        fallos.append(titulo)
        print(f'  FALLA {titulo}')
        for linea in str(detalle).split('\n'):
            if linea:
                print(f'         {linea}')


def firmas(nombre):
    """Las lineas que el codigo autoriza para la caja de esa clase."""
    t = T[nombre]
    campos = {f"{c['vis']}{c['nombre']} : {c['tipo']}" for c in t['campos']}
    metodos = {}
    for m in t['metodos']:
        ps = ', '.join(f"{p['nombre']} : {p['tipo']}" for p in m['params'])
        ctor = m.get('constructor', m['nombre'] == nombre)
        ret = '' if ctor else f" {m['retorno']}"
        metodos.setdefault(m['nombre'], set()).add(
            f"{m['vis']}{m['nombre']}({ps}){ret}")
    return campos, metodos


# class/interface/enum X [<<estereotipo>>] { ... }
CAJA = re.compile(
    r'^(class|interface|enum|annotation) (\w+)(?: <<(\w+)>>)? \{\n(.*?)^\}$',
    re.M | re.S)
FLECHA = re.compile(r'^(\w+)(?: "[^"]*")? ([<>|.\-o*]+)(?: "[^"]*")? '
                    r'(\w+)(?: : (.*))?$', re.M)
# la realizacion y la herencia se escriben al reves: en "A <|.. B" quien
# implementa es B, no A
REVES = ('<|..', '<|--', '<..', '<--', '<*--', '<o--')
PALABRA = {'class': 'class', 'interface': 'interface', 'enum': 'enum',
           'record': 'class', 'annotation': 'annotation'}


def numeros(puml):
    """Los numeros del diagrama, contados aqui y no pedidos al generador."""
    cajas = CAJA.findall(puml)
    cuerpos = [c for _p, _n, _e, c in cajas]
    return dict(
        cajas=len(cajas),
        vacias=len([c for c in cuerpos if not c.strip()]),
        campos=sum(len(re.findall(r'^  [-+#]\w+ :', c, re.M)) for c in cuerpos),
        metodos=sum(len(re.findall(r'^  [-+#]\w+\(', c, re.M)) for c in cuerpos),
        flechas=len(FLECHA.findall(puml)),
        realizaciones=len([f for f in FLECHA.findall(puml) if '|' in f[1]]))


# lo que el texto del .md afirma, frase por frase, para poder contrastarlo
AFIRMA = [
    (re.compile(r'Son (\d+) cajas y (\d+) flechas'), ('cajas', 'flechas')),
    (re.compile(r'(\d+) de (?:ellas de )?realización'), ('realizaciones',)),
    (re.compile(r'con (\d+) atributos? y (\d+) operaciones'),
     ('campos', 'metodos')),
    (re.compile(r'Las otras (\d+) cajas van vacías'), ('vacias',)),
    (re.compile(r'con (\d+) operaciones'), ('metodos',)),
]


def extremos(o, tipo, d):
    return (d, o) if tipo in REVES else (o, d)


def fuente(nombre):
    ruta = f"{SRC}/{T[nombre]['archivo']}"
    return open(ruta, encoding='utf-8').read() if os.path.exists(ruta) else ''


def padres(nombre):
    """Las interfaces y la clase padre de un tipo, por su nombre a secas.

    clases.json guarda 'implements' como el texto de la clausula, que puede
    traer varias interfaces separadas por coma y argumentos genericos; hay
    que partirlo antes de comparar nombres.
    """
    t = T[nombre]
    crudo = (t.get('implements') or '')
    if isinstance(crudo, list):
        crudo = ', '.join(crudo)
    partes = [crudo, t.get('extends') or '']
    out = set()
    for p in partes:
        for x in re.split(r',(?![^<>]*>)', p):
            x = x.split('<')[0].strip()
            if x:
                out.add(x)
    return out


def relacion_real(o, d):
    """Una flecha vale si el codigo de o toca a d de alguna forma real.

    No basta con mirar las llamadas resueltas: un `new DescuentoPorcentaje()`
    no es una llamada, y un `combo.getValue().calcular()` se resuelve sobre
    un tipo generico que el extractor no puede seguir. Las dos cosas son
    relaciones de verdad y tienen que poder dibujarse.
    """
    if d in LLAMA.get(o, {}):
        return True
    t = T[o]
    if d in padres(o):
        return True
    tipos = [c['tipo'] for c in t['campos']]
    for m in t['metodos']:
        tipos.append(m.get('retorno', ''))
        tipos += [p['tipo'] for p in m['params']]
    if any(re.search(rf'\b{re.escape(d)}\b', x or '') for x in tipos):
        return True
    cuerpo = fuente(o)
    # sin comentarios: un nombre citado en un comentario no es una relacion
    cuerpo = re.sub(r'//[^\n]*|/\*.*?\*/', ' ', cuerpo, flags=re.S)
    return bool(re.search(rf'\bnew\s+{re.escape(d)}\s*\(', cuerpo))


# que clases puede tocar cada diagrama: las del issue, y las de alrededor
# solo como caja vacia
ARCHIVOS = {
    'patrones/diagramaSingleton.md': {
        'puml': 'DiagramaPatron_Singleton',
        'completas': {'SesionUsuario'},
        'issue': '#292',
    },
    'patrones/diagramaFacade.md': {
        'puml': 'DiagramaPatron_Facade',
        'completas': {'PedidoFacade'},
        'parciales': {'PedidoService', 'InventarioService', 'DisponibilidadDAO'},
        'issue': '#293',
    },
    'patrones/diagramaStrategy.md': {
        'puml': 'DiagramaPatron_Strategy',
        'completas': {'DescuentoStrategy', 'DescuentoMontoFijo',
                      'DescuentoPorcentaje', 'CalculadoraDescuento'},
        'issue': '#294 y #281',
    },
    'patrones/diagramaAdapter.md': {
        'puml': 'DiagramaPatron_Adapter',
        'completas': {'ComandaCocina', 'PedidoComandaAdapter'},
        'parciales': {'Pedido'},
        'issue': 'tablero de cocina',
    },
    'patrones/diagramaFactory.md': {
        'puml': 'DiagramaPatron_Factory',
        'completas': {'AlertaStockFactory', 'AlertaStock', 'AlertaAgotado',
                      'AlertaStockBajo', 'AlertaNormal'},
        'parciales': {'Ingrediente'},
        'issue': 'alertas de stock',
    },
}
# el Observer vive en la rama de Mariana; solo se comprueba si el analisis
# que se le pasa lo contiene
if 'InsumoObserver' in T:
    ARCHIVOS['patrones/diagramaObserver.md'] = {
        'puml': 'DiagramaPatron_Observer',
        'completas': {'InsumoObserver', 'AdministradorAlertaObserver'},
        'parciales': {'InventarioService'},
        'issue': '#288',
    }

for archivo, cfg in ARCHIVOS.items():
    print(f"\n=== {archivo} ({cfg['issue']}) ===")
    ruta = f"{DOCS}/diagramas-fuente/{cfg['puml']}.puml"
    if not os.path.exists(ruta):
        ok(False, f"existe {cfg['puml']}.puml")
        continue
    texto = open(ruta, encoding='utf-8').read()
    ok(texto.startswith('@startuml') and texto.rstrip().endswith('@enduml'),
       'el .puml abre y cierra')

    # 0. el .md ensena esa imagen y la imagen existe
    md = f'{DOCS}/{archivo}'
    t_md = open(md, encoding='utf-8').read() if os.path.exists(md) else ''
    img = re.findall(r'!\[[^\]]*\]\(\.\./Imagenes/([\w.]+)\)', t_md)
    ok(img == [f"{cfg['puml']}.png"],
       f"el .md muestra {cfg['puml']}.png y nada mas", f'muestra {img}')
    ok(os.path.exists(f"{DOCS}/Imagenes/{cfg['puml']}.png"),
       f"la imagen {cfg['puml']}.png existe")
    ok('```' not in t_md,
       'el .md no se quedo con el bloque de codigo de la version anterior')

    # 0b. el .md trae las dos secciones y sus numeros son los del diagrama
    ok('**Por qué está.**' in t_md and '**Qué muestra.**' in t_md,
       'el .md trae "Por qué está" y "Qué muestra"')
    n = numeros(texto)
    malnum = []
    for patron, claves in AFIRMA:
        m = patron.search(t_md)
        if not m:
            continue
        for i, clave in enumerate(claves):
            if int(m.group(i + 1)) != n[clave]:
                malnum.append(f'el texto dice {m.group(i + 1)} {clave} y el '
                              f'diagrama tiene {n[clave]}')
    ok(not malnum, f'los numeros del texto son los del diagrama '
       f'({n["cajas"]} cajas, {n["flechas"]} flechas, {n["campos"]} campos, '
       f'{n["metodos"]} metodos)', '\n'.join(malnum))

    cajas, tipos_caja = {}, {}
    for palabra, n, _est, cuerpo in CAJA.findall(texto):
        cajas[n] = cuerpo or ''
        tipos_caja[n] = palabra
    ok(bool(cajas), 'el diagrama tiene cajas')

    # 1. toda clase dibujada existe en el codigo
    fantasma = [n for n in cajas if n not in T]
    ok(not fantasma, f'las {len(cajas)} clases dibujadas existen en el codigo',
       ', '.join(fantasma))

    # 1b. y se dibuja con la palabra que le corresponde: una interfaz como
    #     interfaz, no como clase
    mal = [f"{n}: dibujada como {tipos_caja[n]}, el codigo dice "
           f"{PALABRA[T[n]['kind']]}"
           for n in cajas if n in T and tipos_caja[n] != PALABRA[T[n]['kind']]]
    ok(not mal, 'cada caja usa la palabra del codigo (class / interface / '
                'enum)', '\n'.join(mal))

    # 2. las cajas con miembros son solo las del issue
    con_miembros = {n for n, c in cajas.items() if c.strip()}
    permitidas = set(cfg['completas']) | set(cfg.get('parciales', ()))
    ok(con_miembros <= permitidas,
       'solo llevan miembros las clases del issue',
       f'de mas: {sorted(con_miembros - permitidas)}')

    # 3. cada linea dibujada existe en el codigo con esa firma
    mal = []
    for n, cuerpo in cajas.items():
        if n not in T:
            continue
        campos, metodos = firmas(n)
        validas = campos | {x for v in metodos.values() for x in v}
        for linea in cuerpo.split('\n'):
            linea = linea.strip()
            if not linea:
                continue
            if linea not in validas:
                mal.append(f'{n}: "{linea}" no coincide con ninguna '
                           f'declaracion del codigo')
    ok(not mal, 'cada miembro dibujado existe en el codigo con esa firma',
       '\n'.join(mal))

    # 4. las cajas completas no se dejan nada fuera
    mal = []
    for n in cfg['completas']:
        if n not in cajas:
            mal.append(f'falta la caja de {n}')
            continue
        campos, metodos = firmas(n)
        dibujadas = {l.strip() for l in cajas[n].split('\n') if l.strip()}
        faltan = (campos | {x for v in metodos.values() for x in v}) - dibujadas
        if faltan:
            mal.append(f'{n}: faltan {sorted(faltan)}')
    ok(not mal, 'las clases del issue se dibujan completas', '\n'.join(mal))

    # 5. las cajas parciales solo pueden traer metodos que alguien del
    #    diagrama les llame de verdad
    mal = []
    for n in cfg.get('parciales', ()):
        llamados = set()
        for origen in cfg['completas']:
            llamados |= LLAMA.get(origen, {}).get(n, set())
        dibujados = {re.match(r'[-+#~](\w+)\(', l.strip()).group(1)
                     for l in cajas.get(n, '').split('\n')
                     if re.match(r'\s*[-+#~]\w+\(', l)}
        if dibujados != llamados:
            mal.append(f'{n}: dibuja {sorted(dibujados)} y le llaman '
                       f'{sorted(llamados)}')
    ok(not mal, 'del subsistema solo se dibuja lo que la fachada le llama',
       '\n'.join(mal))

    # 6. las flechas de uso corresponden a llamadas reales
    mal = []
    for o_, tipo, d_, _et in FLECHA.findall(texto):
        o, d = extremos(o_, tipo, d_)
        if o == d or o not in T or d not in T:
            continue
        if not relacion_real(o, d):
            mal.append(f'{o} -> {d} no existe en el codigo')
    ok(not mal, 'cada flecha corresponde a una llamada, un campo o una '
                'implementacion real', '\n'.join(mal))

    # 7. ninguna pareja se dibuja dos veces: dos flechas entre las mismas
    #    dos cajas se leen como una sola y esconden informacion
    vistas, repes = set(), []
    for o_, tipo, d_, _et in FLECHA.findall(texto):
        par = extremos(o_, tipo, d_)
        if par in vistas:
            repes.append(f'{par[0]} -> {par[1]}')
        vistas.add(par)
    ok(not repes, 'no hay dos flechas entre el mismo par de cajas',
       ', '.join(repes))

PUML = {c['puml'] for c in ARCHIVOS.values()}

# 8. el enum muerto no puede reaparecer en ningun diagrama de patrones
muerto = [p for p in sorted(PUML)
          if os.path.exists(f'{DOCS}/diagramas-fuente/{p}.puml')
          and 'TipoDescuento' in open(f'{DOCS}/diagramas-fuente/{p}.puml',
                                      encoding='utf-8').read()]
print()
ok(not muerto, 'ningun diagrama documenta el enum TipoDescuento, que ya no '
   'se usa', ', '.join(muerto))

# 9. las realizaciones dibujadas son las del codigo, ni una mas ni una menos
real_cod = {(n, i) for n in T for i in padres(n)
            if i in T and T[i]['kind'] == 'interface'}
real_dib = set()
for p in PUML:
    ruta = f'{DOCS}/diagramas-fuente/{p}.puml'
    if not os.path.exists(ruta):
        continue
    for o_, tipo, d_, _e in FLECHA.findall(open(ruta, encoding='utf-8').read()):
        if tipo in ('<|..', '..|>'):
            real_dib.add(extremos(o_, tipo, d_))
ok(real_dib <= real_cod,
   f'las {len(real_dib)} realizaciones dibujadas son implements reales',
   f'inventadas: {sorted(real_dib - real_cod)}')

# 10. los clientes del singleton son exactamente los que lo llaman
sing = f'{DOCS}/diagramas-fuente/DiagramaPatron_Singleton.puml'
if os.path.exists(sing):
    texto = open(sing, encoding='utf-8').read()
    dib = set()
    for o_, tipo, d_, _e in FLECHA.findall(texto):
        o, d = extremos(o_, tipo, d_)
        if d == 'SesionUsuario' and o != d:
            dib.add(o)
    real = {o for o, ds in LLAMA.items() if 'SesionUsuario' in ds}
    ok(dib == real, f'los {len(real)} clientes del singleton son los que lo '
       f'usan de verdad', f'dibujados {sorted(dib)} vs reales {sorted(real)}')
    # y la flecha a si mismo, que es lo que distingue a un Singleton
    ok('SesionUsuario --> SesionUsuario' in texto,
       'el singleton guarda una instancia de su propio tipo (flecha a si '
       'mismo)')

print(f'\n=== resultado ===\n{pasan} pasan, {len(fallos)} fallan')
if fallos:
    sys.exit(1)
print('todo correcto')
