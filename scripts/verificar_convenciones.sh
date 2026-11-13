#!/usr/bin/env bash
#
# GastroFlow - verificacion de las convenciones del codigo.
#
# Comprueba las reglas que ya se corrigieron una vez en el proyecto, para que
# no vuelvan a entrar sin que nadie se de cuenta. No necesita compilar ni base
# de datos: solo lee los archivos.
#
# Uso:
#   ./scripts/verificar_convenciones.sh
#
# Devuelve 0 si todo esta bien y 1 si encuentra algo, con el archivo y la
# linea exactos para poder ir directo a corregirlo.

set -uo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FUENTES="$BASE_DIR/src/main/java"

PROBLEMAS=0

# Imprime un bloque de hallazgos y cuenta cuantos hay.
reportar() {
    local titulo="$1"
    local explicacion="$2"
    local hallazgos="$3"

    if [ -z "$hallazgos" ]; then
        echo "   ok   $titulo"
        return
    fi

    local cuantos
    cuantos=$(printf '%s\n' "$hallazgos" | wc -l | tr -d ' ')
    PROBLEMAS=$((PROBLEMAS + cuantos))

    echo "  FALLA $titulo ($cuantos)"
    echo "        $explicacion"
    printf '%s\n' "$hallazgos" | sed 's|^'"$BASE_DIR"/'|          |'
    echo
}

echo ">> revisando las convenciones de src/main/java"
echo

# --------------------------------------------------------------------------
# 1. Nada de imprimir a la consola: los errores van al registro.
# --------------------------------------------------------------------------
reportar "sin printStackTrace" \
    "Use LOG.log(Level.SEVERE, \"...\", e) en vez de imprimir la traza." \
    "$(grep -rn 'printStackTrace' "$FUENTES" 2>/dev/null)"

reportar "sin System.out ni System.err" \
    "Use java.util.logging. La consola no queda registrada en ningun lado." \
    "$(grep -rnE 'System\.(out|err)\.print' "$FUENTES" 2>/dev/null)"

# --------------------------------------------------------------------------
# 2. Nada de contrasenas escritas en el codigo.
# --------------------------------------------------------------------------
reportar "sin contrasenas en el codigo" \
    "Los datos de conexion se leen del entorno o de conf/database.properties." \
    "$(grep -rniE '(password|passwd|contrasena|contrasenia|clave|secret|token|api_?key)[[:space:]]*=[[:space:]]*"[^"]+"' \
        "$FUENTES" 2>/dev/null | grep -viE '= *""|getProperty|getenv|setString|valor\(')"

# --------------------------------------------------------------------------
# 3. Toda excepcion del dominio lleva serialVersionUID.
# --------------------------------------------------------------------------
sin_uid=""
for archivo in "$FUENTES"/exceptions/*.java; do
    [ -e "$archivo" ] || continue
    if grep -q 'extends RuntimeException' "$archivo" \
       && ! grep -q 'serialVersionUID' "$archivo"; then
        sin_uid="${sin_uid}${archivo}: falta serialVersionUID"$'\n'
    fi
done
reportar "excepciones con serialVersionUID" \
    "RuntimeException es Serializable: sin el campo, SonarQube lo marca." \
    "$(printf '%s' "$sin_uid")"

# --------------------------------------------------------------------------
# 4. Ningun catch completamente vacio.
# --------------------------------------------------------------------------
vacios=$(find "$FUENTES" -name '*.java' -exec awk '
    /catch[[:space:]]*\(/ { pendiente = 1; linea = FNR; next }
    pendiente == 1 {
        linea_limpia = $0
        gsub(/[[:space:]]/, "", linea_limpia)
        if (linea_limpia == "}") {
            print FILENAME ":" linea ": el catch no hace nada ni explica por que"
        }
        pendiente = 0
    }
' {} \; 2>/dev/null)
reportar "sin catch vacios" \
    "Si se ignora un error a proposito, escriba en el catch por que se ignora." \
    "$vacios"

# --------------------------------------------------------------------------
# 5. Nada de imprimir con formato directamente sobre la consola del sistema.
# --------------------------------------------------------------------------
reportar "sin System.out.printf ni println encadenados" \
    "Misma razon: toda salida del programa pasa por el registro." \
    "$(grep -rnE 'System\.(out|err)\.(printf|write|append)' "$FUENTES" 2>/dev/null)"

echo
if [ "$PROBLEMAS" -eq 0 ]; then
    echo "Todo en orden: el codigo cumple las convenciones."
    exit 0
fi

echo "Se encontraron $PROBLEMAS cosas por corregir."
exit 1
