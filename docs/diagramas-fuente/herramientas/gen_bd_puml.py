"""Diagrama entidad-relacion de GastroFlow en PlantUML, desde esquema.json.

esquema.json lo produce extraer_esquema.py leyendo db/ddl/*.ddl y las tablas
y columnas que agregan db/migrations/*.sql. Aqui no se escribe ningun dato
que no venga de ahi.
"""
import json, os, collections

BASE = os.environ.get('GF_OUT', '/home/claude/diagramas')
E = json.load(open(BASE + '/esquema.json', encoding='utf-8'))

AREAS = [
    ('Personas y acceso', '#F3E8FF',
     ['roles', 'tipos_documento', 'usuarios', 'clientes']),
    ('Salon y mesas', '#DBEAFE',
     ['zonas', 'estados_mesa', 'mesas', 'asignacion_mesa',
      'historial_estado_mesa', 'nota_mesa', 'reservas']),
    ('Carta e inventario', '#DCFCE7',
     ['categorias', 'productos', 'precios_producto', 'unidades_medida',
      'ingredientes', 'lote_ingrediente', 'plato_insumo', 'adicional',
      'movimientos_inventario', 'alerta_inventario', 'auditoria_recetas_hu134']),
    ('Pedidos', '#FFEDD5',
     ['pedidos', 'detalle_pedido', 'detalle_pedido_adicional']),
    ('Caja', '#FEF9C3', ['pagos', 'regla_descuento']),
]

ubicadas = {t for _n, _c, ts in AREAS for t in ts}
faltan = sorted(set(E) - ubicadas)
sobran = sorted(ubicadas - set(E))
assert not faltan, f'tablas sin area: {faltan}'
assert not sobran, f'areas con tablas inexistentes: {sobran}'

L = ['@startuml', 'allowmixing',
     'skinparam backgroundColor #FFFFFF',
     'skinparam shadowing false',
     'skinparam defaultFontName Helvetica',
     'skinparam roundCorner 4',
     'skinparam nodesep 30',
     'skinparam ranksep 55',
     'skinparam ArrowColor #475569',
     'skinparam ArrowFontSize 11',
     'skinparam PackageBorderColor #94A3B8',
     'skinparam PackageFontStyle bold',
     'skinparam ClassBorderColor #334155',
     'skinparam ClassBackgroundColor #FFFFFF',
     'skinparam EntityFontSize 12',
     'hide circle']

for nombre, color, tablas in AREAS:
    L.append(f'package "{nombre}" {color} {{')
    for t in tablas:
        d = E[t]
        pk = set(d['pk'])
        fk = {f['col']: f['tabla'] for f in d['fks']}
        L.append(f'  entity "{t}" as {t} #FFFFFF {{')
        for c, _tipo in d['cols']:
            if c in pk:
                L.append(f'    * **{c}** <<PK>>')
        if pk:
            L.append('    --')
        for c, _tipo in d['cols']:
            if c in pk:
                continue
            L.append(f'    # {c} <<FK>>' if c in fk else f'    {c}')
        L.append('  }')
    L.append('}')

n = 0
for t in sorted(E):
    for f in E[t]['fks']:
        L.append(f"{f['tabla']} ||--o{{ {t} : {f['col']}")
        n += 1
L.append('@enduml')
open(BASE + '/puml/02_BaseDeDatos.puml', 'w', encoding='utf-8').write('\n'.join(L) + '\n')

col = sum(len(d['cols']) for d in E.values())
print(f'tablas: {len(E)}   columnas: {col}   llaves foraneas: {n}')
sinfk = [t for t in sorted(E) if not E[t]['fks']]
print(f'tablas sin FK propia ({len(sinfk)}): {", ".join(sinfk)}')
entra = collections.Counter(f['tabla'] for d in E.values() for f in d['fks'])
print('mas referenciadas:', entra.most_common(5))
