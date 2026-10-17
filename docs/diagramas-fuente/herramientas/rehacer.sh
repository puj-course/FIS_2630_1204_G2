#!/usr/bin/env bash
# Rehace los 22 diagramas de GastroFlow desde el codigo y los comprueba.
#
#   ./rehacer.sh /ruta/al/clon/de/FIS_2630_1204_G2
#
# El clon debe estar en la rama que se quiere dibujar (git checkout develop
# && git pull antes de correr esto). No se escribe nada dentro del clon: solo
# se lee.
set -euo pipefail

REPO="${1:-}"
if [ -z "$REPO" ] || [ ! -d "$REPO/src/main/java" ]; then
    echo "uso: $0 /ruta/al/clon/de/FIS_2630_1204_G2" >&2
    exit 1
fi
AQUI="$(cd "$(dirname "$0")" && pwd)"
export GF_SRC="$REPO"
export GF_OUT="$AQUI"
JAR="${PLANTUML_JAR:-/home/claude/pl/package/vendor/plantuml.jar}"

echo "== repositorio =="
git -C "$REPO" log -1 --format='%h  %ad  %s' --date=short
echo

echo "== 1. leer el codigo =="
python3 -I "$AQUI/extraer_clases.py"
GF_SUB=test python3 -I "$AQUI/extraer_clases.py"
python3 -I "$AQUI/extraer_relaciones.py" | tail -4
python3 -I "$AQUI/extraer_esquema.py" | grep -E '^(TABLAS|FK)'
python3 -I "$AQUI/extraer_llamadas.py" | head -2
echo

echo "== 2. generar los .puml =="
for g in gen_arq_puml gen_bd_puml gen_paq_puml gen_desp_puml; do
    python3 -I "$AQUI/$g.py" > /dev/null
done
python3 -I "$AQUI/gen_clases_completo.py" | tail -3
python3 -I "$AQUI/gen_pruebas.py" | head -1
python3 -I "$AQUI/gen_componentes_puml.py" | head -6
python3 -I "$AQUI/gen_componentes_spring.py" | tail -4
python3 -I "$AQUI/gen_estados_puml.py" | grep -E 'HUERFANOS|FANTASMA|nunca asignados'
echo

echo "== 3. renderizar PNG y SVG =="
for d in puml puml_clases puml_componentes; do
    ( cd "$AQUI/$d" && rm -f ./*.png ./*.svg \
      && java -DPLANTUML_LIMIT_SIZE=16384 -jar "$JAR" -tpng ./*.puml > /dev/null 2>&1 \
      && java -DPLANTUML_LIMIT_SIZE=16384 -jar "$JAR" -tsvg ./*.puml > /dev/null 2>&1 )
    echo "  $d: $(ls "$AQUI/$d"/*.png | wc -l) diagramas"
done
echo

echo "== 4. comprobar contra el codigo =="
python3 -I "$AQUI/verificar.py"            | tail -3
python3 -I "$AQUI/verificar_lineas.py"     "$REPO" "$AQUI" | tail -3
python3 -I "$AQUI/verificar_cajas.py"       "$REPO" "$AQUI" | tail -3
python3 -I "$AQUI/verificar_componentes.py" "$REPO" "$AQUI" | tail -3
python3 -I "$AQUI/verificar_cobertura.py"   "$REPO" "$AQUI" | tail -2
python3 -I "$AQUI/verificar_estados.py"     | tail -2
echo

echo "== 5. empaquetar =="
rm -rf "$AQUI/entrega_final/01_estructura" "$AQUI/entrega_final/02_clases" \
       "$AQUI/entrega_final/03_componentes"
mkdir -p "$AQUI/entrega_final/01_estructura" "$AQUI/entrega_final/02_clases" \
         "$AQUI/entrega_final/03_componentes"
for f in "$AQUI"/puml/*.{puml,png,svg}; do          # los de estados van aparte
    case "$(basename "$f")" in 0[89]_Estados_*) continue ;; esac
    cp "$f" "$AQUI/entrega_final/01_estructura/"
done
cp "$AQUI"/puml_clases/*.{puml,png,svg}     "$AQUI/entrega_final/02_clases/"
cp "$AQUI"/puml_componentes/*.{puml,png,svg} "$AQUI/entrega_final/03_componentes/"
cp "$AQUI"/{clases,clases_test,relaciones,esquema,llamadas}.json "$AQUI/entrega_final/"
rm -rf "$AQUI/entrega_final/04_estados"
mkdir -p "$AQUI/entrega_final/04_estados"
cp "$AQUI"/puml/0{8,9}_Estados_*.{puml,png,svg} "$AQUI/entrega_final/04_estados/"
rm -f "$AQUI/GastroFlow_Diagramas_develop.zip"
( cd "$AQUI/entrega_final" && zip -rq ../GastroFlow_Diagramas_develop.zip . )
echo "  $AQUI/GastroFlow_Diagramas_develop.zip"

echo
echo "== 6. documentacion para docs/ =="
python3 -I "$AQUI/gen_docs.py" | tail -3
rm -f "$AQUI/GastroFlow_docs.zip"
( cd "$AQUI/docs_entrega" && zip -rq ../GastroFlow_docs.zip . )
echo "  $AQUI/GastroFlow_docs.zip"

echo
echo "== 7. JPG a doble resolucion =="
# dpi 192 = el doble del normal; -quality 100 con submuestreo 4:4:4 para que
# el texto no se emborrone en los bordes de las letras
TMP="$(mktemp -d)"
for f in "$AQUI"/puml/0{8,9}_Estados_*.puml; do
    n="$(basename "$f" .puml)"
    sed 's/^@startuml/@startuml\nskinparam dpi 192/' "$f" > "$TMP/$n.puml"
done
( cd "$TMP" && java -DPLANTUML_LIMIT_SIZE=32768 -jar "$JAR" -tpng ./*.puml >/dev/null 2>&1 )
mkdir -p "$AQUI/jpg/04_estados"
rm -f "$AQUI/jpg/04_estados"/*.jpg
for p in "$TMP"/*.png; do
    convert "$p" -quality 100 -sampling-factor 1x1 -interlace none \
            "$AQUI/jpg/04_estados/$(basename "$p" .png).jpg"
done
rm -rf "$TMP"
ls -1 "$AQUI/jpg/04_estados"/*.jpg | sed 's|.*/|  |'
rm -f "$AQUI/GastroFlow_Estados_JPG.zip"
( cd "$AQUI/jpg" && zip -rq ../GastroFlow_Estados_JPG.zip 04_estados )
echo "  $AQUI/GastroFlow_Estados_JPG.zip"
