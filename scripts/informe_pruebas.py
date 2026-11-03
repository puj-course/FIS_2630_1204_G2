#!/usr/bin/env python3
"""
Genera el informe de resultados de pruebas del taller, con la misma
plantilla que el plan de pruebas (punto 2 del enunciado).

Cruza tres fuentes:
  - target/surefire-reports/TEST-*.xml  : lo que paso al ejecutar
  - src/test/java/service/**.java       : que metodo corresponde a cada caso CP
  - docs/planDePruebas.md               : descripcion, entrada, tipo y resultado esperado

Salidas: target/site/informe-pruebas.html y target/site/informe-pruebas.json

Devuelve 1 si el plan y las pruebas no coinciden, para que el pipeline avise
cuando alguien agrega una prueba y se le olvida documentarla, o al reves.
"""

import glob
import html
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SUREFIRE = os.path.join(RAIZ, 'target', 'surefire-reports')
PLAN = os.path.join(RAIZ, 'docs', 'planDePruebas.md')
SALIDA = os.path.join(RAIZ, 'target', 'site')

SERVICIOS = {
    'descuento': 'Descuentos (Strategy)',
    'alerta': 'Alertas de inventario (Factory Method)',
    'cocina': 'Tablero de cocina (Adapter)',
}


def metodos_por_caso():
    """metodo java -> (codigo CP, servicio). Sale de los @DisplayName."""
    mapa = {}
    patron = re.compile(
        r'@DisplayName\("\s*(CP\d\d)[^"]*"\)\s*\n\s*(?:@\w+[^\n]*\n\s*)*void\s+(\w+)\s*\(')
    for ruta in glob.glob(os.path.join(RAIZ, 'src/test/java/service/*/*.java')):
        servicio = os.path.basename(os.path.dirname(ruta))
        with open(ruta, encoding='utf-8') as f:
            for cp, metodo in patron.findall(f.read()):
                mapa[metodo] = (cp, servicio)
    return mapa


def casos_del_plan():
    """codigo CP -> datos de la tabla del plan de pruebas."""
    if not os.path.exists(PLAN):
        return {}
    with open(PLAN, encoding='utf-8') as f:
        texto = f.read()
    casos = {}
    # Tabla del plan: caso | descripcion | entrada | tipo | esperado | real
    for fila in re.findall(
            r'^\|\s*(CP\d\d)\s*\|([^|]*)\|([^|]*)\|([^|]*)\|([^|]*)\|([^|]*)\|\s*$',
            texto, re.M):
        cp, desc, entrada, tipo, esperado, _real = (c.strip() for c in fila)
        casos.setdefault(cp, {}).update(
            descripcion=desc, entrada=entrada, tipo=tipo, esperado=esperado)
    # Tabla de trazabilidad: caso | metodo | justificacion
    for cp, metodo, just in re.findall(
            r'^\|\s*(CP\d\d)\s*\|([^|]*)\|([^|]*)\|\s*$', texto, re.M):
        casos.setdefault(cp, {}).update(
            metodo=metodo.strip().strip('`'), justificacion=just.strip())
    return casos


def resultados_surefire():
    """Lee los XML de Surefire. Devuelve una lista de ejecuciones."""
    ejecuciones = []
    for ruta in sorted(glob.glob(os.path.join(SUREFIRE, 'TEST-*.xml'))):
        raiz = ET.parse(ruta).getroot()
        for tc in raiz.iter('testcase'):
            estado, mensaje = 'exitoso', ''
            for etiqueta, nombre in (('failure', 'fallido'),
                                     ('error', 'error'),
                                     ('skipped', 'omitido')):
                nodo = tc.find(etiqueta)
                if nodo is not None:
                    estado = nombre
                    mensaje = (nodo.get('message') or
                               (nodo.text or '').strip().split('\n')[0])
                    break
            ejecuciones.append({
                'clase': tc.get('classname', ''),
                'nombre': tc.get('name', ''),
                'segundos': float(tc.get('time') or 0),
                'estado': estado,
                'mensaje': mensaje,
            })
    return ejecuciones


def codigo_de(ejecucion, mapa):
    """Saca el CP de una ejecucion, del nombre o del metodo fuente."""
    directo = re.match(r'\s*(CP\d\d)', ejecucion['nombre'])
    if directo:
        return directo.group(1)
    metodo = re.sub(r'\(.*\)$', '', ejecucion['nombre']).strip()
    return mapa.get(metodo, (None, None))[0]


def dato_git(*args):
    try:
        return subprocess.run(['git', *args], cwd=RAIZ, capture_output=True,
                              text=True, timeout=10).stdout.strip() or '—'
    except Exception:
        return '—'


CSS = """
:root{--linea:#d6dee3;--azul:#1f4e6b;--tinta:#1b1b1b;--suave:#f2f6f8}
*{box-sizing:border-box}
body{margin:0;padding:32px 20px;background:#fff;color:var(--tinta);
     font:15px/1.55 -apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif}
.hoja{max-width:1180px;margin:0 auto}
h1{font-size:26px;color:var(--azul);margin:0 0 4px}
h2{font-size:19px;color:var(--azul);margin:34px 0 10px}
.sub{color:#5a5a5a;margin:0 0 22px}
.meta{border:1px solid var(--linea);border-radius:8px;padding:12px 16px;
      margin-bottom:22px;background:var(--suave);font-size:13.5px}
.meta b{color:var(--azul)}
.tarjetas{display:flex;flex-wrap:wrap;gap:12px;margin:0 0 10px}
.t{flex:1 1 150px;border:1px solid var(--linea);border-radius:8px;padding:12px 14px}
.t .n{font-size:26px;font-weight:700;line-height:1.1}
.t .e{font-size:12.5px;color:#5a5a5a;text-transform:uppercase;letter-spacing:.04em}
.ok .n{color:#1a7f40}
.mal .n{color:#b02a37}
.om .n{color:#8a6d00}
table{border-collapse:collapse;width:100%;margin:10px 0 4px;font-size:13px}
th{background:var(--azul);color:#fff;text-align:left;padding:8px 10px;font-weight:600}
td{border:1px solid var(--linea);padding:7px 10px;vertical-align:top}
tr:nth-child(even) td{background:#fafcfd}
code,.mono{font-family:Consolas,Menlo,monospace;font-size:12.3px}
.est{font-weight:700;white-space:nowrap}
.est.exitoso{color:#1a7f40}
.est.fallido,.est.error{color:#b02a37}
.est.omitido{color:#8a6d00}
.msg{color:#b02a37;font-family:Consolas,Menlo,monospace;font-size:12.2px}
.nota{color:#5a5a5a;font-size:13px;margin:8px 0 0}
.aviso{border-left:4px solid #b02a37;background:#fdf3f4;padding:10px 14px;
       border-radius:0 6px 6px 0;margin:14px 0}
"""


def main():
    mapa = metodos_por_caso()
    plan = casos_del_plan()
    ejecuciones = resultados_surefire()

    if not ejecuciones:
        print('No hay resultados en target/surefire-reports. '
              'Corra "mvn test" antes de generar el informe.', file=sys.stderr)
        return 2

    del_taller, otras = {}, []
    for e in ejecuciones:
        cp = codigo_de(e, mapa)
        if cp:
            e['caso'] = cp
            e['servicio'] = mapa.get(
                re.sub(r'\(.*\)$', '', e['nombre']).strip(), (None, ''))[1] \
                or next((s for s in SERVICIOS if s in e['clase']), '')
            del_taller[cp] = e
        else:
            otras.append(e)

    cuenta = lambda xs, st: sum(1 for x in xs if x['estado'] == st)
    todas = list(del_taller.values()) + otras
    resumen = {
        'ejecutados': len(todas),
        'exitosos': cuenta(todas, 'exitoso'),
        'fallidos': cuenta(todas, 'fallido') + cuenta(todas, 'error'),
        'omitidos': cuenta(todas, 'omitido'),
        'segundos': round(sum(x['segundos'] for x in todas), 3),
        'del_taller': len(del_taller),
        'taller_exitosos': cuenta(del_taller.values(), 'exitoso'),
    }

    sin_prueba = sorted(set(plan) - set(del_taller))
    sin_plan = sorted(set(del_taller) - set(plan))

    # ---------------------------------------------------------------- HTML
    def esc(texto):
        """Escapa y convierte `identificador` del markdown en <code>."""
        partes = str(texto).split('`')
        return ''.join(f'<code>{html.escape(p)}</code>' if i % 2 else html.escape(p)
                       for i, p in enumerate(partes) if p != '')
    o = []
    o.append('<!doctype html><html lang="es"><meta charset="utf-8">')
    o.append('<meta name="viewport" content="width=device-width,initial-scale=1">')
    o.append('<title>Informe de resultados de pruebas · GastroFlow</title>')
    o.append(f'<style>{CSS}</style><div class="hoja">')
    o.append('<h1>Informe de resultados de pruebas</h1>')
    o.append('<p class="sub">GastroFlow · Taller de pruebas unitarias con JUnit '
             'y métricas con SonarQube</p>')

    o.append('<div class="meta">')
    o.append(f'<b>Generado:</b> {datetime.now(timezone.utc):%Y-%m-%d %H:%M} UTC'
             f' &nbsp;·&nbsp; <b>Rama:</b> {esc(dato_git("rev-parse", "--abbrev-ref", "HEAD"))}'
             f' &nbsp;·&nbsp; <b>Commit:</b> <span class="mono">'
             f'{esc(dato_git("rev-parse", "--short", "HEAD"))}</span>')
    o.append('</div>')

    o.append('<div class="tarjetas">')
    for etiqueta, valor, clase in (
            ('Casos ejecutados', resumen['ejecutados'], ''),
            ('Exitosos (passed)', resumen['exitosos'], 'ok'),
            ('Fallidos (failed)', resumen['fallidos'], 'mal' if resumen['fallidos'] else ''),
            ('Omitidos', resumen['omitidos'], 'om' if resumen['omitidos'] else ''),
            ('Duración (s)', resumen['segundos'], '')):
        o.append(f'<div class="t {clase}"><div class="n">{valor}</div>'
                 f'<div class="e">{etiqueta}</div></div>')
    o.append('</div>')
    o.append(f'<p class="nota">De esos, {resumen["del_taller"]} corresponden al plan de '
             f'pruebas del taller y {resumen["taller_exitosos"]} pasaron.</p>')

    if sin_prueba or sin_plan:
        o.append('<div class="aviso"><b>El plan y las pruebas no coinciden.</b><br>')
        if sin_prueba:
            o.append(f'Documentados sin prueba que los ejecute: {", ".join(sin_prueba)}.<br>')
        if sin_plan:
            o.append(f'Ejecutados sin estar en el plan: {", ".join(sin_plan)}.')
        o.append('</div>')

    for clave, titulo in SERVICIOS.items():
        casos = sorted((cp for cp, e in del_taller.items() if e.get('servicio') == clave))
        if not casos:
            continue
        o.append(f'<h2>{esc(titulo)}</h2>')
        o.append('<table><tr><th>Caso</th><th>Descripción</th><th>Entrada de datos</th>'
                 '<th>Tipo</th><th>Resultado esperado</th><th>Resultado real</th>'
                 '<th>Estado</th><th>ms</th></tr>')
        for cp in casos:
            e, p = del_taller[cp], plan.get(cp, {})
            real = esc(p.get('esperado', '—')) if e['estado'] == 'exitoso' \
                else f'<span class="msg">{esc(e["mensaje"] or e["estado"])}</span>'
            o.append(
                f'<tr><td class="mono">{cp}</td><td>{esc(p.get("descripcion", "—"))}</td>'
                f'<td>{esc(p.get("entrada", "—"))}</td><td>{esc(p.get("tipo", "—"))}</td>'
                f'<td>{esc(p.get("esperado", "—"))}</td><td>{real}</td>'
                f'<td class="est {e["estado"]}">{e["estado"]}</td>'
                f'<td>{int(e["segundos"] * 1000)}</td></tr>')
        o.append('</table>')

    if otras:
        o.append('<h2>Otras pruebas del proyecto</h2>')
        o.append('<p class="nota">No pertenecen al plan del taller. Se incluyen para que '
                 'el informe refleje la ejecución completa del pipeline.</p>')
        o.append('<table><tr><th>Clase</th><th>Prueba</th><th>Estado</th>'
                 '<th>Mensaje de error</th><th>ms</th></tr>')
        for e in sorted(otras, key=lambda x: (x['clase'], x['nombre'])):
            o.append(
                f'<tr><td class="mono">{esc(e["clase"].split(".")[-1])}</td>'
                f'<td>{esc(e["nombre"])}</td>'
                f'<td class="est {e["estado"]}">{e["estado"]}</td>'
                f'<td class="msg">{esc(e["mensaje"])}</td>'
                f'<td>{int(e["segundos"] * 1000)}</td></tr>')
        o.append('</table>')

    o.append('</div></html>')

    os.makedirs(SALIDA, exist_ok=True)
    with open(os.path.join(SALIDA, 'informe-pruebas.html'), 'w', encoding='utf-8') as f:
        f.write('\n'.join(o))
    with open(os.path.join(SALIDA, 'informe-pruebas.json'), 'w', encoding='utf-8') as f:
        json.dump({'resumen': resumen,
                   'casos': [{**plan.get(cp, {}), **e} for cp, e in sorted(del_taller.items())],
                   'otras': otras,
                   'desajustes': {'sin_prueba': sin_prueba, 'sin_plan': sin_plan}},
                  f, ensure_ascii=False, indent=2)

    print(f'Informe escrito en target/site/informe-pruebas.html')
    print(f'  ejecutados {resumen["ejecutados"]}  exitosos {resumen["exitosos"]}  '
          f'fallidos {resumen["fallidos"]}  omitidos {resumen["omitidos"]}')
    if sin_prueba or sin_plan:
        print(f'  DESAJUSTE  sin prueba: {sin_prueba}  sin plan: {sin_plan}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
