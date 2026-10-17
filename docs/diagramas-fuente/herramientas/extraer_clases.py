"""Extrae de src/main/java TODOS los tipos, campos, metodos y parametros.

Usa tree-sitter (soporta Java 21: records, enums con cuerpo, switch expressions).
Salida: clases.json con la estructura exacta del codigo.
"""
import glob, json, os
import tree_sitter_java
from tree_sitter import Language, Parser

import os
SUB = os.environ.get('GF_SUB', 'main')          # main | test
SRC = os.environ.get('GF_SRC', '/tmp/claude-0/-home-claude/cf991e6c-a30f-5c5f-8ffa-56f3e8e08a43/scratchpad/gf293') + f'/src/{SUB}/java'
SALIDA = (os.environ.get('GF_OUT', '/home/claude/diagramas')
          + ('/clases.json' if SUB == 'main' else f'/clases_{SUB}.json'))

LANG = Language(tree_sitter_java.language())
PARSER = Parser(LANG)

VIS = {'public': '+', 'private': '-', 'protected': '#'}


def txt(n, src):
    return src[n.start_byte:n.end_byte].decode('utf-8')


def modificadores(n, src):
    for h in n.children:
        if h.type == 'modifiers':
            return txt(h, src).split()
    return []


def visibilidad(mods, kind=None):
    for m in mods:
        if m in VIS:
            return VIS[m]
    # en una interfaz y en una anotacion, un miembro sin modificador es
    # publico por definicion del lenguaje, no package-private
    return '+' if kind in ('interface', 'annotation') else '~'


def nombre_de(n, src):
    c = n.child_by_field_name('name')
    return txt(c, src) if c else '?'


def parametros(n, src):
    ps = n.child_by_field_name('parameters')
    out = []
    if not ps:
        return out
    for h in ps.named_children:
        if h.type in ('formal_parameter', 'spread_parameter', 'receiver_parameter'):
            if h.type == 'spread_parameter':
                # 'String... cols': el tipo es el primer hijo y el nombre va
                # dentro de un variable_declarator, no hay campos con nombre
                t = h.named_children[0] if h.named_children else None
                vd = next((c for c in h.named_children
                           if c.type == 'variable_declarator'), None)
                nm = vd.child_by_field_name('name') if vd else None
                tipo = (txt(t, src) if t else '?') + '...'
            else:
                t = h.child_by_field_name('type')
                nm = h.child_by_field_name('name')
                tipo = txt(t, src) if t else '?'
            out.append({'nombre': txt(nm, src) if nm else '?', 'tipo': tipo})
    return out


def miembros(cuerpo, src, kind=None):
    campos, metodos = [], []
    if cuerpo is None:
        return campos, metodos
    for h in cuerpo.named_children:
        if h.type == 'field_declaration':
            mods = modificadores(h, src)
            t = h.child_by_field_name('type')
            tipo = txt(t, src) if t else '?'
            for d in h.named_children:
                if d.type == 'variable_declarator':
                    nm = d.child_by_field_name('name')
                    campos.append({'nombre': txt(nm, src), 'tipo': tipo,
                                   'vis': visibilidad(mods, kind),
                                   'static': 'static' in mods,
                                   'final': 'final' in mods})
        elif h.type == 'method_declaration':
            mods = modificadores(h, src)
            t = h.child_by_field_name('type')
            metodos.append({'nombre': nombre_de(h, src),
                            'retorno': txt(t, src) if t else 'void',
                            'params': parametros(h, src),
                            'vis': visibilidad(mods, kind),
                            'static': 'static' in mods,
                            'abstract': 'abstract' in mods})
        elif h.type == 'constructor_declaration':
            mods = modificadores(h, src)
            metodos.append({'nombre': nombre_de(h, src), 'retorno': '',
                            'params': parametros(h, src),
                            'vis': visibilidad(mods),
                            'static': False, 'abstract': False,
                            'constructor': True})
    return campos, metodos


def recorrer(nodo, src, paquete, archivo, salida, padre=None):
    TIPOS = {'class_declaration': 'class', 'interface_declaration': 'interface',
             'enum_declaration': 'enum', 'record_declaration': 'record',
             'annotation_type_declaration': 'annotation'}
    for n in nodo.named_children:
        if n.type in TIPOS:
            nom = nombre_de(n, src)
            cuerpo = n.child_by_field_name('body')
            campos, metodos = miembros(cuerpo, src, n.type.replace('_declaration', ''))

            # record: los componentes son campos implicitos
            if n.type == 'record_declaration':
                for p in parametros(n, src):
                    campos.append({'nombre': p['nombre'], 'tipo': p['tipo'],
                                   'vis': '-', 'static': False, 'final': True})

            literales = []
            if n.type == 'enum_declaration' and cuerpo is not None:
                for h in cuerpo.named_children:
                    if h.type == 'enum_constant':
                        literales.append(nombre_de(h, src))
                    elif h.type == 'enum_body_declarations':
                        c2, m2 = miembros(h, src, 'enum')
                        campos += c2
                        metodos += m2

            sup = n.child_by_field_name('superclass')
            itf = n.child_by_field_name('interfaces')
            if sup is None:
                # interface X extends Y: el nodo se llama extends_interfaces
                for c in n.children:
                    if c.type == 'extends_interfaces':
                        sup = c
                        break
            salida.append({
                'nombre': nom, 'kind': n.type.replace('_declaration', ''),
                'paquete': paquete, 'archivo': archivo, 'anidada_en': padre,
                'extends': txt(sup, src).replace('extends', '').strip() if sup else None,
                'implements': txt(itf, src).replace('implements', '').strip() if itf else None,
                'literales': literales, 'campos': campos, 'metodos': metodos,
            })
            if cuerpo is not None:
                recorrer(cuerpo, src, paquete, archivo, salida, padre=nom)
        elif n.type not in ('class_body', 'enum_body'):
            recorrer(n, src, paquete, archivo, salida, padre)


tipos = []
errores = []
for ruta in sorted(glob.glob(f'{SRC}/**/*.java', recursive=True)):
    rel = os.path.relpath(ruta, SRC)
    if rel.startswith('database/'):
        continue
    datos = open(ruta, 'rb').read()
    arbol = PARSER.parse(datos)
    if arbol.root_node.has_error:
        errores.append(rel)
    paq = rel.split(os.sep)[0] if os.sep in rel else '(raiz)'
    recorrer(arbol.root_node, datos, paq, rel, tipos)

json.dump(tipos, open(SALIDA, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)

nc = len(tipos)
nf = sum(len(t['campos']) for t in tipos)
nm = sum(len(t['metodos']) for t in tipos)
np = sum(len(m['params']) for t in tipos for m in t['metodos'])
print(f'tipos={nc}  campos={nf}  metodos={nm}  parametros={np}')
print('archivos con error de parseo:', errores or 'ninguno')
import collections
c = collections.Counter(t['paquete'] for t in tipos)
for p, n in sorted(c.items(), key=lambda x: -x[1]):
    print(f'  {p:12} {n:3} tipos')
